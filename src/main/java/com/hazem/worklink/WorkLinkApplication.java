package com.hazem.worklink;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WorkLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkLinkApplication.class, args);
    }
}
