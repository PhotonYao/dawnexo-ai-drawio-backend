package top.kangyaocoding.ai.test.api.tool.mcp;

import dev.langchain4j.agent.tool.Tool;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;

public class BingSearchTool {


    private final McpSyncClient client;


    public BingSearchTool(McpSyncClient client){
        this.client = client;
    }


    @Tool("搜索互联网")
    public String search(String query){

        McpSchema.CallToolResult result =
                client.callTool(
                    new McpSchema.CallToolRequest(
                        "bing_search",
                        Map.of(
                            "query",query
                        )
                    )
                );

        return result.toString();
    }
}
