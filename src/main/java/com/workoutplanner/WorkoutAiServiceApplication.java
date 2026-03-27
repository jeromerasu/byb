package com.workoutplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WorkoutAiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkoutAiServiceApplication.class, args);
    }
}