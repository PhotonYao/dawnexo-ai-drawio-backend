package top.kangyaocoding.ai.test.domain.agent.service.chat;

import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import top.kangyaocoding.ai.domain.agent.service.chat.ChatStreamOrchestrator;
import top.kangyaocoding.ai.domain.agent.service.chat.support.DrawioXmlSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @description: 流式契约解析回归测试。
 * 背景：模型可能无视 prompt 的"禁止代码块"约束，把三字段 JSON 包进 Markdown 围栏输出
 * （生产日志中 agent_drawer 曾透传 '```json {...} ```'，导致契约解析失败、流程误入质检并最终报错）。
 * @author: herbert
 * @date: 2026-09-05
 */
public class ChatContractParseTest {

    private static final String ASK_BACK_JSON = "{\"type\":\"user\",\"explanation\":\"请补充图表的具体内容\"}";

    @Test
    public void plain_json_is_parsed() {
        JSONObject json = ChatStreamOrchestrator.tryParseContract(ASK_BACK_JSON);
        assertTrue(json != null && "user".equals(json.getString("type")));
    }

    @Test
    public void fenced_json_is_parsed() {
        // 生产实际出现过的形态：```json ... ``` 围栏包裹
        String fenced = "```json\n" + ASK_BACK_JSON + "\n```";
        JSONObject json = ChatStreamOrchestrator.tryParseContract(fenced);
        assertTrue(json != null && "user".equals(json.getString("type")));
    }

    @Test
    public void xml_fence_wrapped_json_is_parsed() {
        // ``` 围栏（无语言标记）同样要能剥离
        String fenced = "```\n" + ASK_BACK_JSON + "\n```";
        JSONObject json = ChatStreamOrchestrator.tryParseContract(fenced);
        assertTrue(json != null && "user".equals(json.getString("type")));
    }

    @Test
    public void text_wrapped_json_is_parsed() {
        String wrapped = "好的，以下是结果：\n" + ASK_BACK_JSON + "\n希望对您有帮助";
        JSONObject json = ChatStreamOrchestrator.tryParseContract(wrapped);
        assertTrue(json != null && "user".equals(json.getString("type")));
    }

    @Test
    public void non_json_text_returns_null() {
        assertNull(ChatStreamOrchestrator.tryParseContract("这是一段普通的分析文本，没有 JSON 结构。"));
        assertNull(ChatStreamOrchestrator.tryParseContract(null));
        assertNull(ChatStreamOrchestrator.tryParseContract(""));
    }

    @Test
    public void strip_markdown_fence_keeps_inner_content() {
        String fenced = "```xml\n<mxGraphModel></mxGraphModel>\n```";
        assertEquals("<mxGraphModel></mxGraphModel>", DrawioXmlSupport.stripMarkdownFence(fenced));
        assertEquals("plain", DrawioXmlSupport.stripMarkdownFence("plain"));
        assertNull(DrawioXmlSupport.stripMarkdownFence(null));
    }
}
