package top.kangyaocoding.ai.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * @description: 聊天响应
 * @author: herbert
 * @date: 2026-08-24 19:38
 */
@Data
public class ChatResponseDTO {

    /**
     * 回复类型："user"（向用户追问补充信息）、"drawio"（diagram 为 draw.io XML）或 "mixed"（explanation 与 diagram 同时存在）。
     * 服务端在 type 为空时会按 explanation / diagram 字段的存在性自动推断。
     */
    private String type;

    /**
     * 文本说明：给用户阅读的解释或追问内容
     */
    private String explanation;

    /**
     * 绘图数据：draw.io XML，交由程序解析渲染到画布
     */
    private String diagram;

    /**
     * 本次对话实际使用的会话 ID。
     * 服务端会对入参会话做归属校验，校验失败/绑定丢失时自动重建会话，
     * 前端应以该回传值为准更新本地会话 ID，防止会话交叉污染。
     */
    private String sessionId;

    public boolean hasExplanation() {
        return explanation != null && !explanation.isBlank();
    }

    public boolean hasDiagram() {
        return diagram != null && !diagram.isBlank();
    }

    /**
     * 是否同时包含说明文字与图表数据（避免被序列化进响应契约）
     */
    @JsonIgnore
    public boolean isMixed() {
        return hasExplanation() && hasDiagram();
    }
}
