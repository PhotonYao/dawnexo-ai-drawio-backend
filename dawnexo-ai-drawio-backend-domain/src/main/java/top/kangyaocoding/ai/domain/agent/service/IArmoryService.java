package top.kangyaocoding.ai.domain.agent.service;

import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentConfigTableVO;

import java.util.List;

/**
 * @description: 装配服务接口
 * @author: herbert
 * @date: 2026-08-11 21:25
 */
public interface IArmoryService {
    void acceptArmoryAgents(List<AiAgentConfigTableVO> tables) throws Exception;
}
