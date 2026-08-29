package top.kangyaocoding.ai.domain.agent.service.armory.matter.mcp.client.impl;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import top.kangyaocoding.ai.domain.agent.service.armory.matter.mcp.client.ToolMcpCreateService;

import java.net.URL;
import java.time.Duration;

/**
 * @description: SSEToolMcpCreateService 是一个用于创建基于 SSE（Server-Sent Events）协议的 MCP（Model Context Protocol）客户端的服务类。它实现了 IToolMcpCreateService 接口，提供了根据 ToolMcp 配置构建 ToolCallback 数组的功能。
 * @author: herbert
 * @date: 2026-08-22 13:22
 */
@Slf4j
@Service
public class SSEToolMcpCreateService implements ToolMcpCreateService {

    @Override
    public ToolCallback[] buildToolCallback(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) throws Exception {
        log.info("构建 ToolCallback - SSEToolMcpCreateService");

        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.SSEServerParameters sseConfig = toolMcp.getSse();
        String originalUBaseUri = sseConfig.getBaseUri();
        String baseUri = originalUBaseUri;
        String sseEndpoint = sseConfig.getSseEndpoint();

        if (StringUtils.isBlank(sseEndpoint)) {
            URL url = new URL(baseUri);

            String protocol = url.getProtocol();
            String host = url.getHost();
            int port = url.getPort();

            String baseUrl = port == -1 ? protocol + "://" + host : protocol + "://" + host + ":" + port;

            int index = originalUBaseUri.indexOf(baseUrl);

            if (index != -1) {
                sseEndpoint = originalUBaseUri.substring(index + baseUrl.length());
            }
            baseUri = baseUrl;
        }

        sseEndpoint = StringUtils.isBlank(sseEndpoint) ? "/sse" : sseEndpoint;

        HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport.builder(baseUri)
                .sseEndpoint(sseEndpoint).build();
        McpSyncClient mcpSyncClient = McpClient.sync(sseClientTransport)
                .requestTimeout(Duration.ofSeconds(sseConfig.getRequestTimeout()))
                .build();

        McpSchema.InitializeResult initialize = mcpSyncClient.initialize();

        log.info("Tool SSE MCP Initialized: {}", initialize);

        return SyncMcpToolCallbackProvider.builder()
                .mcpClients(mcpSyncClient)
                .build()
                .getToolCallbacks();
    }

}
