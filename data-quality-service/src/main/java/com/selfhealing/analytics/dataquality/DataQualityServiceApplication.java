package com.selfhealing.analytics.dataquality;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DataQualityServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataQualityServiceApplication.class, args);
    }
}
