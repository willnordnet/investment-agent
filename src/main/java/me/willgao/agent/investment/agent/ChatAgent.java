package me.willgao.agent.investment.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Export;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.common.ai.model.ModelProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Agent(description = "Chat agent")
@Component
public class ChatAgent {

    private final ModelProvider modelProvider;

    public ChatAgent(ModelProvider modelProvider) {
        this.modelProvider = modelProvider;
    }

    public record EnrichedQuestion(String text) {
    }

    public record FinalAnswer(String text) {
    }

    @Action
    public EnrichedQuestion enrichQuestion(String it, OperationContext context) {
        String systemPrompt = """
            You are an expert assistant that enriches questions for better answers.
            Enhance the following question to be more specific and detailed: %s.
            If the question is about a person, ask the age, family background, and profession of the person.
            Provide the enriched question only.
            """.formatted(it);

        String enrichedQuestion = context.ai()
            .withAutoLlm()
            .generateText(String.format(systemPrompt, it));

        log.info("Enriched question: {}", enrichedQuestion);

        return new EnrichedQuestion(enrichedQuestion);
    }

    @Action(description = "answer")
    @AchievesGoal(
        description = "Answer user's question.",
        export = @Export(name = "answerQuestion", startingInputTypes = {String.class})
    )
    public FinalAnswer answer(EnrichedQuestion it, OperationContext context) {
        String systemPrompt = "Prefix the answer with 'Answer: '. Answer the following question concisely: %s. ";

        String answer = context.ai()
            .withAutoLlm()
            .generateText(String.format(systemPrompt, it.text()));

        log.info("Final answer: {}", answer);

        return new FinalAnswer(answer);
    }
}
