package top.kangyaocoding.ai.domain.agent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentConfigTableVO;

/**
 * @description: 装配命令
 * @author: herbert
 * @date: 2026-08-11 21:14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArmoryCommandEntity {
    private AiAgentConfigTableVO aiAgentConfigTableVO;
}
