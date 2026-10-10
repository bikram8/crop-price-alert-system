package com.mandiwatch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Turns on @CreatedDate handling.
 * <p>
 * This lives in its own class rather than on MandiWatchApplication on purpose: test slices
 * such as @WebMvcTest still load the main application class, and @EnableJpaAuditing there
 * would make them fail because there is no JPA in a web-only slice.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaConfig {

    /** Audit timestamps come from the same Clock bean as the rest of the app. */
    @Bean
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(LocalDateTime.now(clock));
    }
}
