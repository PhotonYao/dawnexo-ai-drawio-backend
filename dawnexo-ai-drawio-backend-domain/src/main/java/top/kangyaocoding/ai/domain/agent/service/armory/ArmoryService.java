package top.kangyaocoding.ai.domain.agent.service.armory;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import top.kangyaocoding.ai.domain.agent.model.entity.ArmoryCommandEntity;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import top.kangyaocoding.ai.domain.agent.service.IArmoryService;
import top.kangyaocoding.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;

import java.util.List;

/**
 * @description: 装配服务实现类
 * @author: herbert
 * @date: 2026-08-11 21:41
 */
@Service
public class ArmoryService implements IArmoryService {

    @Resource
    private DefaultArmoryFactory defaultArmoryFactory;

    /**
     * 接受并处理 AI Agent 配置表列表
     *
     * 该方法遍历配置表列表，为每个配置创建相应的装配命令，
     * 并通过工厂生成的策略处理器执行装配逻辑。
     *
     * @param tables AI Agent 配置表集合，包含需要装配的 Agent 信息
     * @throws Exception 当装配过程中发生错误时抛出异常，
     *                   可能的原因包括：
     *                   - 工厂创建处理器失败
     *                   - 策略处理器执行失败
     *                   - Agent 配置信息不合法
     *
     * @see ArmoryCommandEntity
     * @see DefaultArmoryFactory.DynamicContext
     * @see AiAgentRegisterVO
     */
    @Override
    public void acceptArmoryAgents(List<AiAgentConfigTableVO> tables) throws Exception {

        for (AiAgentConfigTableVO table : tables) {
            StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> handler = defaultArmoryFactory.armoryStrategyHandler();
            handler.apply(
                    ArmoryCommandEntity.builder()
                            .aiAgentConfigTableVO(table)
                            .build(),
                    new DefaultArmoryFactory.DynamicContext());
        }
    }
}
