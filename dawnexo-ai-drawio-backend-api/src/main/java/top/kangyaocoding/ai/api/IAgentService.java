package top.kangyaocoding.ai.api;

import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import top.kangyaocoding.ai.api.dto.*;
import top.kangyaocoding.ai.api.response.Response;

import java.util.List;

/**
 * @description: 智能体服务接口
 * @author: herbert
 * @date: 2026-08-24 19:15
 */
public interface IAgentService {

    /**
     * 查询智能体配置列表
     *
     * @return 智能体配置列表
     */
    Response<List<AiAgentConfigResponseDTO>> queryAiAgentConfigList();

    /**
     * 创建会话
     *
     * @param createSessionRequestDTO 创建会话请求参数
     * @return 创建会话响应参数
     */
    Response<CreateSessionResponseDTO> createSession(CreateSessionRequestDTO requestDTO);

    /**
     * 聊天
     *
     * @param chatRequestDTO 聊天请求参数
     * @return 聊天响应参数
     */
    Response<ChatResponseDTO> chat(ChatRequestDTO chatRequestDTO);

    /**
     * 聊天流
     *
     * @param requestDTO 聊天流请求参数
     * @return 聊天流响应参数
     */
    ResponseBodyEmitter chatStream(ChatRequestDTO requestDTO);

}
