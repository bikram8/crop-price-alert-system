package com.mandiwatch;

import com.mandiwatch.config.MandiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(MandiProperties.class)
public class MandiWatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(MandiWatchApplication.class, args);
    }
}
