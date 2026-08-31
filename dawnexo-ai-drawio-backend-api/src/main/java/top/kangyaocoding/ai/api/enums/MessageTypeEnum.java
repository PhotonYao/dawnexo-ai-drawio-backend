package top.kangyaocoding.ai.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @description: 智能体回复消息类型
 * @author: herbert
 * @date: 2026-08-31
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum MessageTypeEnum {

    USER("user", "需要用户交互，explanation 为对话文本"),
    DRAWIO("drawio", "纯绘图数据，diagram 为 draw.io XML"),
    MIXED("mixed", "说明文字 + 绘图数据，explanation 与 diagram 同时存在"),

    ;

    private String code;
    private String desc;

    public static MessageTypeEnum formType(String code) {
        if (code == null) {
            return null;
        }

        for (MessageTypeEnum value : values()) {
            if (value.getCode().equalsIgnoreCase(code)) {
                return value;
            }
        }

        return null;
    }
}
