package me.willgao.agent.investment.type;

import java.util.List;

public record Portfolio(List<Instrument> instruments, String amount) {
}
