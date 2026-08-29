package top.kangyaocoding.ai.trigger.http;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import top.kangyaocoding.ai.api.IAgentService;
import top.kangyaocoding.ai.api.dto.*;
import top.kangyaocoding.ai.api.response.Response;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import top.kangyaocoding.ai.domain.agent.service.IChatService;
import top.kangyaocoding.ai.types.enums.ResponseCode;
import top.kangyaocoding.ai.types.exception.AppException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @description: 智能体服务
 * @author: herbert
 * @date: 2026-08-24 19:48
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/")
@CrossOrigin(origins = "*")
public class AgentServiceController implements IAgentService {

    @Resource
    private IChatService chatService;

    /**
     * 查询智能体配置列表接口
     * 该接口用于获取所有智能体的配置信息，并以列表形式返回
     *
     * @return Response<List<AiAgentConfigResponseDTO>> 包含智能体配置列表的响应对象
     */
    @RequestMapping(value = "query_ai_agent_config_list", method = RequestMethod.GET)
    @Override
    public Response<List<AiAgentConfigResponseDTO>> queryAiAgentConfigList() {
        try {
            // 记录查询智能体配置列表的操作日志
            log.info("查询智能体配置列表");

            // 调用服务层方法查询智能体配置列表数据
            List<AiAgentConfigTableVO.Agent> agentConfigs = chatService.queryAiAgentConfigList();

            // 将VO对象转换为DTO对象，用于API响应
            List<AiAgentConfigResponseDTO> responseDTOS = agentConfigs.stream().map(agentConfig -> {
                // 创建响应DTO对象
                AiAgentConfigResponseDTO aiAgentConfigResponseDTO = new AiAgentConfigResponseDTO();
                // 设置智能体ID
                aiAgentConfigResponseDTO.setAgentId(agentConfig.getAgentId());
                // 设置智能体名称
                aiAgentConfigResponseDTO.setAgentName(agentConfig.getAgentName());
                // 设置智能体描述
                aiAgentConfigResponseDTO.setAgentDesc(agentConfig.getAgentDesc());
                return aiAgentConfigResponseDTO;
            }).collect(Collectors.toList());

            // 构建并返回成功响应
            return Response.<List<AiAgentConfigResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())    // 设置响应码为成功
                    .info(ResponseCode.SUCCESS.getInfo())     // 设置响应信息
                    .data(responseDTOS)                      // 设置响应数据
                    .build();
        } catch (AppException e) {
            // 捕获应用异常并记录错误日志
            log.error("查询智能体配置列表异常: {}", e.getMessage(), e);
            // 返回业务异常响应
            return Response.<List<AiAgentConfigResponseDTO>>builder()
                    .code(e.getCode())        // 设置异常码
                    .info(e.getMessage())     // 设置异常信息
                    .build();
        } catch (Exception e) {
            // 捕获系统异常并记录错误日志
            log.error("查询智能体配置列表失败: {}", e.getMessage(), e);
            // 返回系统错误响应
            return Response.<List<AiAgentConfigResponseDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())    // 设置系统错误码
                    .info(ResponseCode.UN_ERROR.getInfo())     // 设置系统错误信息
                    .build();
        }
    }

    /**
     * 创建会话接口POST
     * 该接口用于处理创建会话的请求，通过POST方式访问
     *
     * @param requestDTO 包含创建会话所需的请求参数，包括agentId和userId
     * @return 返回一个Response对象，其中包含创建会话的结果和相关信息
     */
    @RequestMapping(value = "create_session", method = RequestMethod.POST)
    @Override
    public Response<CreateSessionResponseDTO> createSession(@RequestBody CreateSessionRequestDTO requestDTO) {
        try {
            // 记录创建会话的日志信息，包含agentId和userId
            log.info("创建会话 agentId:{} userId:{}", requestDTO.getAgentId(), requestDTO.getUserId());
            // 调用chatService的createSession方法创建会话，并获取sessionId
            String sessionId = chatService.createSession(requestDTO.getAgentId(), requestDTO.getUserId());

            // 创建响应对象，并设置sessionId
            CreateSessionResponseDTO responseDTO = new CreateSessionResponseDTO();
            responseDTO.setSessionId(sessionId);

            // 构建并返回成功的响应对象，包含成功状态码、信息数据和响应数据
            return Response.<CreateSessionResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTO)
                    .build();
        } catch (AppException e) {
            // 捕获应用异常，记录错误日志，并返回包含错误信息的响应对象
            log.error("查询智能体配置列表异常", e);
            return Response.<CreateSessionResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            // 捕获其他异常，记录错误日志，并返回通用错误响应对象
            log.error("创建会话失败 agentId:{} userId:{}", requestDTO.getAgentId(), requestDTO.getUserId(), e);
            return Response.<CreateSessionResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * 创建会话接口GET
     * 通过GET请求方式处理"/create_session"路径的请求
     *
     * @param agentId 代理ID，从请求参数中获取
     * @param userId  用户ID，从请求参数中获取
     * @return 返回创建会话的响应结果，类型为Response<CreateSessionResponseDTO>
     */
    @RequestMapping(value = "create_session", method = RequestMethod.GET)
    public Response<CreateSessionResponseDTO> createSession(@RequestParam("agentId") String agentId, @RequestParam("userId") String userId) {
        CreateSessionRequestDTO requestDTO = new CreateSessionRequestDTO();  // 创建请求DTO对象
        requestDTO.setAgentId(agentId);  // 设置请求DTO中的agentId
        requestDTO.setUserId(userId);  // 设置请求DTO中的userId
        return createSession(requestDTO);  // 调用重载的createSession方法，并返回结果
    }

    /**
     * 智能体对话接口
     * 该方法接收用户发送的消息，处理后返回智能体的回复
     *
     * @param requestDTO 包含对话请求信息的DTO对象，包含agentId、userId、sessionId和message等字段
     * @return Response<ChatResponseDTO> 包含对话响应的封装对象
     */
    @RequestMapping(value = "chat", method = RequestMethod.POST)
    @Override
    public Response<ChatResponseDTO> chat(@RequestBody ChatRequestDTO requestDTO) {
        try {
            // 记录开始对话的日志信息
            log.info("智能体对话 agentId:{} userId:{}", requestDTO.getAgentId(), requestDTO.getUserId());
            // 获取会话ID，如果不存在则创建新的会话
            String sessionId = requestDTO.getSessionId();
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = chatService.createSession(requestDTO.getAgentId(), requestDTO.getUserId());
            }

            // 处理用户消息并获取智能体回复
            List<String> messages = chatService.handleMessage(requestDTO.getAgentId(), requestDTO.getUserId(), sessionId, requestDTO.getMessage());

            // 构建响应对象
            ChatResponseDTO responseDTO = new ChatResponseDTO();

            // 尝试获取最后一条消息并解析
            try {
                String result = messages.stream()
                        .skip(messages.size() - 1)  // 跳过前面的所有元素
                        .findFirst()
                        .orElse("");
                ChatResponseDTO parsed = JSON.parseObject(result, ChatResponseDTO.class);

                if (ObjectUtils.isNotEmpty(parsed)) {
                    responseDTO = parsed;
                    if (StringUtils.isBlank(responseDTO.getType())) {
                        responseDTO.setType("user");
                    }
                } else {
                    responseDTO.setType("user");
                    responseDTO.setContent(String.join("\n", messages));
                }
            } catch (Exception e) {
                responseDTO.setType("user");
                responseDTO.setContent(String.join("\n", messages));
            }

            // 返回成功响应
            return Response.<ChatResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTO)
                    .build();
        } catch (AppException e) {
            // 处理应用异常
            log.error("智能体对话异常", e);
            return Response.<ChatResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("智能体对话败 agentId:{} userId:{}", requestDTO.getAgentId(), requestDTO.getUserId(), e);
            return Response.<ChatResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * 流式聊天请求的接口
     * 该方法接收一个聊天请求DTO，返回一个响应体发射器，用于流式返回聊天响应
     *
     * @param requestDTO 聊天请求数据传输对象，包含agentId、userId、sessionId和message等信息
     * @return ResponseBodyEmitter 用于流式发送响应的发射器，设置超时时间为3分钟
     */
    @RequestMapping(value = "chat_stream",
            method = RequestMethod.POST,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Override
    public ResponseBodyEmitter chatStream(@RequestBody ChatRequestDTO requestDTO) {
        // 创建响应体发射器，设置超时时间为3分钟
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(3 * 60 * 1000L);

        try {
            log.info("流式对话 agentId:{} userId:{} sessionId:{} message:{}", requestDTO.getAgentId(), requestDTO.getUserId(), requestDTO.getSessionId(), requestDTO.getMessage());
            chatService.handleMessageStream(requestDTO.getAgentId(), requestDTO.getUserId(), requestDTO.getSessionId(), requestDTO.getMessage())
                    .subscribe(
                            event -> {
                                try {
                                    emitter.send(event.stringifyContent());
                                } catch (Exception e) {
                                    log.error("流式对话发送失败", e);
                                    emitter.completeWithError(e);
                                }
                            },
                            emitter::completeWithError,
                            emitter::complete
                    );
        } catch (Exception e) {
            log.error("流式对话失败", e);
            emitter.completeWithError(e);
        }
        return emitter;
    }
}
