package com.splitit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SplitItApplication {

    public static void main(String[] args) {
        SpringApplication.run(SplitItApplication.class, args);
    }
}
