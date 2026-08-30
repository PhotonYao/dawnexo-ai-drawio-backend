package top.kangyaocoding.ai.domain.agent.service;

import com.google.adk.events.Event;
import io.reactivex.rxjava3.core.Flowable;
import top.kangyaocoding.ai.domain.agent.model.entity.ChatCommandEntity;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentConfigTableVO;

import java.util.List;

/**
 * @description: 对话服务接口
 * @author: herbert
 * @date: 2026-08-11 21:27
 */
public interface IChatService {

    /**
     * 查询已配置的智能体列表。
     *
     * @return 智能体配置列表，若未配置任何智能体则返回空列表
     */
    List<AiAgentConfigTableVO.Agent> queryAiAgentConfigList();

    /**
     * 为指定用户与某个智能体创建全新会话。
     * 每次调用都会创建独立的新会话，保证每一次对话都有可独立定位的 sessionId，
     * 避免不同对话之间共享会话导致的上下文交叉污染。
     *
     * @param agentId 智能体 ID
     * @param userId  用户 ID
     * @return 新创建的会话 ID
     */
    String createSession(String agentId, String userId);

    /**
     * 校验并确保会话可用，返回一个归属于指定智能体与用户的有效会话 ID。
     * 用于防止会话交叉污染：会话 ID 为空、绑定丢失（服务重启）或归属与请求的
     * 智能体/用户不一致时，自动创建全新会话隔离上下文。
     *
     * @param agentId   智能体 ID
     * @param userId    用户 ID
     * @param sessionId 前端传入的会话 ID（可为空）
     * @return 归属校验通过或自愈后的有效会话 ID
     */
    String ensureSession(String agentId, String userId, String sessionId);

    /**
     * 向指定智能体发送消息并获取完整回复。
     * 内部会自动创建或复用用户会话，并阻塞等待所有事件处理完成。
     *
     * @param agentId 智能体 ID
     * @param userId  用户 ID
     * @param message 用户消息内容
     * @return 智能体回复内容列表（每个元素为大模型输出的一个内容片段）
     */
    List<String> handleMessage(String agentId, String userId, String message);

    /**
     * 向指定智能体在指定会话中发送消息并获取完整回复。
     *
     * @param agentId   智能体 ID
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @param message   用户消息内容
     * @return 智能体回复内容列表（每个元素为大模型输出的一个内容片段）
     */
    List<String> handleMessage(String agentId, String userId, String sessionId, String message);

    /**
     * 根据聊天指令实体向智能体发送消息并获取完整回复。
     * 支持在指令中携带文本、文件或内联数据等多模态内容。
     *
     * @param chatCommandEntity 聊天指令实体，包含智能体 ID、用户 ID、会话 ID 及消息内容
     * @return 智能体回复内容列表（每个元素为大模型输出的一个内容片段）
     */
    List<String> handleMessage(ChatCommandEntity chatCommandEntity);

    /**
     * 向指定智能体在指定会话中发送消息并以流式方式返回回复事件。
     *
     * @param agentId   智能体 ID
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @param message   用户消息内容
     * @return 事件流 {@link Flowable}，订阅后可逐个接收智能体产生的事件
     */
    Flowable<Event> handleMessageStream(String agentId, String userId, String sessionId, String message);

    /**
     * 根据聊天指令实体向智能体发送消息并以流式方式返回回复事件。
     * 支持在指令中携带文本、文件或内联数据等多模态内容。
     *
     * @param chatCommandEntity 聊天指令实体，包含智能体 ID、用户 ID、会话 ID 及消息内容
     * @return 事件流 {@link Flowable}，订阅后可逐个接收智能体产生的事件
     */
    Flowable<Event> handleMessageStream(ChatCommandEntity chatCommandEntity);
}
