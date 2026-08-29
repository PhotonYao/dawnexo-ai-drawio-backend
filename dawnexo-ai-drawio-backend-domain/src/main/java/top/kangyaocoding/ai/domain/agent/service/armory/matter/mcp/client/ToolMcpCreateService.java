package top.kangyaocoding.ai.domain.agent.service.armory.matter.mcp.client;

import org.springframework.ai.tool.ToolCallback;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentConfigTableVO;

/**
 * @description: 类的描述信息
 * @author: herbert
 * @date: 2026-08-22 13:16
 */
public interface ToolMcpCreateService {

    /**
     * 根据 ToolMcp 配置构建 ToolCallback 数组
     *
     * @param toolMcp ToolMcp 配置对象
     * @return ToolCallback 数组
     */
    ToolCallback[] buildToolCallback(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) throws Exception;
}
