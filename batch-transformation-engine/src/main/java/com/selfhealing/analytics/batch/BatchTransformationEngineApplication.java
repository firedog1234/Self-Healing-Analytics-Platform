package com.selfhealing.analytics.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BatchTransformationEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(BatchTransformationEngineApplication.class, args);
    }
}
