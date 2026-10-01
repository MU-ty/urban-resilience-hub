package com.example.resilience;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ResilienceHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResilienceHubApplication.class, args);
    }
}
