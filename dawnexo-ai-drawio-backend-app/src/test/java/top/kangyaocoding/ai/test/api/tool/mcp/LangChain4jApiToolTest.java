package top.kangyaocoding.ai.test.api.tool.mcp;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public class LangChain4jApiToolTest {

    interface Assistant {
        String chat(String message);
    }

    public static void main(String[] args) {

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl("https://opencode.ai/zen/v1")
                .apiKey("")
                .modelName("mimo-v2.5-free")
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .tools(new BingSearchTool(sseMcpClient()))
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();

        String chat = assistant.chat("我正在测试SpringAI调用MCP，帮我使用搜索工具搜索现在几点了？");

        log.info("输出结果：{}", chat);
    }

    public static McpSyncClient sseMcpClient() {
        String token = "ms-4aa25259-2d80-4b0d-b970-b213479ea924";

        // 魔搭社区 bing 搜索 MCP；see 协议，直接使用完整 URL 对接，并携带鉴权请求头
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
