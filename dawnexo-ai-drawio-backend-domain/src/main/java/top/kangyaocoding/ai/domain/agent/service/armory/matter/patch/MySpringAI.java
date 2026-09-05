package top.kangyaocoding.ai.domain.agent.service.armory.matter.patch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.models.BaseLlm;
import com.google.adk.models.BaseLlmConnection;
import com.google.adk.models.LlmRequest;
import com.google.adk.models.LlmResponse;
import com.google.adk.models.springai.error.SpringAIErrorMapper;
import com.google.adk.models.springai.observability.SpringAIObservabilityHandler;
import com.google.adk.models.springai.properties.SpringAIProperties;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @description: MySpringAI 是一个自定义的 Spring AI 模型类，继承自 BaseLlm。它用于与 Spring AI 的 ChatModel 和 StreamingChatModel 进行交互，提供了生成内容和流式内容的功能。该类还集成了可观察性处理器，用于记录请求和响应的日志信息，并处理错误映射。通过 MySpringAI，可以方便地在应用程序中使用 Spring AI 模型进行自然语言处理任务。并修复了原有 MessageConverter 无法使用图片消息的能力
 * @author: herbert
 * @date: 2026-08-22 20:54
 */
public class MySpringAI extends BaseLlm {

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final ObjectMapper objectMapper;
    private final MyMessageConverter messageConverter;
    private final SpringAIObservabilityHandler observabilityHandler;

    public MySpringAI(ChatModel chatModel) {
        super(extractModelName(chatModel));
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel cannot be null");
        this.streamingChatModel =
                (chatModel instanceof StreamingChatModel) ? (StreamingChatModel) chatModel : null;
        this.objectMapper = new ObjectMapper();
        this.messageConverter = new MyMessageConverter(objectMapper);
        this.observabilityHandler =
                new SpringAIObservabilityHandler(createDefaultObservabilityConfig());
    }

    public MySpringAI(ChatModel chatModel, String modelName) {
        super(Objects.requireNonNull(modelName, "model name cannot be null"));
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel cannot be null");
        this.streamingChatModel =
                (chatModel instanceof StreamingChatModel) ? (StreamingChatModel) chatModel : null;
        this.objectMapper = new ObjectMapper();
        this.messageConverter = new MyMessageConverter(objectMapper);
        this.observabilityHandler =
                new SpringAIObservabilityHandler(createDefaultObservabilityConfig());
    }

    public MySpringAI(StreamingChatModel streamingChatModel) {
        super(extractModelName(streamingChatModel));
        this.chatModel =
                (streamingChatModel instanceof ChatModel) ? (ChatModel) streamingChatModel : null;
        this.streamingChatModel =
                Objects.requireNonNull(streamingChatModel, "streamingChatModel cannot be null");
        this.objectMapper = new ObjectMapper();
        this.messageConverter = new MyMessageConverter(objectMapper);
        this.observabilityHandler =
                new SpringAIObservabilityHandler(createDefaultObservabilityConfig());
    }

    public MySpringAI(StreamingChatModel streamingChatModel, String modelName) {
        super(Objects.requireNonNull(modelName, "model name cannot be null"));
        this.chatModel =
                (streamingChatModel instanceof ChatModel) ? (ChatModel) streamingChatModel : null;
        this.streamingChatModel =
                Objects.requireNonNull(streamingChatModel, "streamingChatModel cannot be null");
        this.objectMapper = new ObjectMapper();
        this.messageConverter = new MyMessageConverter(objectMapper);
        this.observabilityHandler =
                new SpringAIObservabilityHandler(createDefaultObservabilityConfig());
    }

    public MySpringAI(ChatModel chatModel, StreamingChatModel streamingChatModel, String modelName) {
        super(Objects.requireNonNull(modelName, "model name cannot be null"));
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel cannot be null");
        this.streamingChatModel =
                Objects.requireNonNull(streamingChatModel, "streamingChatModel cannot be null");
        this.objectMapper = new ObjectMapper();
        this.messageConverter = new MyMessageConverter(objectMapper);
        this.observabilityHandler =
                new SpringAIObservabilityHandler(createDefaultObservabilityConfig());
    }

    public MySpringAI(
            ChatModel chatModel,
            StreamingChatModel streamingChatModel,
            String modelName,
            SpringAIProperties.Observability observabilityConfig) {
        super(Objects.requireNonNull(modelName, "model name cannot be null"));
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel cannot be null");
        this.streamingChatModel =
                Objects.requireNonNull(streamingChatModel, "streamingChatModel cannot be null");
        this.objectMapper = new ObjectMapper();
        this.messageConverter = new MyMessageConverter(objectMapper);
        this.observabilityHandler =
                new SpringAIObservabilityHandler(
                        Objects.requireNonNull(observabilityConfig, "observabilityConfig cannot be null"));
    }

    public MySpringAI(
            ChatModel chatModel, String modelName, SpringAIProperties.Observability observabilityConfig) {
        super(Objects.requireNonNull(modelName, "model name cannot be null"));
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel cannot be null");
        this.streamingChatModel =
                (chatModel instanceof StreamingChatModel) ? (StreamingChatModel) chatModel : null;
        this.objectMapper = new ObjectMapper();
        this.messageConverter = new MyMessageConverter(objectMapper);
        this.observabilityHandler =
                new SpringAIObservabilityHandler(
                        Objects.requireNonNull(observabilityConfig, "observabilityConfig cannot be null"));
    }

    public MySpringAI(
            StreamingChatModel streamingChatModel,
            String modelName,
            SpringAIProperties.Observability observabilityConfig) {
        super(Objects.requireNonNull(modelName, "model name cannot be null"));
        this.chatModel =
                (streamingChatModel instanceof ChatModel) ? (ChatModel) streamingChatModel : null;
        this.streamingChatModel =
                Objects.requireNonNull(streamingChatModel, "streamingChatModel cannot be null");
        this.objectMapper = new ObjectMapper();
        this.messageConverter = new MyMessageConverter(objectMapper);
        this.observabilityHandler =
                new SpringAIObservabilityHandler(
                        Objects.requireNonNull(observabilityConfig, "observabilityConfig cannot be null"));
    }

    @Override
    public Flowable<LlmResponse> generateContent(LlmRequest llmRequest, boolean stream) {
        if (stream) {
            if (this.streamingChatModel == null) {
                return Flowable.error(new IllegalStateException("StreamingChatModel is not configured"));
            }

            return generateStreamingContent(llmRequest);
        } else {
            if (this.chatModel == null) {
                return Flowable.error(new IllegalStateException("ChatModel is not configured"));
            }

            return generateContent(llmRequest);
        }
    }

    private Flowable<LlmResponse> generateContent(LlmRequest llmRequest) {
        SpringAIObservabilityHandler.RequestContext context =
                observabilityHandler.startRequest(model(), "chat");

        try {
            Prompt prompt = messageConverter.toLlmPrompt(llmRequest);
            observabilityHandler.logRequest(prompt.toString(), model());

            ChatResponse chatResponse = chatModel.call(prompt);
            LlmResponse llmResponse = messageConverter.toLlmResponse(chatResponse);

            observabilityHandler.logResponse(extractTextFromResponse(llmResponse), model());

            // Extract token counts if available
            int totalTokens = extractTokenCount(chatResponse);
            int inputTokens = extractInputTokenCount(chatResponse);
            int outputTokens = extractOutputTokenCount(chatResponse);

            observabilityHandler.recordSuccess(context, totalTokens, inputTokens, outputTokens);
            return Flowable.just(llmResponse);
        } catch (Exception e) {
            observabilityHandler.recordError(context, e);
            SpringAIErrorMapper.MappedError mappedError = SpringAIErrorMapper.mapError(e);

            return Flowable.error(new RuntimeException(mappedError.getNormalizedMessage(), e));
        }
    }

    /**
     * 流式生成（增量聚合版）。
     * <p>
     * ADK 对流式事件的语义要求与 Python ADK 一致：partial=true 的增量事件只推送给调用方、
     * 不参与会话持久化与 output-key 保存，每次模型调用结束时必须补发一条 partial=false 的
     * 全量事件承载完整文本。而父类 MessageConverter 的流式转换是「逐 chunk 直转」，partial
     * 靠「文本是否以句号等结尾」的启发式判定，纯 XML/JSON 输出会被永远判为 partial，导致
     * 工作流的 output-key（如 analysis_result -> draft_diagram）永远无法保存。
     * <p>
     * 这里改为自行聚合：每个含文本的 chunk 发一条 partial=true 的增量事件，流结束时补发
     * 一条 partial=false 的全量事件（含累积文本与工具调用），保证下游工作流语义完整。
     */
    private Flowable<LlmResponse> generateStreamingContent(LlmRequest llmRequest) {
        SpringAIObservabilityHandler.RequestContext context =
                observabilityHandler.startRequest(model(), "streaming");

        return Flowable.create(
                emitter -> {
                    try {
                        Prompt prompt = messageConverter.toLlmPrompt(llmRequest);
                        observabilityHandler.logRequest(prompt.toString(), model());

                        StringBuilder aggregatedText = new StringBuilder();
                        List<AssistantMessage.ToolCall> aggregatedToolCalls = new ArrayList<>();
                        AtomicReference<String> finishReasonRef = new AtomicReference<>(null);

                        Flux<ChatResponse> responseFlux = streamingChatModel.stream(prompt);

                        responseFlux
                                .doOnError(
                                        error -> {
                                            observabilityHandler.recordError(context, error);
                                            SpringAIErrorMapper.MappedError mappedError =
                                                    SpringAIErrorMapper.mapError(error);
                                            emitter.onError(
                                                    new RuntimeException(mappedError.getNormalizedMessage(), error));
                                        })
                                .subscribe(
                                        chatResponse -> {
                                            try {
                                                LlmResponse delta =
                                                        toDeltaResponse(chatResponse, aggregatedText, aggregatedToolCalls, finishReasonRef);
                                                if (delta != null) {
                                                    emitter.onNext(delta);
                                                }
                                            } catch (Exception e) {
                                                observabilityHandler.recordError(context, e);
                                                SpringAIErrorMapper.MappedError mappedError =
                                                        SpringAIErrorMapper.mapError(e);
                                                emitter.onError(
                                                        new RuntimeException(mappedError.getNormalizedMessage(), e));
                                            }
                                        },
                                        error -> {
                                            observabilityHandler.recordError(context, error);
                                            SpringAIErrorMapper.MappedError mappedError =
                                                    SpringAIErrorMapper.mapError(error);
                                            emitter.onError(
                                                    new RuntimeException(mappedError.getNormalizedMessage(), error));
                                        },
                                        () -> {
                                            // Record success for streaming completion
                                            observabilityHandler.recordSuccess(context, 0, 0, 0);
                                            try {
                                                emitter.onNext(
                                                        toAggregatedResponse(aggregatedText, aggregatedToolCalls, finishReasonRef.get()));
                                                emitter.onComplete();
                                            } catch (Exception e) {
                                                observabilityHandler.recordError(context, e);
                                                SpringAIErrorMapper.MappedError mappedError =
                                                        SpringAIErrorMapper.mapError(e);
                                                emitter.onError(
                                                        new RuntimeException(mappedError.getNormalizedMessage(), e));
                                            }
                                        });
                    } catch (Exception e) {
                        observabilityHandler.recordError(context, e);
                        SpringAIErrorMapper.MappedError mappedError = SpringAIErrorMapper.mapError(e);
                        emitter.onError(new RuntimeException(mappedError.getNormalizedMessage(), e));
                    }
                },
                BackpressureStrategy.BUFFER);
    }

    /**
     * 把单个流式 chunk 转为增量事件：抽取本次增量文本（partial=true），同时累积文本与工具调用供聚合使用。
     *
     * @return 增量事件；chunk 无文本（如纯 usage/空帧）时返回 null 不下发
     */
    private LlmResponse toDeltaResponse(
            ChatResponse chatResponse,
            StringBuilder aggregatedText,
            List<AssistantMessage.ToolCall> aggregatedToolCalls,
            AtomicReference<String> finishReasonRef) {
        Generation generation = chatResponse.getResult();
        if (generation == null) {
            return null;
        }
        AssistantMessage output = generation.getOutput();
        if (output == null) {
            return null;
        }
        if (generation.getMetadata() != null) {
            String finishReason = generation.getMetadata().getFinishReason();
            if (finishReason != null) {
                finishReasonRef.set(finishReason);
            }
        }
        if (output.getToolCalls() != null && !output.getToolCalls().isEmpty()) {
            aggregatedToolCalls.addAll(output.getToolCalls());
        }
        String delta = output.getText();
        if (delta == null || delta.isEmpty()) {
            return null;
        }
        aggregatedText.append(delta);
        Content deltaContent = Content.builder()
                .role("model")
                .parts(List.of(Part.fromText(delta)))
                .build();
        return LlmResponse.builder().content(deltaContent).partial(Boolean.TRUE).build();
    }

    /**
     * 流结束后构建全量事件（partial=false）：承载完整文本与工具调用，
     * 参与 ADK 的会话持久化与 output-key 保存，使工作流状态机语义与官方模型适配层一致。
     */
    private LlmResponse toAggregatedResponse(
            StringBuilder aggregatedText, List<AssistantMessage.ToolCall> toolCalls, String finishReason) {
        LlmResponse.Builder builder = LlmResponse.builder();
        String text = aggregatedText.toString();
        List<Part> parts = new ArrayList<>();
        if (!text.isEmpty()) {
            parts.add(Part.fromText(text));
        }
        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            parts.add(Part.fromFunctionCall(toolCall.name(), parseToolCallArguments(toolCall)));
        }
        if (!parts.isEmpty()) {
            builder.content(Content.builder().role("model").parts(parts).build());
        }
        builder.partial(Boolean.FALSE);
        // 与父类 isTurnCompleteResponse 的判定保持一致：finish_reason 为 stop / tool_calls 视为轮次结束
        builder.turnComplete("stop".equalsIgnoreCase(finishReason) || "tool_calls".equalsIgnoreCase(finishReason));
        return builder.build();
    }

    /**
     * 解析工具调用参数 JSON；解析失败时回退为空参数，避免整个聚合事件失败。
     */
    private Map<String, Object> parseToolCallArguments(AssistantMessage.ToolCall toolCall) {
        try {
            String arguments = toolCall.arguments();
            if (arguments != null && !arguments.isEmpty()) {
                return objectMapper.readValue(arguments, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception ignored) {
            // 参数解析失败不影响事件语义，交由下游工具层的参数校验兜底
        }
        return Map.of();
    }

    @Override
    public BaseLlmConnection connect(LlmRequest llmRequest) {
        throw new UnsupportedOperationException(
                "Live connection is not supported for Spring AI models.");
    }

    private static String extractModelName(Object model) {
        // Spring AI models may not always have a straightforward way to get model name
        // This is a fallback that can be overridden by providing explicit model name
        String className = model.getClass().getSimpleName();
        return className.toLowerCase().replace("chatmodel", "").replace("model", "");
    }

    private SpringAIProperties.Observability createDefaultObservabilityConfig() {
        SpringAIProperties.Observability config = new SpringAIProperties.Observability();
        config.setEnabled(true);
        config.setMetricsEnabled(true);
        config.setIncludeContent(false);
        return config;
    }

    private int extractTokenCount(ChatResponse chatResponse) {
        // Spring AI may include usage metadata in the response
        // This is a simplified implementation - actual token counts depend on provider
        try {
            if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
                return chatResponse.getMetadata().getUsage().getTotalTokens();
            }
        } catch (Exception e) {
            // Ignore errors in token extraction
        }
        return 0;
    }

    private int extractInputTokenCount(ChatResponse chatResponse) {
        try {
            if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
                return chatResponse.getMetadata().getUsage().getPromptTokens();
            }
        } catch (Exception e) {
            // Ignore errors in token extraction
        }
        return 0;
    }

    private int extractOutputTokenCount(ChatResponse chatResponse) {
        try {
            if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
                return chatResponse.getMetadata().getUsage().getCompletionTokens();
            }
        } catch (Exception e) {
            // Ignore errors in token extraction
        }
        return 0;
    }

    private String extractTextFromResponse(LlmResponse response) {
        if (response.content().isPresent() && response.content().get().parts().isPresent()) {
            return response.content().get().parts().get().stream()
                    .map(part -> part.text().orElse(""))
                    .filter(text -> text != null && !text.isEmpty())
                    .findFirst()
                    .orElse("");
        }
        return "";
    }
}
