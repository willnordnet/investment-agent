package me.willgao.agent.investment;

import com.embabel.agent.config.annotation.EnableAgents;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableAgents(loggingTheme = "starwars")
@SpringBootApplication
class Application {
    void main() {

        SpringApplication.run(Application.class);

    }

}
