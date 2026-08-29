package top.kangyaocoding.ai.domain.agent.service.armory.node.workflow;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LoopAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.kangyaocoding.ai.domain.agent.model.entity.ArmoryCommandEntity;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import top.kangyaocoding.ai.domain.agent.model.valobj.enums.AgentTypeEnum;
import top.kangyaocoding.ai.domain.agent.service.armory.AbstractArmorySupport;
import top.kangyaocoding.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;

import java.util.List;

/**
 * @description: 循环执行节点
 * @author: herbert
 * @date: 2026-08-16 20:15
 */
@Slf4j
@Service
public class LoopAgentNode extends AbstractArmorySupport {

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - LoopAgentNode");

        AiAgentConfigTableVO.Module.AgentWorkflow currentAgentWorkflow = dynamicContext.getCurrentAgentWorkflow();

        List<BaseAgent> subAgents = dynamicContext.getAgentList(currentAgentWorkflow.getSubAgents());

        LoopAgent loopAgentNode = LoopAgent.builder()
                .name(currentAgentWorkflow.getName())
                .description(currentAgentWorkflow.getDescription())
                .subAgents(subAgents)
                .maxIterations(currentAgentWorkflow.getMaxIterations())
                .build();

        dynamicContext.getAgentGroup().put(currentAgentWorkflow.getName(), loopAgentNode);

        log.info("LoopAgentNode 装配操作 - 已装配循环执行节点: {}", loopAgentNode.name());

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        return getBean("agentWorkflowNode");
    }
}
