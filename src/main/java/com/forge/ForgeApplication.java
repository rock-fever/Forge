package com.forge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ForgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(ForgeApplication.class, args);
    }
}
