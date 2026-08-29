package top.kangyaocoding.ai.domain.agent.service.armory.matter.mcp.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import top.kangyaocoding.ai.domain.agent.service.armory.matter.mcp.client.ToolMcpCreateService;

import java.time.Duration;

/**
 * @description: StdioToolMcpCreateService 是一个用于创建基于标准输入输出（Stdio）协议的 MCP（Model Context Protocol）客户端的服务类。它实现了 IToolMcpCreateService 接口，提供了根据 ToolMcp 配置构建 ToolCallback 数组的功能。
 * @author: herbert
 * @date: 2026-08-22 13:38
 */
@Slf4j
@Service
public class StdioToolMcpCreateService implements ToolMcpCreateService {

    @Override
    public ToolCallback[] buildToolCallback(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) throws Exception {
        log.info("构建 ToolCallback - StdioToolMcpCreateService");

        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.StdioServerParameters stdioConfig = toolMcp.getStdio();
        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.StdioServerParameters.ServerParameters serverParameters = stdioConfig.getServerParameters();

        ServerParameters stdioParameters = ServerParameters.builder(serverParameters.getCommand())
                .args(serverParameters.getArgs())
                .env(serverParameters.getEnv())
                .build();

        McpSyncClient mcpSyncClient = McpClient.sync(new StdioClientTransport(stdioParameters, new JacksonMcpJsonMapper(new ObjectMapper())))
                .requestTimeout(Duration.ofSeconds(stdioConfig.getRequestTimeout()))
                .build();

        McpSchema.InitializeResult initialize = mcpSyncClient.initialize();

        log.info("Tool SSE MCP Initialized: {}", initialize);

        return SyncMcpToolCallbackProvider.builder().mcpClients(mcpSyncClient).build().getToolCallbacks();
    }
}
