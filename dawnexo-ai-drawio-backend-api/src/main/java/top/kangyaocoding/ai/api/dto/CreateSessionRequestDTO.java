package top.kangyaocoding.ai.api.dto;

import lombok.Data;

/**
 * @description: 创建会话请求
 * @author: herbert
 * @date: 2026-08-24 19:35
 */
@Data
public class CreateSessionRequestDTO {

    private String agentId;

    private String userId;
}
