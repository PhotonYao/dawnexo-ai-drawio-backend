package top.kangyaocoding.ai.domain.agent.service.armory.factory;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.SequentialAgent;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import top.kangyaocoding.ai.domain.agent.model.entity.ArmoryCommandEntity;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import top.kangyaocoding.ai.domain.agent.service.armory.node.RootNode;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description: 默认的装配工厂
 * @author: herbert
 * @date: 2026-08-11 21:49
 */
@Service
public class DefaultArmoryFactory {

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private RootNode rootNode;

    public StrategyHandler<ArmoryCommandEntity, DynamicContext, AiAgentRegisterVO> armoryStrategyHandler() {
        return rootNode;
    }

    public AiAgentRegisterVO getAiAgentRegisterVO(String agentId) {
        return applicationContext.getBean(agentId, AiAgentRegisterVO.class);
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {

        /**
         * LLM Api
         */
        private OpenAiApi openAiApi;

        /**
         * LLM ChatModel
         */
        private ChatModel chatModel;

        /**
         * 智能体配置组
         */
        private Map<String, BaseAgent> agentGroup = new HashMap<>();

        /**
         * 步长
         */
        private AtomicInteger currentStepIndex = new AtomicInteger(0);

        /**
         * 当前智能体工作流节点
         */
        private AiAgentConfigTableVO.Module.AgentWorkflow currentAgentWorkflow;

        private Map<String, Object> dataObjects = new HashMap<>();

        private <T> void setValue(String key, T value) {
            dataObjects.put(key, value);
        }

        public <T> T getValue(String key) {
            return (T) dataObjects.get(key);
        }

        public List<BaseAgent> getAgentList(List<String> agentNames) {
            if (null == agentNames || agentNames.isEmpty() || null == agentGroup) {
                return Collections.emptyList();
            }

            List<BaseAgent> agents = new ArrayList<>();
            for (String agentName : agentNames) {
                BaseAgent baseAgent = agentGroup.get(agentName);
                if (baseAgent != null) {
                    agents.add(baseAgent);
                }
            }
            return agents;
        }

        /**
         * 增加步长的方法
         */
        public void addCurrentStepIndex() {
            currentStepIndex.incrementAndGet();
        }

        /**
         * 获取步长的方法
         */
        public int getCurrentStepIndex() {
            return currentStepIndex.get();
        }
    }
}
