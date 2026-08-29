package top.kangyaocoding.ai.test.app;

import com.alibaba.fastjson2.JSON;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import top.kangyaocoding.ai.domain.agent.model.valobj.AiAgentRegisterVO;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @description: 类的描述信息
 * @author: herbert
 * @date: 2026-08-17 19:43
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class AiAgentAutoConfigTest {

    @Resource
    private ApplicationContext applicationContext;

    @Value("classpath:static/PixPin.png")
    private org.springframework.core.io.Resource resource;

    @Test
    public void test_agent() throws InterruptedException {
        AiAgentRegisterVO aiAgentRegisterVO = applicationContext.getBean("100001", AiAgentRegisterVO.class);

        log.info("获取到的AiAgentRegisterVO: {}", aiAgentRegisterVO);

        String appName = aiAgentRegisterVO.getAppName();
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        Session session = runner.sessionService().createSession(appName, "Herbert").blockingGet();

        Content content = Content.fromParts(Part.fromText("写一个冒泡排序"));

        Flowable<Event> eventFlowable = runner.runAsync("Herbert", session.id(), content);

        List<String> outputs = new ArrayList<>();

        eventFlowable.blockingForEach(event -> outputs.add(event.stringifyContent()));

        log.info("输出结果: {}", outputs);

        new CountDownLatch(1).await();
    }

    @Test
    public void test_single_agent() throws InterruptedException {
        AiAgentRegisterVO aiAgentRegisterVO = applicationContext.getBean("100003", AiAgentRegisterVO.class);

        log.info("获取到的AiAgentRegisterVO: {}", aiAgentRegisterVO);

        String appName = aiAgentRegisterVO.getAppName();
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        Session session = runner.sessionService().createSession(appName, "Herbert").blockingGet();

        Content content = Content.fromParts(Part.fromText("帮我整理一个学习机计划"));

        Flowable<Event> events = runner.runAsync("Herbert", session.id(), content);

        List<String> outputs = new ArrayList<>();

        events.blockingForEach(event -> outputs.add(event.stringifyContent()));

        log.info("输出结果: {}", outputs);

        new CountDownLatch(1).await();
    }

    @Test
    public void test_handlerMessage_02() {
        AiAgentRegisterVO aiAgentRegisterVO = applicationContext.getBean("100003", AiAgentRegisterVO.class);

        String appName = aiAgentRegisterVO.getAppName();
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        Session session = runner.sessionService().createSession(appName, "Herbert").blockingGet();

        Content content = Content.fromParts(Part.fromText("你具备哪些能力，或者工具可以使用？"));

        Flowable<Event> eventFlowable = runner.runAsync("Herbert", session.id(), content);

        List<String> outputs = new ArrayList<>();

        eventFlowable.blockingForEach(event -> outputs.add(event.stringifyContent()));

        log.info("测试结果:{}", JSON.toJSONString(outputs));
    }

    @Test
    public void test_handlerMessage_03() throws IOException {
        AiAgentRegisterVO aiAgentRegisterVO = applicationContext.getBean("100003", AiAgentRegisterVO.class);

        String appName = aiAgentRegisterVO.getAppName();
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        Session session = runner.sessionService().createSession(appName, "Herbert").blockingGet();

        Content content = Content.fromParts(Part.fromText("这是一张什么图片？"),
                Part.fromBytes(resource.getContentAsByteArray(), MimeTypeUtils.IMAGE_PNG_VALUE));

        Flowable<Event> eventFlowable = runner.runAsync("Herbert", session.id(), content);

        List<String> outputs = new ArrayList<>();

        eventFlowable.blockingForEach(event -> outputs.add(event.stringifyContent()));

        log.info("测试结果:{}", JSON.toJSONString(outputs));
    }

}
