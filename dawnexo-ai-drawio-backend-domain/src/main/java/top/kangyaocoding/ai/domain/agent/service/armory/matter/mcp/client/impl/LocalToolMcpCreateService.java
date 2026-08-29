package top.kangyaocoding.ai.domain.agent.service.armory.matter.mcp.client.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import top.kangyaocoding.ai.domain.agent.service.armory.matter.mcp.client.ToolMcpCreateService;

/**
 * @description: 类的描述信息
 * @author: herbert
 * @date: 2026-08-22 13:50
 */
@Slf4j
@Service
public class LocalToolMcpCreateService implements ToolMcpCreateService {

    @Resource
    private ApplicationContext applicationContext;

    @Override
    public ToolCallback[] buildToolCallback(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) throws Exception {
        log.info("构建 ToolCallback - LocalToolMcpCreateService");

        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.LocalServerParameters localConfig = toolMcp.getLocal();

        ToolCallbackProvider toolCallbackProvider = (ToolCallbackProvider) applicationContext.getBean(localConfig.getName());

        log.info("Tool Local MCP Initialized {}", localConfig.getName());

        return toolCallbackProvider.getToolCallbacks();
    }
}
