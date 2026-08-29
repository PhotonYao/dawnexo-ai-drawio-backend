package top.kangyaocoding.ai.test.api.model;

import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LangChain4jApiTest {
    public static void main(String[] args) {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl("https://opencode.ai/zen/v1")
                .apiKey("")
                .modelName("mimo-v2.5-free")
                .build();

        String chat = chatModel.chat("Hello, I am testing Spring AI.");

        log.info("输出结果：{}", chat);
    }
}
