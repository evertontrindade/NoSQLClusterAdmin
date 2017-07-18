package br.com.evertontrindade.nosql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ComponentScan
@EnableAutoConfiguration
@EnableScheduling
@SpringBootApplication
public class NosqlGerenicConfigurationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NosqlGerenicConfigurationApplication.class, args);
    }
}
