# Spring AI + MCP Tool Calling 方案总结

## 1. 目标

实现：

> Spring AI ChatModel 调用大模型时，自动发现并调用外部 MCP Server 提供的工具能力。

本次验证场景：

* 大模型：`deepseek-v4-flash-free`
* AI 框架：Spring AI
* MCP Server：魔搭社区 Bing Search MCP
* MCP 协议：SSE
* 工具：

  * `bing_search`
  * `crawl_webpage`

最终效果：

用户输入：

```
帮我搜索网络今天是几号？
```

模型自动：

1. 判断需要联网查询
2. 调用 MCP 工具 `bing_search`
3. 获取搜索结果
4. 根据结果生成自然语言回答

---

# 2. 整体架构

```
                  用户请求
                     |
                     v
              Spring AI ChatModel
                     |
                     |
          +----------+-----------+
          |                      |
          v                      v
      OpenAI API             Tool Callback
          |                      |
          |                      |
          |                MCP Tool Adapter
          |                      |
          |                      v
          |              MCP Client (SSE)
          |                      |
          |                      |
          +--------------> MCP Server
                                 |
                                 |
                         bing_search工具
                         crawl_webpage工具

```

---

# 3. 核心流程

## 第一步：创建 OpenAI API 客户端

Spring AI 本身不限制 OpenAI 服务。

通过：

```java
OpenAiApi.builder()
```

配置兼容 OpenAI API 的服务。

```java
OpenAiApi openAiApi = OpenAiApi.builder()
        .baseUrl("https://opencode.ai/zen/")
        .apiKey("xxx")
        .completionsPath("v1/chat/completions")
        .embeddingsPath("v1/embeddings")
        .build();
```

支持：

* OpenAI 官方
* DeepSeek
* OpenAI Compatible API
* 自建模型网关

---

# 4. 创建 MCP Client

采用：

```
MCP SSE Transport
```

连接：

```
https://mcp.api-inference.modelscope.net/
```

代码：

```java
HttpClientSseClientTransport sseClientTransport =
        HttpClientSseClientTransport.builder(
            "https://mcp.api-inference.modelscope.net/")
        .sseEndpoint("5351e78128dd4a/sse")
        .build();
```

协议流程：

```
Client
 |
 | initialize
 |
 v
MCP Server

返回:

protocolVersion
capabilities
serverInfo

```

日志：

```
MCP initialized:

protocolVersion=2024-11-05

serverInfo:
bing-cn-search
version=1.9.4

```

说明：

MCP 握手成功。

---

# 5. 获取 MCP Tool 列表

通过：

```java
mcpClient.listTools()
```

获取：

```
bing_search

crawl_webpage

```

返回：

```text
Tool[name=bing_search]

Tool[name=crawl_webpage]

```

这里完成：

> MCP 服务能力发现

类似：

```
服务注册中心
       |
       |
发现有哪些API
```

---

# 6. MCP Tool 转换为 Spring AI ToolCallback

这是本次最关键的点。

## 错误方案

之前：

```java
SyncMcpToolCallback.builder()
        .mcpClient(mcpClient)
        .build();
```

错误原因：

`SyncMcpToolCallback` 代表：

> 一个具体 MCP Tool

不是：

> MCP Client工具集合

所以必须绑定：

```java
.tool(tool)
```

---

## 正确方案

遍历 MCP Tool：

```java
List<ToolCallback> callbacks =
        toolsList.tools()
        .stream()
        .map(tool ->
            SyncMcpToolCallback.builder()
                .mcpClient(mcpClient)
                .tool(tool)
                .build()
        )
        .map(callback -> (ToolCallback)callback)
        .collect(Collectors.toList());

```

转换关系：

```
MCP Tool

    |
    |
    v

SyncMcpToolCallback

    |
    |
    v

Spring AI ToolCallback

```

---

# 7. 注册 Tool 到 ChatModel

最终：

```java
ChatModel chatModel =
        OpenAiChatModel.builder()
        .openAiApi(openAiApi)
        .defaultOptions(
            OpenAiChatOptions.builder()
                .model("deepseek-v4-flash-free")
                .toolCallbacks(mcpToolCallbacks)
                .build()
        )
        .build();

```

此时：

模型拥有：

```
普通语言能力

+

bing搜索能力

+

网页抓取能力

```

---

# 8. 一次完整调用链

用户：

```
帮我搜索网络今天是几号？
```

## Step 1

Spring AI发送：

```
messages:
[
 {
  role:user,
  content:
  "帮我搜索网络今天是几号？"
 }
]


tools:

[
 bing_search,
 crawl_webpage
]

```

---

## Step 2

模型判断：

需要调用：

```
bing_search
```

生成：

```json
{
"name":"bing_search",
"arguments":{
 "query":"今天日期"
}
}

```

---

## Step 3

Spring AI执行 MCP Tool

调用：

```
MCP Client

        |
        |
        v

MCP Server

        |
        |
        v

bing_search

```

---

## Step 4

返回搜索结果

例如：

```
2026年8月7日 星期五

```

---

## Step 5

再次提交给模型

模型生成最终回答：

```
今天是2026年8月7日星期五
```

---

# 9. 当前方案验证结果

运行结果：

```
Process finished with exit code 0
```

说明：

| 模块             | 状态 |
| -------------- | -- |
| Spring AI启动    | ✅  |
| OpenAI兼容API    | ✅  |
| MCP SSE连接      | ✅  |
| MCP初始化         | ✅  |
| Tool发现         | ✅  |
| Tool注册         | ✅  |
| 模型Tool Calling | ✅  |
| MCP工具执行        | ✅  |
| 最终回答生成         | ✅  |

---

# 10. 当前代码可以优化的地方

## 10.1 Authorization 拼写

当前：

```java
builder.header(
    "Authorization",
    "Bear " + token
);
```

应该：

```java
builder.header(
    "Authorization",
    "Bearer " + token
);
```

标准：

```
Authorization: Bearer xxx
```

虽然当前 MCP Server 容忍了，但建议修正。

---

## 10.2 ToolCallback 可以抽成 Provider

生产环境建议：

```
McpToolManager

        |
        |
        +---- connect MCP Server

        |
        |
        +---- discover tools

        |
        |
        +---- register callbacks

```

例如：

```java
@Component
public class McpToolProvider {


    public List<ToolCallback> loadTools(){
        ...
    }

}

```

业务 ChatService：

```java
chatModel.call(prompt);
```

不关心 MCP 细节。

---

# 11. 后续扩展方向

当前：

```
一个 MCP Server
        |
        |
   bing搜索
```

可以扩展：

```
                 Spring AI
                    |
        +-----------+------------+
        |                        |
        |                        |
    MCP Server A             MCP Server B

    搜索服务                 数据库服务

    bing                    MySQL

                              |
                              |
                         企业内部知识库


```

最终形成：

```
AI Agent

+

MCP Tool Ecosystem

```

---

# 12. 最终方案结论

本次实现验证了：

> Spring AI 可以作为 MCP Client，通过 MCP SSE 协议动态发现外部工具，并将 MCP Tool 转换成 Spring AI ToolCallback 注册给 ChatModel，实现大模型自主调用外部能力。

核心代码链路：

```
McpSyncClient
        |
        |
listTools()
        |
        |
SyncMcpToolCallback
        |
        |
ToolCallback
        |
        |
OpenAiChatModel
        |
        |
LLM Tool Calling
        |
        |
MCP Server Tool Execute

```

这个方案已经具备演进成 **Spring AI Agent + MCP 工具生态平台** 的基础。
