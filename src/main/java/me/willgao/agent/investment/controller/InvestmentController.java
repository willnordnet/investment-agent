package me.willgao.agent.investment.controller;

import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.web.rest.AgentProcessStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.willgao.agent.investment.type.PortfolioRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/agent")
public class InvestmentController {

    private final AgentPlatform agentPlatform;


    @PostMapping("/portfolios")
    public void portfolio(@RequestBody PortfolioRequest request) {
        log.info("Received portfolio: {}", request);

        var agent = agentPlatform.agents().stream()
            .filter(a -> a.getName().equals("InvestmentAgent"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("InvestmentAgent not found"));

        AgentProcess agentProcess = agentPlatform.createAgentProcess(
            agent,
            ProcessOptions.DEFAULT,
            Map.of("it", request.portfolio())
        );

        agentPlatform.start(agentProcess);

        String processId = agentProcess.getId();
        AgentProcessStatus status = new AgentProcessStatus(
            processId,
            agentProcess.getStatus(),
            agentProcess.getTimestamp(),
            agentProcess.getRunningTime(),
            null,  // result not available yet
            "/api/v1/process/" + processId,
            "/events/process/" + processId
        );
        log.info("Started with status: {}", status);

    }


}
