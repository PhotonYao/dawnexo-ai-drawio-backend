package top.kangyaocoding.ai.domain.agent.model.valobj.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentConfigTableVO;

import java.util.Map;

@Data
@ConfigurationProperties(prefix = "ai.agent.config", ignoreInvalidFields = true)
public class AiAgentAutoConfigProperties {
    /**
     * 是否启用 AI Agent 自动装配
     */
    private boolean enabled = false;

    Map<String, AiAgentConfigTableVO> tables;
}
