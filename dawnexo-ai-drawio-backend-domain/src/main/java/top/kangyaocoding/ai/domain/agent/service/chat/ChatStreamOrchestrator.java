package top.kangyaocoding.ai.domain.agent.service.chat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.adk.events.Event;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.kangyaocoding.ai.domain.agent.model.valobj.ChatStreamEvent;
import top.kangyaocoding.ai.domain.agent.service.chat.support.DrawioXmlSupport;
import top.kangyaocoding.ai.domain.agent.service.IChatService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @description: 流式对话编排器。
 * 将 ADK 工作流的事件流翻译为面向前端的 ChatStreamEvent 协议流（stage / diagram / message / done / error）：
 * <ol>
 *   <li>按智能体作者的首个事件推进 stage（analyze -> draw -> review），让用户全程看到进度；</li>
 *   <li>XML 不逐 token 下发（半截 XML 无法被 draw.io 解析），而是在绘图/质检智能体产出完整文本后，
 *       经 DrawioXmlSupport 提取校验，以「快照」事件一次性下发，前端画布可立即加载；</li>
 *   <li>命中三字段 JSON 契约时按契约拆分 explanation / diagram；</li>
 *   <li>绘图草稿校验通过时直接收尾（跳过质检智能体的整段 XML 重写，大幅缩短总耗时），
 *       草稿无效则放行质检智能体做修复后以 final 快照下发；</li>
 *   <li>追问轮（type=user）识别后立即终止并取消上游订阅，省去后续智能体的纯透传调用。</li>
 * </ol>
 * @author: herbert
 * @date: 2026-09-04
 */
@Slf4j
@Service
public class ChatStreamOrchestrator {

    private final IChatService chatService;

    public ChatStreamOrchestrator(IChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 发起流式对话并返回协议事件流。
     * 终止事件（done / error）发出后上游自动被取消；若上游正常结束却未产出终止事件（如三个智能体都没给出有效图表），
     * 补发一条 error 兜底，保证前端一定能收到明确的结束语义。
     *
     * @param agentId   智能体 ID
     * @param userId    用户 ID
     * @param sessionId 会话 ID（调用方需先 ensureSession）
     * @param message   用户消息
     * @return 协议事件流
     */
    public Flowable<ChatStreamEvent> stream(String agentId, String userId, String sessionId, String message) {
        return Flowable.defer(() -> {
            StreamState state = new StreamState(sessionId);
            return chatService.handleMessageStream(agentId, userId, sessionId, message)
                    .concatMap(event -> Flowable.fromIterable(processEvent(event, state)))
                    // 终止事件透出后立即完成下游并取消上游（后续智能体不再调用）
                    .takeUntil(ChatStreamEvent::terminal)
                    // 上游无终止事件就正常结束时补发 error，避免前端无限等待
                    .concatWith(Flowable.defer(() -> state.finished
                            ? Flowable.empty()
                            : Flowable.just(ChatStreamEvent.error("本次未能生成有效结果，请重试"))));
        });
    }

    /**
     * 处理单个 ADK 事件，映射为 0..n 个协议事件。
     */
    private List<ChatStreamEvent> processEvent(Event event, StreamState state) {
        List<ChatStreamEvent> out = new ArrayList<>();
        if (state.finished) {
            return out;
        }

        // 每个智能体的首个事件推进一次阶段提示
        String author = event.author() == null ? "unknown" : event.author();
        int authorIndex = state.authors.computeIfAbsent(author, k -> state.authors.size());
        if (!state.stages.contains(author)) {
            out.add(ChatStreamEvent.stage(authorIndex == 0 ? ChatStreamEvent.STAGE_ANALYZE : stageOf(authorIndex), author));
            state.stages.add(author);
        }

        // 流式增量：透传为 token 事件驱动前端打字机效果（正文增量才外发，函数调用帧无正文自然跳过）
        if (event.partial().orElse(false)) {
            String delta = textOf(event);
            if (!delta.isEmpty()) {
                out.add(ChatStreamEvent.token(stageOf(authorIndex), author, delta));
            }
            return out;
        }

        // 仅「轮次终稿」参与协议判定：非 partial、不含函数调用/回执、且带正文。
        // 工具调用循环中的中间确认文本与函数调用事件不产生对用户可见的输出。
        if (!event.functionCalls().isEmpty() || !event.functionResponses().isEmpty()) {
            return out;
        }
        String text = textOf(event);
        if (text.isEmpty()) {
            return out;
        }

        out.addAll(onTurnFinal(text, author, state));
        return out;
    }

    /**
     * 轮次终稿状态机：按三字段 JSON 契约与 XML 校验结果推进/收尾。
     */
    private List<ChatStreamEvent> onTurnFinal(String text, String author, StreamState state) {
        List<ChatStreamEvent> out = new ArrayList<>();
        int turnIndex = state.turnFinals++;
        log.info("流式对话轮次终稿 turnIndex:{} sessionId:{} 长度:{}", turnIndex, state.sessionId, text.length());

        // 1. 命中三字段 JSON 契约（analyst 追问 / drawer 透传 / reviewer 组装的最终结果都走这里）
        JSONObject contract = tryParseContract(text);
        if (null != contract) {
            String type = contract.getString("type");
            String explanation = contract.getString("explanation");
            String diagram = contract.getString("diagram");

            // 追问轮：只要说明文字，终止并取消后续智能体（省去纯透传的 LLM 调用）
            if ("user".equals(type) || (null == diagram && explanation != null && !explanation.isEmpty())) {
                return terminal(out, state,
                        ChatStreamEvent.message("user", explanation),
                        ChatStreamEvent.done(state.sessionId, "user"));
            }

            String xml = extractFromContract(diagram);
            if (DrawioXmlSupport.isValidDrawioXml(xml)) {
                String phase = turnIndex >= 2 ? ChatStreamEvent.PHASE_FINAL : ChatStreamEvent.PHASE_DRAFT;
                String responseType = "drawio".equals(type) ? "drawio" : "mixed";
                return terminal(out, state,
                        ChatStreamEvent.message(responseType, explanation),
                        ChatStreamEvent.diagram(phase, xml),
                        ChatStreamEvent.done(state.sessionId, responseType));
            }

            // 契约里的 diagram 无效：前两轮放行给后续质检智能体修复，最后一轮直接报错
            if (turnIndex < 2) {
                out.add(ChatStreamEvent.stage(ChatStreamEvent.STAGE_REVIEW, author));
                return out;
            }
            return terminal(out, state, ChatStreamEvent.error("图表校验未通过，请重试或补充更具体的需求"));
        }

        // 2. 非 JSON 输出：尝试提取 XML（绘图智能体的裸 XML / 单智能体配置直接出图）
        String xml = DrawioXmlSupport.extractDrawioXml(text);
        if (DrawioXmlSupport.isValidDrawioXml(xml)) {
            // 校验通过即收尾：跳过质检智能体对整段 XML 的重写。说明文字留空，前端用默认文案兜底。
            return terminal(out, state,
                    ChatStreamEvent.message("mixed", ""),
                    ChatStreamEvent.diagram(ChatStreamEvent.PHASE_DRAFT, xml),
                    ChatStreamEvent.done(state.sessionId, "mixed"));
        }

        // 3. 非 JSON 且无 XML 的纯文本：
        //    - 第 1 轮（analyst）的纯文本是给 drawer 看的内部需求描述，推进到绘图；
        //    - 第 2 轮起仍是纯文本，说明工作流没有产出图表（如用户闲聊时各智能体跟聊），
        //      把最后一轮文本作为普通对话回复优雅收尾，而不是误报"校验未通过"。
        if (turnIndex == 0) {
            out.add(ChatStreamEvent.stage(ChatStreamEvent.STAGE_DRAW, author));
            return out;
        }
        return terminal(out, state,
                ChatStreamEvent.message("user", text),
                ChatStreamEvent.done(state.sessionId, "user"));
    }

    /**
     * 追加终止事件序列并标记结束。
     */
    private List<ChatStreamEvent> terminal(List<ChatStreamEvent> out, StreamState state, ChatStreamEvent... events) {
        for (ChatStreamEvent event : events) {
            out.add(event);
        }
        state.finished = true;
        return out;
    }

    /**
     * 解析三字段 JSON 契约；非 JSON 或无有效字段时返回 null，交由后续 XML 提取兜底。
     * 容错：模型可能无视"禁止代码块"约束把 JSON 包进 Markdown 围栏，或在前 后附加说明文字，
     * 因此先剥围栏、再截取首个 '{' 到最后一个 '}' 的片段尝试解析。
     */
    public static JSONObject tryParseContract(String text) {
        String trimmed = null == text ? "" : DrawioXmlSupport.stripMarkdownFence(text.trim());
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            JSONObject json = null;
            if (trimmed.startsWith("{")) {
                json = JSON.parseObject(trimmed);
            } else {
                // 附加文字包裹的兜底：截取首个 { 到最后一个 } 的片段
                int start = trimmed.indexOf('{');
                int end = trimmed.lastIndexOf('}');
                if (start >= 0 && end > start) {
                    json = JSON.parseObject(trimmed.substring(start, end + 1));
                }
            }
            if (null == json) {
                return null;
            }
            String type = json.getString("type");
            boolean hasKnownField = (type != null && !type.isEmpty())
                    || json.containsKey("explanation") || json.containsKey("diagram");
            return hasKnownField ? json : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从契约 diagram 字段提取纯 XML（容忍模型把 XML 包进 Markdown 围栏等残留）。
     */
    private String extractFromContract(String diagram) {
        if (null == diagram || diagram.isEmpty()) {
            return null;
        }
        String extracted = DrawioXmlSupport.extractDrawioXml(diagram);
        return null != extracted ? extracted : diagram;
    }

    /**
     * 按智能体出场顺序映射阶段名：第 1 个为分析，第 2 个为绘图，其后统一为质检。
     */
    private String stageOf(int authorIndex) {
        if (authorIndex == 1) {
            return ChatStreamEvent.STAGE_DRAW;
        }
        return ChatStreamEvent.STAGE_REVIEW;
    }

    /**
     * 提取事件正文（仅文本 part，剔除函数调用/回执，避免 stringifyContent 的调试文案混入协议内容）。
     */
    private String textOf(Event event) {
        return event.content().flatMap(Content::parts).orElse(List.of()).stream()
                .filter(part -> part.functionCall().isEmpty() && part.functionResponse().isEmpty())
                .map(part -> part.text().orElse(""))
                .reduce("", String::concat);
    }

    /**
     * 单次流式对话的可变状态（每次 stream 调用独立持有，无跨请求共享）。
     */
    private static class StreamState {
        private final String sessionId;
        /** 作者 -> 出场顺序（LinkedHashMap 保持首次出现顺序） */
        private final Map<String, Integer> authors = new LinkedHashMap<>();
        /** 已推进过阶段提示的作者集合 */
        private final Set<String> stages = new java.util.HashSet<>();
        /** 已处理的轮次终稿数 */
        private int turnFinals = 0;
        private boolean finished = false;

        private StreamState(String sessionId) {
            this.sessionId = sessionId;
        }
    }
}
