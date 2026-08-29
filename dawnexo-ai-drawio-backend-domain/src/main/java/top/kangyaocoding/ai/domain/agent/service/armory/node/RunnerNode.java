package top.kangyaocoding.ai.domain.agent.service.armory.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.SequentialAgent;
import com.google.adk.plugins.BasePlugin;
import com.google.adk.runner.InMemoryRunner;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import top.kangyaocoding.ai.domain.agent.model.entity.ArmoryCommandEntity;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import top.kangyaocoding.ai.domain.agent.service.armory.AbstractArmorySupport;
import top.kangyaocoding.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import top.kangyaocoding.ai.types.enums.ResponseCode;
import top.kangyaocoding.ai.types.exception.AppException;

import java.util.ArrayList;
import java.util.List;

/**
 * @description: 智能体运行节点
 * @author: herbert
 * @date: 2026-08-17 19:04
 */
@Slf4j
@Service
public class RunnerNode extends AbstractArmorySupport {
    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - RunnerNode");

        AiAgentConfigTableVO aiAgentConfigTableVO = requestParameter.getAiAgentConfigTableVO();
        String appName = aiAgentConfigTableVO.getAppName();
        String agentId = aiAgentConfigTableVO.getAgent().getAgentId();
        String agentName = aiAgentConfigTableVO.getAgent().getAgentName();
        String agentDesc = aiAgentConfigTableVO.getAgent().getAgentDesc();

        // Runner 运行体
        InMemoryRunner runner = this.createRunner(requestParameter, dynamicContext, appName);

        // 构建注册对象
        AiAgentRegisterVO aiAgentRegisterVO = AiAgentRegisterVO.builder()
                .appName(appName)
                .agentId(agentId)
                .agentName(agentName)
                .agentDesc(agentDesc)
                .runner(runner)
                .build();

        registerBean(agentId, AiAgentRegisterVO.class, aiAgentRegisterVO);

        return aiAgentRegisterVO;
    }

    private InMemoryRunner createRunner(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext, String appName) {
        AiAgentConfigTableVO.Module.Runner runnerConfig = requestParameter.getAiAgentConfigTableVO().getModule().getRunner();

        // 获取智能体名称
        String agentName = runnerConfig.getAgentName();

        if (StringUtils.isBlank(agentName)) {
            log.error("runner.agentName is null");
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }

        // 使用该智能体进行组装 InMemory
        BaseAgent baseAgent = dynamicContext.getAgentGroup().get(agentName);

        // 拓展插件
        List<BasePlugin> plugins;
        List<String> pluginNameList = runnerConfig.getPluginNameList();
        if (pluginNameList == null || pluginNameList.isEmpty()) {
            plugins = ImmutableList.of();
        } else {
            plugins = new ArrayList<>();
            for (String pluginName : pluginNameList) {
                BasePlugin plugin = getBean(pluginName);
                plugins.add(plugin);
            }
        }

        return new InMemoryRunner(baseAgent, appName, plugins);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        return defaultStrategyHandler;
    }
}
