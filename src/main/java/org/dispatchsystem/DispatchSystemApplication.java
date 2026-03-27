package org.dispatchsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling

public class DispatchSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(DispatchSystemApplication.class, args);
    }
}