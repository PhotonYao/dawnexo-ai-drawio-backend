package top.kangyaocoding.ai.test.app;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.MimeTypeUtils;
import top.kangyaocoding.ai.domain.agent.model.entity.ChatCommandEntity;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import top.kangyaocoding.ai.domain.agent.service.IChatService;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ChatServiceTest {

    @Resource
    private IChatService chatService;

    @Value("classpath:static/PixPin.png")
    private org.springframework.core.io.Resource resource;

    @Test
    public void test_handleMessage_01() throws InterruptedException {
        List<String> messages = chatService.handleMessage("100001", "xiaofuge", "你具备哪些能力");
        log.info("测试结果:{}", JSON.toJSONString(messages));
    }

    @Test
    public void test_handleMessage_02() throws InterruptedException {
        List<String> messages = chatService.handleMessage("100002", "xiaofuge", "你具备哪些能力");
        log.info("测试结果:{}", JSON.toJSONString(messages));
    }

    @Test
    public void test_handleMessage_03() throws InterruptedException {
        List<String> messages = chatService.handleMessage("100003", "xiaofuge", "把xiaofuge转换为大写");
        log.info("测试结果:{}", JSON.toJSONString(messages));
    }

    @Test
    public void test_queryAiAgentConfigList() {
        List<AiAgentConfigTableVO.Agent> agents = chatService.queryAiAgentConfigList();
        log.info("测试结果:{}", JSON.toJSONString(agents));
    }

    @Test
    public void test_handleMessage_04_withImage() throws Exception {
        String agentId = "100003";
        String userId = "xiaofuge";

        String sessionId = chatService.createSession(agentId, userId);

//        URL resource = Thread.currentThread().getContextClassLoader().getResource("PixPin.png");

        Assert.assertNotNull(resource);

//        byte[] bytes;
//        try (InputStream inputStream = resource.openStream()) {
//            bytes = inputStream.readAllBytes();
//        }

        ChatCommandEntity chatCommandEntity = ChatCommandEntity.builder()
                .agentId(agentId)
                .userId(userId)
                .sessionId(sessionId)
                .textList(List.of(new ChatCommandEntity.Content.Text("请识别这张图片，并用一句话描述。")))
//                .texts(List.of())
                .fileList(List.of())
//                .inlineDatas(List.of(new ChatCommandEntity.Content.InlineData(imageResource.getContentAsByteArray(), MimeTypeUtils.IMAGE_PNG_VALUE)))
                .inlineDataList(List.of(new ChatCommandEntity.Content.InlineData(resource.getContentAsByteArray(), MimeTypeUtils.IMAGE_PNG_VALUE)))
                .build();

        List<String> messages = chatService.handleMessage(chatCommandEntity);
        log.info("测试结果:{}", JSON.toJSONString(messages));
    }

}