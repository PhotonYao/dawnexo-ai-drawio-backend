package top.kangyaocoding.ai.test.api.model;

import com.google.adk.agents.LlmAgent;
import com.google.adk.events.Event;
import com.google.adk.models.springai.SpringAI;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import top.kangyaocoding.ai.domain.agent.service.armory.matter.patch.MySpringAI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringAiApiTest {

    @Value("classpath:static/PixPin.png")
    private org.springframework.core.io.Resource resource;

    @Test
    public void test() {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl("https://opencode.ai/zen/")
                .apiKey("")
                .completionsPath("v1/chat/completions")
                .embeddingsPath("v1/embeddings")
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("mimo-v2.5-free")
                        .build()
                ).build();

        ChatResponse response = chatModel.call(new Prompt(UserMessage.builder()
                .text("这是一张什么图片？")
                .media(Media.builder()
                        .mimeType(MimeType.valueOf(MimeTypeUtils.IMAGE_PNG_VALUE))
                        .data(resource)
                        .build()
                ).build(),
                OpenAiChatOptions.builder()
                        .model("mimo-v2.5-free")
                        .build()));

        log.info("测试结果:{}", response);
    }

    @Test
    public void test2() throws IOException {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl("https://opencode.ai/zen/")
                .apiKey("")
                .completionsPath("v1/chat/completions")
                .embeddingsPath("v1/embeddings")
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("mimo-v2.5-free")
                        .build()
                ).build();

        LlmAgent llmAgent = LlmAgent.builder()
                .name("test")
                .model(new MySpringAI(chatModel))
                .description("这是一个测试的AI代理，能够处理文本和图像输入。")
                .instruction("你是一个AI助手，能够理解和处理文本和图像输入。请根据用户的输入提供有用的回答。")
                .build();

        InMemoryRunner runner = new InMemoryRunner(llmAgent);

        Session session = runner.sessionService()
                .createSession("test", "herbert")
                .blockingGet();

        List<Part> parts = new ArrayList<>();
        parts.add(Part.fromText("这是一张什么图片？"));
        parts.add(Part.fromBytes(resource.getContentAsByteArray(), MimeTypeUtils.IMAGE_PNG_VALUE));

        Content userMsg = Content.builder().role("user").parts(parts).build();

        Flowable<Event> eventFlowable = runner.runAsync("herbert", session.id(), userMsg);

        ArrayList<String> outputs = new ArrayList<>();

        eventFlowable.blockingForEach(event -> outputs.add(event.stringifyContent()));

        log.info("测试结果:{}", outputs);
    }
}
