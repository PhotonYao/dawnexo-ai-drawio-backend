package top.kangyaocoding.ai.domain.agent.service.armory.matter.skills;

import org.springframework.ai.tool.ToolCallback;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentConfigTableVO;

/**
 * @description: 工具技能创建服务接口
 * @author: herbert
 * @date: 2026-08-27 19:49
 */
public interface ToolSkillsCreateService {
    ToolCallback[] buildToolCallback(AiAgentConfigTableVO.Module.ChatModel.ToolSkills toolSkills) throws Exception;
}
