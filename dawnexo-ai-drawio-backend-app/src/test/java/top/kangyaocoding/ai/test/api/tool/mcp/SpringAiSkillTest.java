package top.kangyaocoding.ai.test.api.tool.mcp;

import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;

/**
 * @description: 类的描述信息
 * @author: herbert
 * @date: 2026-08-27 19:34
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringAiSkillTest {
    public static void main(String[] args) {

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl("https://apihub.agnes-ai.com/")
                .apiKey("")
                .completionsPath("v1/chat/completions")
                .embeddingsPath("v1/embeddings")
                .build();

        ToolCallback toolCallback = SkillsTool.builder().addSkillsResource(new ClassPathResource("agent/skills")).build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("agnes-2.0-flash")
                        .toolCallbacks(new ArrayList<>() {{
                            add(toolCallback);
                        }})
                        .build())
                .build();

        String call = chatModel.call("你具备什么技能");

        log.info("测试结果:{}", call);
    }
}
