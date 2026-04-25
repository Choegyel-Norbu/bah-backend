package com.attirehub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan(basePackages = "com.attirehub")
@EnableJpaRepositories(basePackages = "com.attirehub")
@EnableScheduling
public class AttireHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(AttireHubApplication.class, args);
    }
}
