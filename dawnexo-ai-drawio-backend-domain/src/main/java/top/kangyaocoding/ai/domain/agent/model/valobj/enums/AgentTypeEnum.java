package top.kangyaocoding.ai.domain.agent.model.valobj.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @description: 智能体工作流枚举
 * @author: herbert
 * @date: 2026-08-16 19:47
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum AgentTypeEnum {

    Loop("循环执行", "loop", "loopAgentNode"),
    Parallel("并行执行", "parallel", "parallelAgentNode"),
    Sequential("串行执行", "sequential", "sequentialAgentNode"),

    ;

    private String name;
    private String type;
    private String node;

    public static AgentTypeEnum formType(String type) {
        if (type == null) {
            return null;
        }

        for (AgentTypeEnum value : values()) {
            if (value.getType().equalsIgnoreCase(type)) {
                return value;
            }
        }

        return null;
    }
}
