package top.kangyaocoding.ai.domain.agent.service.armory.matter.skills.impl;

import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import top.kangyaocoding.ai.domain.agent.service.armory.matter.skills.ToolSkillsCreateService;

import java.util.ArrayList;
import java.util.List;

/**
 * @description: 工具技能创建服务接口实现类
 * @author: herbert
 * @date: 2026-08-27 19:53
 */
@Slf4j
@Service
public class DefaultToolSkillsCreateService implements ToolSkillsCreateService {
    @Override
    public ToolCallback[] buildToolCallback(AiAgentConfigTableVO.Module.ChatModel.ToolSkills toolSkills) throws Exception {
        String type = toolSkills.getType();
        String path = toolSkills.getPath();

        List<ToolCallback> toolCallbackList = new ArrayList<>();

        if ("directory".equals(type)) {
            // 读取目录下的所有工具，并创建ToolCallback
            ToolCallback toolCallback = SkillsTool.builder()
                    .addSkillsDirectory(path)
                    .build();
            toolCallbackList.add(toolCallback);
        }

        if ("resource".equals(type)) {
            // 读取资源文件中的工具，并创建ToolCallback
            ToolCallback toolCallback = SkillsTool.builder()
                    .addSkillsResource(new ClassPathResource(path))
                    .build();
            toolCallbackList.add(toolCallback);
        }

        return toolCallbackList.toArray(new ToolCallback[0]);
    }
}
