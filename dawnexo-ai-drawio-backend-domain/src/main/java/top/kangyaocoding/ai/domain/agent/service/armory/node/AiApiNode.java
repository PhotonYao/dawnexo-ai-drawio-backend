package top.kangyaocoding.ai.domain.agent.service.armory.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import top.kangyaocoding.ai.domain.agent.model.entity.ArmoryCommandEntity;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import top.kangyaocoding.ai.domain.agent.service.armory.AbstractArmorySupport;
import top.kangyaocoding.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;

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

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(aiApi.getBaseUrl())
                .apiKey(aiApi.getApiKey())
                .completionsPath(StringUtils.isNotBlank(aiApi.getCompletionsPath()) ? aiApi.getCompletionsPath() : "v1/chat/completions")
                .embeddingsPath(StringUtils.isNotBlank(aiApi.getEmbeddingsPath()) ? aiApi.getEmbeddingsPath() : "v1/embeddings")
                .build();

        dynamicContext.setOpenAiApi(openAiApi);

        log.info("Ai Agent 装配操作 - AiApiNode 完成，OpenAiApi 已配置");
        // 路由到下一个节点，如果不需要了，可以返回结果
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        return chatModelNode;
    }
}
