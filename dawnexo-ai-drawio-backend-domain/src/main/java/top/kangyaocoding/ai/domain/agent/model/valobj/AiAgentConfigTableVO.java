package top.kangyaocoding.ai.domain.agent.model.valobj;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * AiAgentConfigTableVO 是一个面向智能体应用的配置值对象，用于承载应用名、智能体元信息，以及与大模型和工具链相关的模块化配置。
 * <p>
 * 顶层包含 appName 、 agent 与 module 三部分，分别代表应用标识、智能体基础信息和功能模块配置。顶层 Agent 表示主智能体的“元信息”； Module.Agent 则是编排中使用的“子智能体”定义，用于明确职责区分。
 * <p>
 * ChatModel.ToolMcp 同时支持 SSE 与 Stdio 两种服务方式，覆盖远程与本地工具的集成场景。后续可以支持本地自己实现的 MCP 业务服务对接。
 *
 */
@Data
public class AiAgentConfigTableVO {
    /**
     * 应用名称
     */
    private String appName;

    /**
     * 智能体配置
     */
    private Agent agent;

    /**
     * 智能体模块
     */
    private Module module;

    @Data
    public static class Agent {
        /**
         * 智能体ID
         */
        private String agentId;

        /**
         * 智能体名称
         */
        private String agentName;

        /**
         * 智能体描述
         */
        private String agentDesc;

    }

    @Data
    public static class Module {
        private AiApi aiApi;

        private ChatModel chatModel;

        private List<Agent> agents;

        private List<AgentWorkflow> agentWorkflows;

        private Runner runner;

        @Data
        public static class AiApi {
            private String baseUrl;
            private String apiKey;
            private String completionsPath = "/v1/chat/completions";
            private String embeddingsPath = "/v1/embeddings";
        }

        @Data
        public static class ChatModel {
            private String model;
            private List<ToolMcp> toolMcpList;
            private List<ToolSkills> toolSkillsList;

            @Data
            public static class ToolMcp {
                private SSEServerParameters sse;
                private StdioServerParameters stdio;
                private LocalServerParameters local;

                @Data
                public static class SSEServerParameters {
                    private String name;
                    private String baseUri;
                    private String sseEndpoint;
                    private Integer requestTimeout = 3000;
                }

                @Data
                public static class StdioServerParameters {
                    private String name;
                    private Integer requestTimeout = 3000;
                    private ServerParameters serverParameters;

                    @Data
                    public static class ServerParameters {
                        private String command;
                        private List<String> args;
                        private Map<String, String> env;
                    }
                }

                @Data
                public static class LocalServerParameters {
                    private String name;
                }
            }

            @Data
            public static class ToolSkills {
                /**
                 * 类型；directory（用户配置的，映射进来的）、resource（放到工程下的）
                 */
                private String type = "directory";
                /**
                 * 路径；
                 */
                private String path;
            }
        }

        @Data
        public static class Agent {
            private String name;
            private String instruction;
            private String description;
            private String outputKey;
        }

        @Data
        public static class AgentWorkflow {
            /**
             * 类型；loop、parallel、sequential
             */
            private String type;
            private String name;
            private List<String> subAgents;
            private String description;
            private Integer maxIterations = 3;
        }

        @Data
        public static class Runner {
            private String agentName;
            private List<String> pluginNameList;
        }
    }
}
