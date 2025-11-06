package me.willgao.agent.investment.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import me.willgao.agent.investment.type.EnrichedInstrument;
import me.willgao.agent.investment.type.EnrichedPortfolio;
import me.willgao.agent.investment.type.Instrument;
import me.willgao.agent.investment.type.Portfolio;
import me.willgao.agent.investment.type.Proposal;
import me.willgao.agent.investment.type.Rating;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Agent(description = "Investment agent")
@Component
public class InvestmentAgent {

    private final ObjectMapper objectMapper;
    private Map<String, EnrichedInstrument> enrichedInstrumentMap;
    private String ratingSchema;

    public InvestmentAgent(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private Map<String, EnrichedInstrument> loadInstruments() {
        String instrumentsJson;
        try {
            instrumentsJson = new ClassPathResource("json/instruments.json").getContentAsString(StandardCharsets.UTF_8);
            final List<EnrichedInstrument> enrichedInstruments = objectMapper.readValue(instrumentsJson, new TypeReference<>() {
            });
            return enrichedInstruments.stream()
                .collect(Collectors.toMap(
                    EnrichedInstrument::name,
                    instrument -> instrument
                ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    public void loadMockData() throws IOException {
        enrichedInstrumentMap = loadInstruments();
        ratingSchema = new ClassPathResource("json/ratingResponseSchema.json").getContentAsString(StandardCharsets.UTF_8);
    }

    @Action(description = "enrichPortfolio")
    public EnrichedPortfolio enrichPortfolio(Portfolio it, OperationContext context) {
        List<EnrichedInstrument> enrichedInstruments = new LinkedList<>();
        it.instruments().forEach(instrument -> {
            EnrichedInstrument e = enrichedInstrumentMap.get(instrument.name());
            if (e != null) {
                enrichedInstruments.add(e);
            } else {
                log.warn("No enriched data found for instrument: {}", instrument.name());
            }
        });
        log.info("Enriched {} instruments with more data", enrichedInstruments.size());
        return new EnrichedPortfolio(enrichedInstruments);
    }

    @Action(description = "rate")
    public Rating rate(Portfolio it, EnrichedPortfolio enrichedPortfolio, OperationContext context) throws Exception {
        String systemPrompt = """
            You are an expert financial advisor.
            Rate the quality of the provided portfolio.
            Portfolio: %s.
            Respond with only minimal raw json according to the schema: %s.
            Output minimal raw JSON (no markdown, no explanation, no spaces or line breaks)
            """;

        String json = context.ai()
            .withAutoLlm()
            .generateText(String.format(systemPrompt, enrichedPortfolio, ratingSchema));

        log.info("Received rating JSON: {}", json);

        return objectMapper.readValue(json, Rating.class);
    }

    @Action(description = "propose")
    @AchievesGoal(
        description = "Propose investment advice based on user's question."
    )
    public Proposal propose(Portfolio it, EnrichedPortfolio enrichedPortfolio, Rating rating, OperationContext context) throws JsonProcessingException {
        String systemPrompt = """
            You are an expert financial advisor.
            The rating of the user's portfolio is: %s.
            Based on the rating and portfolio %s, propose a diversified portfolio of investment instruments.
            Rules:
            - Use ONLY the provided instruments
            - Output minimal raw JSON array (no markdown, no explanation, no spaces or line breaks)
            - Each object: {"name": "...", "weight": 0.00}
            - Weights sum to 100.00
            - Maximum 20 instruments in response
            """;

        List<Instrument> instruments;
        String json = context.ai()
            .withAutoLlm()
            .generateText(String.format(systemPrompt, rating.recommendation(), enrichedPortfolio));

        log.info("Received proposed instruments JSON: {}", json);

        instruments = objectMapper.readValue(json, new TypeReference<>() {
        });

        return new Proposal(instruments);
    }
}
