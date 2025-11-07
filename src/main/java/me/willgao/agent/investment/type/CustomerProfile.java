package me.willgao.agent.investment.type;

public record CustomerProfile(
    String dateOfBirth,          // e.g., "1985-10-20"
    String maritalStatus,        // e.g., "Single", "Married", "Divorced"
    String numberOfDependents,   // e.g., "2"
    String employmentStatus,     // e.g., "Employed", "Self-Employed", "Retired"
    String annualIncome,         // e.g., "120000"
    String totalNetWorth,        // e.g., "750000"
    String existingLiabilities,  // e.g., "Mortgage: 200000, Car Loan: 15000"
    String investmentObjectives // e.g., "Retirement, Education, Capital Growth"
) {
}
