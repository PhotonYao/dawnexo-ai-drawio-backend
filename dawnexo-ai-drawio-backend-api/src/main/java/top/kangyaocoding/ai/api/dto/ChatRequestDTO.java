package top.kangyaocoding.ai.api.dto;

import lombok.Data;

/**
 * @description: 聊天请求
 * @author: herbert
 * @date: 2026-08-24 19:39
 */
@Data
public class ChatRequestDTO {

    private String agentId;
    private String userId;
    private String sessionId;
    private String message;

}
