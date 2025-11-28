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
import me.willgao.agent.investment.type.CustomerProfile;
import me.willgao.agent.investment.type.EnrichedInstrument;
import me.willgao.agent.investment.type.EnrichedPortfolio;
import me.willgao.agent.investment.type.Portfolio;
import me.willgao.agent.investment.type.Proposal;
import me.willgao.agent.investment.type.Rating;
import me.willgao.agent.investment.type.SuggestedInstrument;
import org.springframework.context.ApplicationEventPublisher;
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
    private String customerProfile;
    private final ApplicationEventPublisher eventPublisher;

    public InvestmentAgent(ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher) {
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
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
        customerProfile = new ClassPathResource("json/customerProfile.json").getContentAsString(StandardCharsets.UTF_8);
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
        return new EnrichedPortfolio(enrichedInstruments, it.amount());
    }

    @Action(description = "enrichCustomerInformation")
    public CustomerProfile enrichCustomerInformation(Portfolio it, OperationContext context) throws JsonProcessingException {
        final CustomerProfile profile = objectMapper.readValue(customerProfile, CustomerProfile.class);
        log.info("Loaded customer profile: {}", profile);
        return profile;
    }

    @Action(description = "rate")
    public Rating rate(Portfolio it, EnrichedPortfolio enrichedPortfolio, CustomerProfile profile, OperationContext context) throws Exception {
        String systemPrompt = """
            ### ROLE
            You are an Expert Investment Suitability Engine. Your objective is to audit the alignment between a specific investment portfolio and a customer's financial profile.
            
            ### INPUT DATA
            1. **Target Portfolio:** %s
            2. **Customer Profile:** %s
            3. **Required JSON Schema:** %s
            
            ### ALGORITHM
            Perform the following logic step-by-step before generating output:
            1. **Profile Analysis:** Identify the customer's Risk Tolerance (e.g., Aggressive, Conservative), Time Horizon, and specific constraints (e.g., "No fossil fuels").
            Instrument with 0 weight should not be considered in the portfolio analysis.
            2. **Portfolio Audit:** specific asset allocation risks and implied volatility of the `Target Portfolio`.
            3. **Gap Analysis:** Compare the Profile vs. the Portfolio. Does the portfolio take too much risk? Too little? Does it violate constraints?
            4. **Evidence Extraction:** specific the exact sentence from the `Customer Profile` that proves your assessment.
            5. **Compliment**: Always provide positive feedback citing specific aspects in the first paragraph.
            
            ### OUTPUT RULES
            - **Format:** Return ONLY a single, valid, minified JSON object matching the `Required JSON Schema`.
            - **Reasoning Field:** Your "reason" value must explicitly contrast the customer's needs against the portfolio's composition.
            - **Quote Field:** Your "quote" value must be a verbatim substring extracted from the `Customer Profile`.
            - **Constraints:** No Markdown (```json). No explanatory text outside the JSON object. No whitespace.
            
            ### EXECUTE
            """.formatted(objectMapper.writeValueAsString(enrichedPortfolio), objectMapper.writeValueAsString(profile), ratingSchema);

        String json = context.ai()
            .withLlm("gemini-3.0-pro")
            .generateText(systemPrompt);

        log.info("Received rating JSON: {}", json);

        final Rating rating = objectMapper.readValue(json, Rating.class);
        eventPublisher.publishEvent(rating);
        return rating;
    }

    @Action(description = "propose")
    @AchievesGoal(
        description = "Propose investment advice based on user's question."
    )
    public Proposal propose(Portfolio it, EnrichedPortfolio enrichedPortfolio, Rating rating, OperationContext context) throws JsonProcessingException {
        String systemPrompt = """
            ### ROLE
            You are a Quantitative Portfolio Manager. Your goal is to construct an optimal asset allocation from a fixed universe of instruments.
            
            ### INPUT DATA
            1. **Target Strategy/Rating:** %s
            2. **Available Instruments Universe:** %s
            3. **Customer Current Portfolio:** %s
            4. **Customer Hard Constraints:** %s
            
            ### ALGORITHM
            Based on the rating and current portfolio, propose a diversified portfolio of investment instruments.
            Rules:
            - Constraint Check: `Customer Hard Constraints` must be strictly followed regardless of other factors.
            - Evaluate all the provided instruments and cash from the Available Instruments Universe.
            - Add all customer provided instruments into the response list even if you set an instrument's weight to 0.00.
            - Explain the reason for each instrument's weight.
            - Maximum 20 instruments in response.
            - Active Universe Scanning: specific the `Target Strategy` requirements (e.g., need for hedging, growth, or income).
            You must scan the entire `Available Instruments Universe` and recommend (assign positive weight to) specific instruments that fill gaps in diversification or risk management, even if they were not explicitly requested.
            - Strategy Alignment: Distribute weights among the selected instruments. Higher risk strategies should favor equities/volatility; lower risk strategies favor bonds/stablecoins.
            
            ### OUTPUT RULES
            - Return a single JSON Array.
            - Output minimal raw JSON array (no markdown, no explanation, no spaces or line breaks)
            - keys: "name", "weight" (float, 2 decimals), "reason" (string).
            - **Strict Math:** The sum of all "weight" values must be exactly 100.00.
            - **Format:** Minified JSON only. No markdown.
            
            ### EXECUTE
            """.formatted(rating.recommendation(), objectMapper.writeValueAsString(enrichedInstrumentMap.values()), objectMapper.writeValueAsString(enrichedPortfolio), it.customerInstructions());

        List<SuggestedInstrument> instruments;
        String json = context.ai()
            .withLlm("gemini-3.0-pro")
            .generateText(systemPrompt);

        log.info("Received proposed instruments JSON: {}", json);

        instruments = objectMapper.readValue(json, new TypeReference<>() {
        });
        Proposal proposal = new Proposal(instruments);
        eventPublisher.publishEvent(proposal);
        return proposal;
    }
}
