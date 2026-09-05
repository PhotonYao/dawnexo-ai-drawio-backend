package top.kangyaocoding.ai.domain.agent.model.valobj;

import lombok.Builder;
import lombok.Data;

/**
 * @description: 流式对话事件值对象。
 * chat_stream SSE 通路的统一事件契约，type 标识事件种类，其余字段按种类选用：
 * <ul>
 *   <li>stage   —— 工作流阶段推进（stage: analyze / draw / review，author 为当前智能体名），
 *       前端更新等待提示 / 执行轨迹行状态</li>
 *   <li>token   —— 流式增量文本（author 所属智能体 + stage 当前阶段 + delta 增量内容），
 *       前端据此做打字机效果；XML 增量也会流出，由前端放进执行轨迹区展示</li>
 *   <li>diagram —— 完整且已通过校验的 draw.io XML 快照（phase: draft / final），前端直接加载画布；
 *       XML 不做逐 token 下发，因为半截 XML 无法被 draw.io 解析</li>
 *   <li>message —— 给用户阅读的完整文本（responseType: user / mixed / drawio，explanation 为文案）</li>
 *   <li>done    —— 正常结束（sessionId 回传给前端更新会话，type 为本次回复类型）</li>
 *   <li>error   —— 失败（message 为错误说明）</li>
 * </ul>
 * @author: herbert
 * @date: 2026-09-04
 */
@Data
@Builder
public class ChatStreamEvent {

    public static final String TYPE_STAGE = "stage";
    public static final String TYPE_TOKEN = "token";
    public static final String TYPE_DIAGRAM = "diagram";
    public static final String TYPE_MESSAGE = "message";
    public static final String TYPE_DONE = "done";
    public static final String TYPE_ERROR = "error";

    /** 工作流阶段：分析 */
    public static final String STAGE_ANALYZE = "analyze";
    /** 工作流阶段：绘图 */
    public static final String STAGE_DRAW = "draw";
    /** 工作流阶段：检查修复 */
    public static final String STAGE_REVIEW = "review";

    /** 图表快照阶段：草稿（drawer 产出） */
    public static final String PHASE_DRAFT = "draft";
    /** 图表快照阶段：终稿（reviewer 产出） */
    public static final String PHASE_FINAL = "final";

    /** 事件类型：stage / token / diagram / message / done / error */
    private String type;

    /** 事件所属智能体名（stage / token 事件携带，如 agent_analyst） */
    private String author;

    /** stage 事件：当前阶段 analyze / draw / review */
    private String stage;

    /** token 事件：流式增量文本 */
    private String delta;

    /** diagram 事件：快照阶段 draft / final */
    private String phase;

    /** diagram 事件：完整且校验通过的 draw.io XML */
    private String xml;

    /** message / done 事件：回复类型 user / mixed / drawio */
    private String responseType;

    /** message 事件：给用户阅读的说明/追问文本 */
    private String explanation;

    /** done 事件：本次对话实际使用的会话 ID（自愈后可能更新，前端以此为准） */
    private String sessionId;

    /** error 事件：错误说明 */
    private String message;

    public static ChatStreamEvent stage(String stage, String author) {
        return ChatStreamEvent.builder().type(TYPE_STAGE).stage(stage).author(author).build();
    }

    public static ChatStreamEvent token(String stage, String author, String delta) {
        return ChatStreamEvent.builder().type(TYPE_TOKEN).stage(stage).author(author).delta(delta).build();
    }

    public static ChatStreamEvent diagram(String phase, String xml) {
        return ChatStreamEvent.builder().type(TYPE_DIAGRAM).phase(phase).xml(xml).build();
    }

    public static ChatStreamEvent message(String responseType, String explanation) {
        return ChatStreamEvent.builder().type(TYPE_MESSAGE).responseType(responseType).explanation(explanation).build();
    }

    public static ChatStreamEvent done(String sessionId, String responseType) {
        return ChatStreamEvent.builder().type(TYPE_DONE).sessionId(sessionId).responseType(responseType).build();
    }

    public static ChatStreamEvent error(String message) {
        return ChatStreamEvent.builder().type(TYPE_ERROR).message(message).build();
    }

    /** 是否为终止事件（done / error），编排器据此收尾并取消上游 */
    public boolean terminal() {
        return TYPE_DONE.equals(type) || TYPE_ERROR.equals(type);
    }
}
