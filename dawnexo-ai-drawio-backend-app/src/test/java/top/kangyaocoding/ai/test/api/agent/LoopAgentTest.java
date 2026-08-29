package top.kangyaocoding.ai.test.api.agent;

import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.LoopAgent;
import com.google.adk.agents.SequentialAgent;
import com.google.adk.events.Event;
import com.google.adk.models.springai.SpringAI;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.adk.tools.FunctionTool;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

@Slf4j
public class LoopAgentTest {

    private static final String APP_NAME = "IterativeWritingPipeline";
    private static final String USER_ID = "test_user_456";

    private static final String STATE_CURRENT_DOC = "current_document";
    private static final String STATE_CRITICISM = "criticism";


    public static void main(String[] args) {
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

        LlmAgent initialWriterAgent = LlmAgent.builder()
                .model(new SpringAI(chatModel))
                .name("InitialWriterAgent")
                .description(
                        "Writes the initial document draft based on the topic, aiming for some initial"
                                + " substance.")
                .instruction(
                        """
                                    You are a Creative Writing Assistant tasked with starting a story.
                                    Write the *first draft* of a short story (aim for 2-4 sentences).
                                    Base the content *only* on the topic provided below. Try to introduce a specific element (like a character, a setting detail, or a starting action) to make it engaging.
                                
                                    Output *only* the story/document text. Do not add introductions or explanations.
                                """)
                .outputKey(STATE_CURRENT_DOC)
                .includeContents(LlmAgent.IncludeContents.NONE)
                .build();

        LlmAgent.builder()
                .model(new SpringAI(chatModel));

        LlmAgent criticAgentInLoop =
                LlmAgent.builder()
                        .model(new SpringAI(chatModel))
                        .name("CriticAgent")
                        .description(
                                "Reviews the current draft, providing critique if clear improvements are needed,"
                                        + " otherwise signals completion.")
                        .instruction(
                                """
                                        You are a Constructive Critic AI reviewing a short document draft (typically 2-6 sentences). Your goal is balanced feedback.
                                        
                                        **Document to Review:**
                                        ```
                                        {{current_document}}
                                        ```
                                        
                                        **Task:**
                                        Review the document for clarity, engagement, and basic coherence according to the initial topic (if known).
                                        
                                        IF you identify 1-2 *clear and actionable* ways the document could be improved to better capture the topic or enhance reader engagement (e.g., "Needs a stronger opening sentence", "Clarify the character's goal"):
                                        Provide these specific suggestions concisely. Output *only* the critique text.
                                        
                                        ELSE IF the document is coherent, addresses the topic adequately for its length, and has no glaring errors or obvious omissions:
                                        Respond *exactly* with the phrase "No major issues found." and nothing else. It doesn't need to be perfect, just functionally complete for this stage. Avoid suggesting purely subjective stylistic preferences if the core is sound.
                                        
                                        Do not add explanations. Output only the critique OR the exact completion phrase.
                                        """)
                        .outputKey(STATE_CRITICISM)
                        .includeContents(LlmAgent.IncludeContents.NONE)
                        .build();

        LlmAgent refinerAgentInLoop =
                LlmAgent.builder()
                        .model(new SpringAI(chatModel))
                        .name("RefinerAgent")
                        .description(
                                "Refines the document based on critique, or calls exitLoop if critique indicates"
                                        + " completion.")
                        .instruction(
                                """
                                            You are a Creative Writing Assistant refining a document based on feedback OR exiting the process.
                                            **Current Document:**
                                            ```
                                            {{current_document}}
                                            ```
                                            **Critique/Suggestions:**
                                            {{criticism}}
                                        
                                            **Task:**
                                            Analyze the 'Critique/Suggestions'.
                                            IF the critique is *exactly* "No major issues found.":
                                            You MUST call the 'exitLoop' function. Do not output any text.
                                            ELSE (the critique contains actionable feedback):
                                            Carefully apply the suggestions to improve the 'Current Document'. Output *only* the refined document text.
                                        
                                            Do not add explanations. Either output the refined document OR call the exitLoop function.
                                        """)
                        .outputKey(STATE_CURRENT_DOC)
                        .includeContents(LlmAgent.IncludeContents.NONE)
                        .tools(FunctionTool.create(LoopAgentTest.class, "exitLoop"))
                        .build();

        LoopAgent refinementLoop = LoopAgent.builder()
                .name("RefinementLoop")
                .description("Repeatedly refines the document with critique and then exits.")
                .subAgents(criticAgentInLoop, refinerAgentInLoop)
                .maxIterations(5)
                .build();

        SequentialAgent sequentialAgent = SequentialAgent.builder()
                .name(APP_NAME)
                .description("Writes an initial document and then iteratively refines it with critique using an exit tool.")
                .subAgents(initialWriterAgent, refinementLoop)
                .build();
        InMemoryRunner memoryRunner = new InMemoryRunner(sequentialAgent, APP_NAME);
        Session session = memoryRunner.sessionService().createSession(APP_NAME, USER_ID).blockingGet();
        Content userMessage = Content.fromParts(Part.fromText("Write a document about a cat"));

        Flowable<Event> eventFlowable = memoryRunner.runAsync(USER_ID, session.id(), userMessage);

        eventFlowable.blockingForEach(event -> {
            if (event.finalResponse()) {
                log.info(event.stringifyContent());
            }
        });
    }
}
