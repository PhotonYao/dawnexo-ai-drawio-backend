package top.kangyaocoding.ai.domain.agent.service.armory.matter.patch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.models.LlmRequest;
import com.google.adk.models.springai.ConfigMapper;
import com.google.adk.models.springai.MessageConversionException;
import com.google.adk.models.springai.MessageConverter;
import com.google.adk.models.springai.ToolConverter;
import com.google.genai.types.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.MimeType;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @description: MyMessageConverter 是一个自定义的消息转换器类，继承自 MessageConverter。它用于将 LlmRequest 对象转换为 Prompt 对象，同时处理消息中的多媒体内容（如图片、音频、视频等）。在转换过程中，MyMessageConverter 会解析消息的各个部分，包括文本、函数响应、内联数据和文件数据，并将其封装为 Media 对象，以便在 Prompt 中使用。该类还处理了可能出现的异常情况，并记录相关的警告信息，以确保消息转换的稳定性和可靠性。
 * @author: herbert
 * @date: 2026-08-22 20:47
 */
public class MyMessageConverter extends MessageConverter {

    private final ObjectMapper objectMapper;
    private final ToolConverter toolConverter;
    private final ConfigMapper configMapper;

    public MyMessageConverter(ObjectMapper objectMapper) {
        super(objectMapper);
        this.objectMapper = objectMapper;
        // 重新创建父类中的组件（因为父类的这些字段是 private，子类无法复用）
        this.toolConverter = new ToolConverter();
        this.configMapper = new ConfigMapper();
    }

    /**
     * Converts an ADK LlmRequest to a Spring AI Prompt.
     *
     * @param llmRequest The ADK request to convert
     * @return A Spring AI Prompt
     */
    public Prompt toLlmPrompt(LlmRequest llmRequest) {
        List<Message> messages = new ArrayList<>();
        List<String> allSystemMessages = new ArrayList<>();

        // Collect system instructions from LlmRequest
        allSystemMessages.addAll(llmRequest.getSystemInstructions());

        // Collect system messages from Content objects
        List<Message> nonSystemMessages = new ArrayList<>();
        for (Content content : llmRequest.contents()) {
            String role = content.role().orElse("user").toLowerCase();
            if ("system".equals(role)) {
                // Extract text from system content and add to combined system message
                StringBuilder systemText = new StringBuilder();
                for (Part part : content.parts().orElse(List.of())) {
                    if (part.text().isPresent()) {
                        systemText.append(part.text().get());
                    }
                }
                if (systemText.length() > 0) {
                    allSystemMessages.add(systemText.toString());
                }
            } else {
                // Handle non-system messages normally
                nonSystemMessages.addAll(toSpringAiMessages(content));
            }
        }

        // Create single combined SystemMessage if any system content exists
        if (!allSystemMessages.isEmpty()) {
            String combinedSystemMessage = String.join("\n\n", allSystemMessages);
            messages.add(new SystemMessage(combinedSystemMessage));
        }

        // Add all non-system messages
        messages.addAll(nonSystemMessages);

        // Convert config to ChatOptions
        ChatOptions chatOptions = configMapper.toSpringAiChatOptions(llmRequest.config());

        // Convert ADK tools to Spring AI ToolCallback and add to ChatOptions
        if (llmRequest.tools() != null && !llmRequest.tools().isEmpty()) {
            List<ToolCallback> toolCallbacks = toolConverter.convertToSpringAiTools(llmRequest.tools());
            if (!toolCallbacks.isEmpty()) {
                // Create new ChatOptions with tools included
                ToolCallingChatOptions.Builder optionsBuilder = ToolCallingChatOptions.builder();

                // Always set tool callbacks
                optionsBuilder.toolCallbacks(toolCallbacks);

                // Copy existing chat options properties if present
                if (chatOptions != null) {
                    // Copy all relevant properties from existing ChatOptions
                    if (chatOptions.getTemperature() != null) {
                        optionsBuilder.temperature(chatOptions.getTemperature());
                    }
                    if (chatOptions.getMaxTokens() != null) {
                        optionsBuilder.maxTokens(chatOptions.getMaxTokens());
                    }
                    if (chatOptions.getTopP() != null) {
                        optionsBuilder.topP(chatOptions.getTopP());
                    }
                    if (chatOptions.getTopK() != null) {
                        optionsBuilder.topK(chatOptions.getTopK());
                    }
                    if (chatOptions.getStopSequences() != null) {
                        optionsBuilder.stopSequences(chatOptions.getStopSequences());
                    }
                    // Copy model name if present
                    if (chatOptions.getModel() != null) {
                        optionsBuilder.model(chatOptions.getModel());
                    }
                    // Copy frequency penalty if present
                    if (chatOptions.getFrequencyPenalty() != null) {
                        optionsBuilder.frequencyPenalty(chatOptions.getFrequencyPenalty());
                    }
                    // Copy presence penalty if present
                    if (chatOptions.getPresencePenalty() != null) {
                        optionsBuilder.presencePenalty(chatOptions.getPresencePenalty());
                    }
                }

                chatOptions = optionsBuilder.build();
            }
        }

        return new Prompt(messages, chatOptions);
    }

    /**
     * Converts an ADK Content to Spring AI Message(s).
     *
     * @param content The ADK content to convert
     * @return A list of Spring AI messages
     */
    private List<Message> toSpringAiMessages(Content content) {
        String role = content.role().orElse("user").toLowerCase();

        return switch (role) {
            case "user" -> handleUserContent(content);
            case "model", "assistant" -> List.of(handleAssistantContent(content));
            case "system" -> List.of(handleSystemContent(content));
            default -> throw new IllegalStateException("Unexpected role: " + role);
        };
    }

    private List<Message> handleUserContent(Content content) {
        StringBuilder textBuilder = new StringBuilder();
        List<ToolResponseMessage> toolResponseMessages = new ArrayList<>();
        List<Media> mediaList = new ArrayList<>();

        for (Part part : content.parts().orElse(List.of())) {
            if (part.text().isPresent()) {
                textBuilder.append(part.text().get());
            } else if (part.functionResponse().isPresent()) {
                // TODO: Spring AI 1.1.0 ToolResponseMessage constructors are protected
                // For now, we skip tool responses in user messages
                // This will need to be addressed in a future update when Spring AI provides
                // a public API for creating ToolResponseMessage
            } else if (part.inlineData().isPresent()) {
                // Handle inline media data (images, audio, video, etc.)
                com.google.genai.types.Blob blob = part.inlineData().get();
                if (blob.mimeType().isPresent() && blob.data().isPresent()) {
                    try {
                        MimeType mimeType = MimeType.valueOf(blob.mimeType().get());
                        // Create Media object from inline data using ByteArrayResource
                        org.springframework.core.io.ByteArrayResource resource =
                                new org.springframework.core.io.ByteArrayResource(blob.data().get());
                        mediaList.add(new Media(mimeType, resource));
                    } catch (Exception e) {
                        // Log warning but continue processing other parts
                        // In production, consider proper logging framework
                        System.err.println("Warning: Failed to process media part: " + e.getMessage());
                    }
                }
            } else if (part.fileData().isPresent()) {
                // Handle file-based media (URI references)
                com.google.genai.types.FileData fileData = part.fileData().get();
                if (fileData.mimeType().isPresent() && fileData.fileUri().isPresent()) {
                    try {
                        MimeType mimeType = MimeType.valueOf(fileData.mimeType().get());
                        // Create Media object from file URI
                        URI uri = URI.create(fileData.fileUri().get());
                        mediaList.add(new Media(mimeType, uri));
                    } catch (Exception e) {
                        System.err.println("Warning: Failed to process media part: " + e.getMessage());
                    }
                }
            }
        }

        List<Message> messages = new ArrayList<>();
        messages.add(UserMessage.builder().text(textBuilder.toString()).media(mediaList).build());
        messages.addAll(toolResponseMessages);

        return messages;
    }

    private AssistantMessage handleAssistantContent(Content content) {
        StringBuilder textBuilder = new StringBuilder();
        List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();

        for (Part part : content.parts().orElse(List.of())) {
            if (part.text().isPresent()) {
                textBuilder.append(part.text().get());
            } else if (part.functionCall().isPresent()) {
                FunctionCall functionCall = part.functionCall().get();
                toolCalls.add(
                        new AssistantMessage.ToolCall(
                                functionCall
                                        .id()
                                        .orElseThrow(() -> new IllegalStateException("Function call ID is missing")),
                                "function",
                                functionCall
                                        .name()
                                        .orElseThrow(() -> new IllegalStateException("Function call name is missing")),
                                toJson(functionCall.args().orElse(Map.of()))));
            }
        }

        String text = textBuilder.toString();
        if (toolCalls.isEmpty()) {
            return new AssistantMessage(text);
        } else {
            return AssistantMessage.builder().content(text).toolCalls(toolCalls).build();
        }
    }

    private SystemMessage handleSystemContent(Content content) {
        StringBuilder textBuilder = new StringBuilder();
        for (Part part : content.parts().orElse(List.of())) {
            if (part.text().isPresent()) {
                textBuilder.append(part.text().get());
            }
        }
        return new SystemMessage(textBuilder.toString());
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw MessageConversionException.jsonParsingFailed("object serialization", e);
        }
    }
}
