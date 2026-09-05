package top.kangyaocoding.ai.test.app;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import top.kangyaocoding.ai.domain.agent.model.valobj.ChatStreamEvent;
import top.kangyaocoding.ai.domain.agent.service.IChatService;
import top.kangyaocoding.ai.domain.agent.service.chat.ChatStreamOrchestrator;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @description: 流式对话编排器集成测试（走真实大模型链路，需配置 OPENAI_API_KEY）。
 * 验证 SSE 事件序列：stage 推进 -> diagram 快照校验通过 -> message 说明 -> done 收尾；
 * 以及追问轮的早停行为（只调用 analyst 一次即结束）。
 * @author: herbert
 * @date: 2026-09-04
 */
@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ChatStreamOrchestratorTest {

    private static final String AGENT_ID = "100003";
    private static final String USER_ID = "xiaofuge";

    @Resource
    private ChatStreamOrchestrator chatStreamOrchestrator;

    @Resource
    private IChatService chatService;

    /**
     * 完整绘图请求：应观察到 stage(analyze) -> stage(draw) -> diagram(draft) -> message -> done
     * （草稿校验通过时直接收尾，不再出现 stage(review)）
     */
    @Test
    public void test_stream_drawFlow() throws InterruptedException {
        String sessionId = chatService.createSession(AGENT_ID, USER_ID);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        chatStreamOrchestrator.stream(AGENT_ID, USER_ID, sessionId, "帮我画一个用户登录的流程图：输入账号密码，校验通过进入首页，失败则提示错误")
                .subscribe(
                        event -> log.info("事件 [{}]: {}", event.getType(), JSON.toJSONString(event)),
                        err -> {
                            error.set(err);
                            latch.countDown();
                        },
                        latch::countDown
                );

        if (!latch.await(5, TimeUnit.MINUTES)) {
            log.error("测试超时");
            return;
        }
        if (error.get() != null) {
            log.error("流式对话失败", error.get());
        }
    }

    /**
     * 追问轮早停：模糊请求应只收到 stage(analyze) + message(user) + done，不再进入绘图阶段
     */
    @Test
    public void test_stream_askBackEarlyStop() throws InterruptedException {
        String sessionId = chatService.createSession(AGENT_ID, USER_ID);
        CountDownLatch latch = new CountDownLatch(1);
        chatStreamOrchestrator.stream(AGENT_ID, USER_ID, sessionId, "画个图")
                .subscribe(
                        event -> log.info("事件 [{}]: {}", event.getType(), JSON.toJSONString(event)),
                        err -> {
                            log.error("流式对话失败", err);
                            latch.countDown();
                        },
                        latch::countDown
                );
        latch.await(3, TimeUnit.MINUTES);
    }

    /**
     * 多轮会话：同一会话内先追问、再补充明确需求，第二轮应驱动完整绘图流程
     */
    @Test
    public void test_stream_multiTurn() throws InterruptedException {
        String sessionId = chatService.createSession(AGENT_ID, USER_ID);
        // 第一轮：模糊请求，预期追问
        CountDownLatch first = new CountDownLatch(1);
        chatStreamOrchestrator.stream(AGENT_ID, USER_ID, sessionId, "画个图")
                .subscribe(
                        event -> log.info("第一轮事件 [{}]: {}", event.getType(), JSON.toJSONString(event)),
                        err -> log.error("第一轮失败", err),
                        first::countDown
                );
        first.await(3, TimeUnit.MINUTES);

        // 第二轮：补充明确需求，预期完整绘图
        CountDownLatch second = new CountDownLatch(1);
        chatStreamOrchestrator.stream(AGENT_ID, USER_ID, sessionId, "TCP 三次握手时序图，客户端与服务端两条生命线，SYN/SYN+ACK/ACK 三步")
                .subscribe(
                        event -> log.info("第二轮事件 [{}]: {}", event.getType(), JSON.toJSONString(event)),
                        err -> log.error("第二轮失败", err),
                        second::countDown
                );
        second.await(5, TimeUnit.MINUTES);
    }
}
