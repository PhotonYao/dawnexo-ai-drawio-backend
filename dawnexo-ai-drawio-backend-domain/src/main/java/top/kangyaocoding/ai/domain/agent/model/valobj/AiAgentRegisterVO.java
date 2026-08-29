package top.kangyaocoding.ai.domain.agent.model.valobj;

import com.google.adk.runner.InMemoryRunner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @description: 智能体注册值对象
 * @author: herbert
 * @date: 2026-08-11 21:52
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiAgentRegisterVO {
    /**
     * 智能体应用名称
     */
    private String appName;

    /**
     * 智能体 ID
     */
    private String agentId;

    /**
     * 智能体名称
     */
    private String agentName;

    /**
     * 智能体描述
     */
    private String agentDesc;

    /**
     * 智能体执行对象
     */
    private InMemoryRunner runner;
}
