package top.kangyaocoding.ai.api.dto;

import lombok.Data;

/**
 * @description: 聊天响应
 * @author: herbert
 * @date: 2026-08-24 19:38
 */
@Data
public class ChatResponseDTO {

    /**
     * 回复类型："user"（向用户追问补充信息）或 "drawio"（content 为 draw.io XML）
     */
    private String type;

    /**
     * 回复内容：type=user 时为对话文本；type=drawio 时为 draw.io XML
     */
    private String content;

    /**
     * 本次对话实际使用的会话 ID。
     * 服务端会对入参会话做归属校验，校验失败/绑定丢失时自动重建会话，
     * 前端应以该回传值为准更新本地会话 ID，防止会话交叉污染。
     */
    private String sessionId;
}
