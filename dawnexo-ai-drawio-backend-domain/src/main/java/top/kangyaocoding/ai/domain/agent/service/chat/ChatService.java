package top.kangyaocoding.ai.domain.agent.service.chat;

import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import top.kangyaocoding.ai.domain.agent.model.entity.ChatCommandEntity;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import top.kangyaocoding.ai.domain.agent.model.valobj.properties.AiAgentAutoConfigProperties;
import top.kangyaocoding.ai.domain.agent.service.IChatService;
import top.kangyaocoding.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import top.kangyaocoding.ai.types.enums.ResponseCode;
import top.kangyaocoding.ai.types.exception.AppException;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: 对话服务实现类
 * @author: herbert
 * @date: 2026-08-11 21:33
 */
@Slf4j
@Service
public class ChatService implements IChatService {

    @Resource
    private DefaultArmoryFactory defaultArmoryFactory;

    @Resource
    private AiAgentAutoConfigProperties aiAgentAutoConfigProperties;

    /**
     * 会话绑定关系：记录 sessionId 归属的智能体与用户，用于防止会话交叉污染
     */
    private record SessionBinding(String agentId, String userId, String appName) {
    }

    /**
     * 会话绑定注册表：sessionId -> 归属信息（agentId + userId）。
     * 注意：会话与绑定均保存在内存中，服务重启后绑定丢失，对话时将自动重建会话。
     */
    private final Map<String, SessionBinding> sessionBindings = new ConcurrentHashMap<>();

    /**
     * 查询已配置的智能体列表。
     *
     * @return 智能体配置列表，若未配置任何智能体则返回空列表
     */
    @Override
    public List<AiAgentConfigTableVO.Agent> queryAiAgentConfigList() {
        // 获取所有智能体配置表
        Map<String, AiAgentConfigTableVO> tables = aiAgentAutoConfigProperties.getTables();

        List<AiAgentConfigTableVO.Agent> agentList = new ArrayList<>();
        // 配置为空时记录错误日志并返回空列表
        if (null == tables || CollectionUtils.isEmpty(tables.values())) {
            log.error("未配置任何智能体，请检查配置文件");
            return agentList;
        }

        // 遍历配置表，收集每个表中已注册的智能体
        for (AiAgentConfigTableVO tableVO : tables.values()) {
            if (null != tableVO.getAgent()) {
                agentList.add(tableVO.getAgent());
            }
        }

        log.info("查询到的智能体列表: {}", agentList);
        return agentList;
    }

    /**
     * 为指定用户与某个智能体创建全新会话。
     * 每次调用都会创建独立的新会话，保证每一次对话都有可独立定位的 sessionId，
     * 避免不同对话之间共享会话导致的上下文交叉污染。
     *
     * @param agentId 智能体 ID
     * @param userId  用户 ID
     * @return 新创建的会话 ID
     */
    @Override
    public String createSession(String agentId, String userId) {
        // 获取智能体注册信息（包含应用名与运行器）
        AiAgentRegisterVO aiAgentRegisterVO = getAiAgentRegisterVO(agentId);

        String appName = aiAgentRegisterVO.getAppName();
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        // 每次调用均创建全新会话，并登记会话归属绑定关系
        Session session = runner.sessionService().createSession(appName, userId).blockingGet();
        sessionBindings.put(session.id(), new SessionBinding(agentId, userId, appName));
        log.info("创建会话 agentId:{} userId:{} sessionId:{}", agentId, userId, session.id());
        return session.id();
    }

    /**
     * 校验并确保会话可用，返回一个归属于指定智能体与用户的有效会话 ID。
     * 防止会话交叉污染：
     * 1. 会话 ID 为空时，创建新会话；
     * 2. 会话绑定不存在（如服务重启内存丢失）时，重建新会话（历史上下文不可恢复）；
     * 3. 会话归属与请求的智能体/用户不一致时，隔离为全新会话，避免上下文串扰。
     *
     * @param agentId   智能体 ID
     * @param userId    用户 ID
     * @param sessionId 前端传入的会话 ID（可为空）
     * @return 归属校验通过或自愈后的有效会话 ID
     */
    @Override
    public String ensureSession(String agentId, String userId, String sessionId) {
        // 会话 ID 为空，直接创建新会话
        if (null == sessionId || sessionId.isEmpty()) {
            return createSession(agentId, userId);
        }

        SessionBinding binding = sessionBindings.get(sessionId);
        // 绑定不存在（服务重启等），重建会话自愈
        if (null == binding) {
            log.warn("会话绑定不存在，重建会话 sessionId:{}", sessionId);
            return createSession(agentId, userId);
        }
        // 会话归属校验失败，隔离为全新会话，防止交叉污染
        if (!binding.agentId().equals(agentId) || !binding.userId().equals(userId)) {
            log.warn("会话与智能体或用户不匹配，隔离为新会话 sessionId:{} 绑定agentId:{} 请求agentId:{}",
                    sessionId, binding.agentId(), agentId);
            return createSession(agentId, userId);
        }
        return sessionId;
    }

    /**
     * 向指定智能体发送消息并获取完整回复。
     * 内部会创建全新会话（每次调用即一次独立对话），并阻塞等待所有事件处理完成。
     *
     * @param agentId 智能体 ID
     * @param userId  用户 ID
     * @param message 用户消息内容
     * @return 智能体回复内容列表（每个元素为大模型输出的一个内容片段）
     */
    @Override
    public List<String> handleMessage(String agentId, String userId, String message) {
        // 获取智能体注册信息
        AiAgentRegisterVO aiAgentRegisterVO = getAiAgentRegisterVO(agentId);

        // 创建或复用用户会话
        String session = createSession(agentId, userId);

        // 复用带会话 ID 的重载方法处理消息
        return handleMessage(agentId, userId, session, message);
    }

    /**
     * 向指定智能体在指定会话中发送消息并获取完整回复。
     *
     * @param agentId   智能体 ID
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @param message   用户消息内容
     * @return 智能体回复内容列表（每个元素为大模型输出的一个内容片段）
     */
    @Override
    public List<String> handleMessage(String agentId, String userId, String sessionId, String message) {
        // 获取智能体注册信息
        AiAgentRegisterVO aiAgentRegisterVO = getAiAgentRegisterVO(agentId);

        // 获取智能体运行器
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        // 将用户文本消息封装为 Content
        Content userMsg = Content.fromParts(Part.fromText(message));
        // 异步执行智能体并获取事件流
        Flowable<Event> events = runner.runAsync(userId, sessionId, userMsg);

        List<String> outputs = new ArrayList<>();
        // 阻塞遍历所有事件，收集每个事件的文本内容作为回复
        events.blockingForEach(event -> outputs.add(event.stringifyContent()));

        return outputs;
    }

    /**
     * 根据聊天指令实体向智能体发送消息并获取完整回复。
     * 支持在指令中携带文本、文件或内联数据等多模态内容。
     *
     * @param chatCommandEntity 聊天指令实体，包含智能体 ID、用户 ID、会话 ID 及消息内容
     * @return 智能体回复内容列表（每个元素为大模型输出的一个内容片段）
     */
    @Override
    public List<String> handleMessage(ChatCommandEntity chatCommandEntity) {
        // 获取智能体注册信息
        AiAgentRegisterVO aiAgentRegisterVO = getAiAgentRegisterVO(chatCommandEntity.getAgentId());
        // 获取智能体运行器
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        // 根据指令实体构建多模态 Content
        Content content = buildContentFromChatCommand(chatCommandEntity);
        String userId = chatCommandEntity.getUserId();
        String sessionId = chatCommandEntity.getSessionId();

        // 异步执行智能体并获取事件流
        Flowable<Event> events = runner.runAsync(userId, sessionId, content);

        List<String> outputs = new ArrayList<>();
        // 阻塞遍历所有事件，收集每个事件的文本内容作为回复
        events.blockingForEach(event -> outputs.add(event.stringifyContent()));

        return outputs;
    }

    /**
     * 向指定智能体在指定会话中发送消息并以流式方式返回回复事件。
     *
     * @param agentId   智能体 ID
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @param message   用户消息内容
     * @return 事件流 {@link Flowable}，订阅后可逐个接收智能体产生的事件
     */
    @Override
    public Flowable<Event> handleMessageStream(String agentId, String userId, String sessionId, String message) {
        // 获取智能体注册信息
        AiAgentRegisterVO aiAgentRegisterVO = getAiAgentRegisterVO(agentId);

        // 获取智能体运行器
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();
        // 将用户文本消息封装为 Content
        Content userMsg = Content.fromParts(Part.fromText(message));

        // 直接返回异步事件流，由调用方订阅以获取流式回复
        return runner.runAsync(userId, sessionId, userMsg);
    }

    /**
     * 根据聊天指令实体向智能体发送消息并以流式方式返回回复事件。
     * 支持在指令中携带文本、文件或内联数据等多模态内容。
     *
     * @param chatCommandEntity 聊天指令实体，包含智能体 ID、用户 ID、会话 ID 及消息内容
     * @return 事件流 {@link Flowable}，订阅后可逐个接收智能体产生的事件
     */
    @Override
    public Flowable<Event> handleMessageStream(ChatCommandEntity chatCommandEntity) {
        // 获取智能体注册信息
        AiAgentRegisterVO aiAgentRegisterVO = getAiAgentRegisterVO(chatCommandEntity.getAgentId());
        // 获取智能体运行器
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        // 根据指令实体构建多模态 Content
        Content content = buildContentFromChatCommand(chatCommandEntity);
        String userId = chatCommandEntity.getUserId();
        String sessionId = chatCommandEntity.getSessionId();

        // 直接返回异步事件流，由调用方订阅以获取流式回复
        return runner.runAsync(userId, sessionId, content);
    }

    /**
     * 获取 AI Agent 注册信息
     */
    private AiAgentRegisterVO getAiAgentRegisterVO(String agentId) {
        // 从武器库工厂获取智能体注册信息
        AiAgentRegisterVO aiAgentRegisterVO = defaultArmoryFactory.getAiAgentRegisterVO(agentId);
        // 未注册时抛出业务异常
        if (null == aiAgentRegisterVO) {
            throw new AppException(ResponseCode.E0001.getCode());
        }
        return aiAgentRegisterVO;
    }

    /**
     * 从 ChatCommandEntity 构建 Content 对象
     */
    private Content buildContentFromChatCommand(ChatCommandEntity chatCommandEntity) {
        List<Part> parts = new ArrayList<>();

        // 添加文本内容
        List<ChatCommandEntity.Content.Text> textList = chatCommandEntity.getTextList();
        if (!CollectionUtils.isEmpty(textList)) {
            for (ChatCommandEntity.Content.Text text : textList) {
                parts.add(Part.fromText(text.getMessage()));
            }
        }

        // 添加文件内容
        List<ChatCommandEntity.Content.File> fileList = chatCommandEntity.getFileList();
        if (!CollectionUtils.isEmpty(fileList)) {
            for (ChatCommandEntity.Content.File file : fileList) {
                parts.add(Part.fromUri(file.getFileUri(), file.getMimeType()));
            }
        }

        // 添加内联数据内容
        List<ChatCommandEntity.Content.InlineData> inlineDataList = chatCommandEntity.getInlineDataList();
        if (!CollectionUtils.isEmpty(inlineDataList)) {
            for (ChatCommandEntity.Content.InlineData inlineData : inlineDataList) {
                parts.add(Part.fromBytes(inlineData.getBytes(), inlineData.getMimeType()));
            }
        }

        return Content.builder().role("user").parts(parts).build();
    }
}
