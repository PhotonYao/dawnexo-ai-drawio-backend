# Dawnexo AI Draw 后端服务 · AI Draw.io 绘图智能体

基于 **xfg-frame-archetype - DDD 脚手架 - @小傅哥 v2.2** 搭建的 AI 绘图智能体服务：用户以自然语言描述图表需求，智能体按「需求分析 → 绘图 → 检查优化」的串行工作流生成 draw.io XML，返回给前端渲染到画布。

> 原脚手架相关资源（@小傅哥）：
>
> - docker 使用文档：[https://bugstack.cn/md/road-map/docker.html](https://bugstack.cn/md/road-map/docker.html)
> - DDD 教程；
>   - [DDD 概念理论](https://bugstack.cn/md/road-map/ddd-guide-01.html)
>   - [DDD 建模方法](https://bugstack.cn/md/road-map/ddd-guide-02.html)
>   - [DDD 工程模型](https://bugstack.cn/md/road-map/ddd-guide-03.html)
>   - [DDD 架构设计](https://bugstack.cn/md/road-map/ddd.html)
>   - [DDD 建模案例](https://bugstack.cn/md/road-map/ddd-model.html)

## 技术栈

- Java 17 / Spring Boot 3.4.3
- Google ADK（多智能体编排、会话管理）+ LangChain4j（模型接入、MCP 工具）
- DDD 分层架构 + Maven 多模块

## 模块结构

| 模块 | 说明 |
| --- | --- |
| dawnexo-ai-drawio-backend-app | 应用启动与装配（智能体注册、配置加载，`resources/agent/*.yml`） |
| dawnexo-ai-drawio-backend-domain | 领域层（智能体编排、串行/循环/并行工作流、会话服务、MCP 工具装配） |
| dawnexo-ai-drawio-backend-trigger | 触发器层（HTTP 接口：配置查询、会话、对话、流式对话） |
| dawnexo-ai-drawio-backend-api | 对外接口定义与 DTO（`ChatRequestDTO` / `ChatResponseDTO` 等） |
| dawnexo-ai-drawio-backend-infrastructure | 基础设施层 |
| dawnexo-ai-drawio-backend-types | 通用类型（统一响应 `Response`、响应码、异常） |

## 智能体配置

绘图智能体配置见 `dawnexo-ai-drawio-backend-app/src/main/resources/agent/agent-draw-io.yml`：

- 智能体 ID `100003`（AI交互式绘图智能体）
- 串行工作流 `sequential_draw_process`：`agent_analyst`（需求分析与检索）→ `agent_drawer`（生成 draw.io XML）→ `agent_reviewer`（检查优化并格式化输出）
- 最终输出 JSON：`{"type": "user", "content": "追问文本"}`（需要用户补充信息）或 `{"type": "drawio", "content": "<mxGraphModel>...</mxGraphModel>"}`（图表 XML）

模型 API Key、MCP 工具等在同一配置文件的 `module.ai-api` / `chat-model` 下配置。

## 接口一览

接口文档与请求/返回示例见 [docs/dev-ops/api/api.md](docs/dev-ops/api/api.md)，统一响应结构 `{code, info, data}`（成功码 `0000`）：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/query_ai_agent_config_list` | 查询智能体配置列表 |
| GET / POST | `/api/v1/create_session` | 创建会话，每次调用返回全新 `sessionId` |
| POST | `/api/v1/chat` | 智能体对话，返回 `{type, content, sessionId}` |
| POST | `/api/v1/chat_stream` | 流式对话（SSE） |

### 会话防交叉污染设计

- `createSession` 每次调用都创建**全新会话**，并登记 `sessionId → (agentId, userId)` 绑定关系；
- 对话前通过 `ensureSession` 校验会话归属：会话 ID 为空、绑定丢失（服务重启）或与请求的智能体/用户不一致时，自动创建隔离的新会话，避免不同对话间上下文交叉污染；
- chat 响应回传本次实际使用的 `sessionId`，前端以此为准。

## 本地启动

1. 配置模型密钥：编辑 `dawnexo-ai-drawio-backend-app/src/main/resources/agent/agent-draw-io.yml` 中的 `module.ai-api.api-key`；
2. 启动：运行 `dawnexo-ai-drawio-backend-app` 模块的 `Application#main`（默认 dev 配置，端口 `8090`，生产 `8091`），或执行 `mvn -DskipTests package` 后运行 jar；
3. 前端配套项目 [dawnexo-ai-drawio-front](../dawnexo-ai-drawio-front) 的后端地址在 `app/config/api-config.ts` 中配置（默认 `http://127.0.0.1:8090`）。
