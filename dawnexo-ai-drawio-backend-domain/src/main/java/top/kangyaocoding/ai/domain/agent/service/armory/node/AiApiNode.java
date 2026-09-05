package top.kangyaocoding.ai.domain.agent.service.armory.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import top.kangyaocoding.ai.domain.agent.model.entity.ArmoryCommandEntity;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import top.kangyaocoding.ai.domain.agent.service.armory.AbstractArmorySupport;
import top.kangyaocoding.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * @description: AiApi节点
 * @author: herbert
 * @date: 2026-08-11 21:51
 */
@Slf4j
@Service
public class AiApiNode extends AbstractArmorySupport {

    @Resource
    private ChatModelNode chatModelNode;

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - AiApiNode");
        AiAgentConfigTableVO aiAgentConfigTableVO = requestParameter.getAiAgentConfigTableVO();
        AiAgentConfigTableVO.Module.AiApi aiApi = aiAgentConfigTableVO.getModule().getAiApi();

        // 非 streaming 请求走 RestClient：显式配置连接/读超时，避免长调用在生产环境无限挂起
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectTimeoutMillis(aiApi))).build());
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMillis(aiApi)));

        // streaming（SSE）请求走 WebClient：JDK HttpClient 连接器 + 连接/读超时。
        // 注意：不能使用 (HttpClient.Builder, JdkHttpClientResourceFactory) 双参构造器——
        // 该工厂的 executor 在 afterPropertiesSet 生命周期回调里才初始化，直接 new 出来 getExecutor() 为 null 会导致 NPE
        HttpClient streamingClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMillis(aiApi)))
                .build();
        JdkClientHttpConnector streamingConnector = new JdkClientHttpConnector(streamingClient);
        streamingConnector.setReadTimeout(Duration.ofMillis(readTimeoutMillis(aiApi)));

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(aiApi.getBaseUrl())
                .apiKey(aiApi.getApiKey())
                .completionsPath(StringUtils.isNotBlank(aiApi.getCompletionsPath()) ? aiApi.getCompletionsPath() : "v1/chat/completions")
                .embeddingsPath(StringUtils.isNotBlank(aiApi.getEmbeddingsPath()) ? aiApi.getEmbeddingsPath() : "v1/embeddings")
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                .webClientBuilder(WebClient.builder().clientConnector(streamingConnector))
                .build();

        dynamicContext.setOpenAiApi(openAiApi);

        log.info("Ai Agent 装配操作 - AiApiNode 完成，OpenAiApi 已配置（connect:{}s read:{}s）", aiApi.getConnectTimeoutSeconds(), aiApi.getReadTimeoutSeconds());
        // 路由到下一个节点，如果不需要了，可以返回结果
        return router(requestParameter, dynamicContext);
    }

    private long connectTimeoutMillis(AiAgentConfigTableVO.Module.AiApi aiApi) {
        return Duration.ofSeconds(null != aiApi.getConnectTimeoutSeconds() ? aiApi.getConnectTimeoutSeconds() : 10).toMillis();
    }

    private long readTimeoutMillis(AiAgentConfigTableVO.Module.AiApi aiApi) {
        return Duration.ofSeconds(null != aiApi.getReadTimeoutSeconds() ? aiApi.getReadTimeoutSeconds() : 300).toMillis();
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        return chatModelNode;
    }
}
