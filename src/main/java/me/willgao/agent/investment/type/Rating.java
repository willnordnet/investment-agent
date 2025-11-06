package me.willgao.agent.investment.type;

public record Rating(
    ScoreLevel overallScore,
    RiskLevel riskLevel,
    ScoreLevel diversificationScore,
    ScoreLevel performanceScore,
    ScoreLevel costEfficiencyScore,
    String recommendation
) {

    enum ScoreLevel {
        EXCELLENT,
        GOOD,
        AVERAGE,
        BELOW_AVERAGE,
        POOR
    }

    enum RiskLevel {
        LOW,
        MODERATE,
        HIGH
    }
}

