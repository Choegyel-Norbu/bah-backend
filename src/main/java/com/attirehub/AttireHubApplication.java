package com.attirehub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.attirehub")
@EnableJpaRepositories(basePackages = "com.attirehub")
public class AttireHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(AttireHubApplication.class, args);
    }
}
