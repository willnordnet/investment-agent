package me.willgao.agent.investment.controller;

import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.web.rest.AgentProcessStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/agent")
public class ChatController {

    private final AgentPlatform agentPlatform;

    public record ChatRequest(String question) {
    }


    @PostMapping("/chat")
    public void chat(@RequestBody ChatRequest request) {
        log.info("Received chat request: {}", request.question());
        var agent = agentPlatform.agents().stream()
            .filter(a -> a.getName().equals("ChatAgent"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("ChatAgent not found"));

        AgentProcess agentProcess = agentPlatform.createAgentProcess(
            agent,
            ProcessOptions.DEFAULT,
            Map.of("it", request.question())
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
