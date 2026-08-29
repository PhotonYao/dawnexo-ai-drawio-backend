package top.kangyaocoding.ai.domain.agent.service.armory.matter.mcp.client.factory;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import top.kangyaocoding.ai.domain.agent.service.armory.matter.mcp.client.ToolMcpCreateService;
import top.kangyaocoding.ai.domain.agent.service.armory.matter.mcp.client.impl.LocalToolMcpCreateService;
import top.kangyaocoding.ai.domain.agent.service.armory.matter.mcp.client.impl.SSEToolMcpCreateService;
import top.kangyaocoding.ai.domain.agent.service.armory.matter.mcp.client.impl.StdioToolMcpCreateService;
import top.kangyaocoding.ai.types.enums.ResponseCode;
import top.kangyaocoding.ai.types.exception.AppException;

/**
 * @description: DefaultMcpClientFactory 是一个用于创建和管理 MCP（Model Context Protocol）客户端的工厂类。它提供了对不同类型的 MCP 客户端创建服务的访问，包括本地、SSE（Server-Sent Events）和标准输入输出（Stdio）协议的客户端。通过依赖注入，这个工厂类可以方便地获取和使用这些服务，从而简化 MCP 客户端的创建和管理过程。
 * @author: herbert
 * @date: 2026-08-22 13:56
 */
@Slf4j
@Service
public class DefaultMcpClientFactory {
    @Resource
    private LocalToolMcpCreateService localToolMcpCreateService;

    @Resource
    private SSEToolMcpCreateService sseToolMcpCreateService;

    @Resource
    private StdioToolMcpCreateService stdioToolMcpCreateService;

    public ToolMcpCreateService getToolMcpCreateService(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) {
        log.info("获取 ToolMcpCreateService - DefaultMcpClientFactory");

        if (toolMcp.getLocal() != null) {
            return localToolMcpCreateService;
        }
        if (toolMcp.getSse() != null) {
            return sseToolMcpCreateService;
        }
        if (toolMcp.getStdio() != null) {
            return stdioToolMcpCreateService;
        }
        throw new AppException(ResponseCode.NOT_FOUND_METHOD.getCode(), ResponseCode.NOT_FOUND_METHOD.getInfo());
    }
}
