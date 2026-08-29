package top.kangyaocoding.ai.domain.agent.service.armory.node.workflow;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.ParallelAgent;
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
 * @description: 并行执行智能体工作流节点
 * @author: herbert
 * @date: 2026-08-16 20:20
 */
@Slf4j
@Service
public class ParallelAgentNode extends AbstractArmorySupport {

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - ParallelAgentNode");

        AiAgentConfigTableVO.Module.AgentWorkflow currentAgentWorkflow = dynamicContext.getCurrentAgentWorkflow();

        List<BaseAgent> subAgents = dynamicContext.getAgentList(currentAgentWorkflow.getSubAgents());

        ParallelAgent parallelAgent = ParallelAgent.builder()
                .name(currentAgentWorkflow.getName())
                .description(currentAgentWorkflow.getDescription())
                .subAgents(subAgents)
                .build();

        dynamicContext.getAgentGroup().put(currentAgentWorkflow.getName(), parallelAgent);

        log.info("ParallelAgentNode 装配操作 - 已装配并行执行智能体工作流节点: {}", parallelAgent.name());

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        return getBean("agentWorkflowNode");
    }
}
