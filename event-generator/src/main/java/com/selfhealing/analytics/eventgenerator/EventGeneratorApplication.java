package com.selfhealing.analytics.eventgenerator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EventGeneratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventGeneratorApplication.class, args);
    }
}
