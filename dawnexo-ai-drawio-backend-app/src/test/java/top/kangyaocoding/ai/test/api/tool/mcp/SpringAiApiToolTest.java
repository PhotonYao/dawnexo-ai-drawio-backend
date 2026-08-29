package top.kangyaocoding.ai.test.api.tool.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class SpringAiApiToolTest {

    public static void main(String[] args) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl("https://opencode.ai/zen/")
                .apiKey("")
                .completionsPath("v1/chat/completions")
                .embeddingsPath("v1/embeddings")
                .build();

        // 1. 初始化 MCP 同步客户端
        McpSyncClient mcpClient = sseMcpClient();

        // 先获取可用工具列表
        var toolsList = mcpClient.listTools();

        log.info("Available tools from MCP server: {}", toolsList);

        if (toolsList.tools() == null || toolsList.tools().isEmpty()) {
            log.warn("No tools found from MCP server!");
            return;
        }

        // 为每个 MCP Tool 创建 Callback
        List<ToolCallback> mcpToolCallbacks = toolsList.tools()
                .stream()
                .map(tool -> SyncMcpToolCallback.builder()
                        .mcpClient(mcpClient)
                        .tool(tool)
                        .build())
                .map(callback -> (ToolCallback) callback)
                .collect(Collectors.toList());

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .model("deepseek-v4-flash-free")
                                .toolCallbacks(mcpToolCallbacks)
                                .build()
                )
                .build();


        String call = chatModel.call(
                "帮我搜索网络今天是几号？"
        );

        log.info("call: {}", call);
    }

    public static McpSyncClient sseMcpClient() {
        String token = "ms-4aa25259-2d80-4b0d-b970-b213479ea924";
        // 魔搭社区 bing 搜索 MCP；see 协议，直接使用完整 URL 对接
        HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport.builder(
                        "https://mcp.api-inference.modelscope.net/")
                .sseEndpoint("5351e78128dd4a/sse")
//                .httpRequestCustomizer((builder, method, endpoint, body, context) -> {
//                    builder.header("Authorization", "Bearer " + token);
//                })
                .build();
        McpSyncClient mcpSyncClient = McpClient.sync(sseClientTransport).requestTimeout(Duration.ofSeconds(120)).build();
        McpSchema.InitializeResult initialize = mcpSyncClient.initialize();
        log.info("MCP initialized: {}", initialize);
        return mcpSyncClient;
    }

}
