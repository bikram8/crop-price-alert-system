package com.mandiwatch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableResilientMethods // turns on Spring Framework 7's built-in @Retryable (used by DataGovClient)
public class AppConfig {

    /**
     * Code asks this clock for "today" instead of calling LocalDate.now() directly.
     * Mandi bulletins are dated in Indian time, so the zone is fixed rather than
     * taken from whatever server the app runs on. Tests can swap in Clock.fixed(...).
     */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Kolkata"));
    }
}
