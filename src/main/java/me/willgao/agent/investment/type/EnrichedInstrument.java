package me.willgao.agent.investment.type;

public record EnrichedInstrument(String name,
                                 String weight,
                                 String fee,
                                 String risk,
                                 String type,
                                 String currency,
                                 String suggestion) {
}
