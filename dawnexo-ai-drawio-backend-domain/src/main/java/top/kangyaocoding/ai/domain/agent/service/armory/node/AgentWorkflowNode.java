package top.kangyaocoding.ai.domain.agent.service.armory.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import top.kangyaocoding.ai.domain.agent.model.entity.ArmoryCommandEntity;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import top.kangyaocoding.ai.domain.agent.model.valobj.enums.AgentTypeEnum;
import top.kangyaocoding.ai.domain.agent.service.armory.AbstractArmorySupport;
import top.kangyaocoding.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import top.kangyaocoding.ai.domain.agent.service.armory.node.workflow.LoopAgentNode;
import top.kangyaocoding.ai.domain.agent.service.armory.node.workflow.ParallelAgentNode;
import top.kangyaocoding.ai.domain.agent.service.armory.node.workflow.SequentialAgentNode;

import java.util.List;

/**
 * @description: 智能体工作流节点
 * @author: herbert
 * @date: 2026-08-16 19:55
 */
@Slf4j
@Service
public class AgentWorkflowNode extends AbstractArmorySupport {

    @Resource
    private LoopAgentNode loopAgentNode;

    @Resource
    private SequentialAgentNode sequentialAgentNode;

    @Resource
    private ParallelAgentNode parallelAgentNode;

    @Resource
    private RunnerNode runnerNode;

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - AgentWorkflowNode");

        AiAgentConfigTableVO aiAgentConfigTableVO = requestParameter.getAiAgentConfigTableVO();
        List<AiAgentConfigTableVO.Module.AgentWorkflow> agentWorkflows = aiAgentConfigTableVO.getModule().getAgentWorkflows();

        if (agentWorkflows == null || agentWorkflows.isEmpty() || dynamicContext.getCurrentStepIndex() >= agentWorkflows.size()) {
            log.info("AgentWorkflowNode 装配操作 - agentWorkflows 为空或当前工作流节点已处理完成，直接路由最终节点");
            dynamicContext.setCurrentAgentWorkflow(null);

            return router(requestParameter, dynamicContext);
        }

        // 获取当前工作流节点
        dynamicContext.setCurrentAgentWorkflow(agentWorkflows.get(dynamicContext.getCurrentStepIndex()));

        // 增加步长，处理下一个工作流节点
        dynamicContext.addCurrentStepIndex();

        log.info("AgentWorkflowNode 装配操作 - agentWorkflows 完成，agentWorkflows 已配置");

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {

        AiAgentConfigTableVO.Module.AgentWorkflow currentAgentWorkflow = dynamicContext.getCurrentAgentWorkflow();

        if (currentAgentWorkflow == null) {
            log.info("AgentWorkflowNode 装配操作 - currentAgentWorkflow 为空，直接路由最终节点");
            return runnerNode;
        }

        String type = currentAgentWorkflow.getType();
        AgentTypeEnum agentTypeEnum = AgentTypeEnum.formType(type);

        if (agentTypeEnum == null) {
            throw new RuntimeException("agentWorkflow type is null");
        }

        String node = agentTypeEnum.getNode();

        return switch (node) {
            case "loopAgentNode" -> loopAgentNode;
            case "parallelAgentNode" -> parallelAgentNode;
            case "sequentialAgentNode" -> sequentialAgentNode;
            default -> defaultStrategyHandler;
        };
    }
}
