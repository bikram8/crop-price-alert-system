package com.mandiwatch.service;

import com.mandiwatch.domain.ImportRun;
import com.mandiwatch.repository.ImportRunRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Shows up as "lastImport" under /actuator/health. Spring Boot picks up any HealthIndicator
 * bean automatically and uses the bean name minus "HealthIndicator" as the key.
 * <p>
 * The app can be "up" while serving week-old prices, which is useless to a farmer.
 * This makes that visible to whoever monitors the app.
 */
@Component
public class LastImportHealthIndicator implements HealthIndicator {

    private static final Duration STALE_AFTER = Duration.ofDays(2);

    private final ImportRunRepository importRunRepository;
    private final Clock clock;

    public LastImportHealthIndicator(ImportRunRepository importRunRepository, Clock clock) {
        this.importRunRepository = importRunRepository;
        this.clock = clock;
    }

    @Override
    public Health health() {
        return importRunRepository.findFirstBySuccessTrueOrderByStartedAtDesc()
                .map(this::describe)
                .orElseGet(() -> Health.unknown().withDetail("reason", "no successful import yet").build());
    }

    private Health describe(ImportRun run) {
        Duration age = Duration.between(run.getStartedAt(), LocalDateTime.now(clock));
        // Stale data is reported as a detail, not DOWN: the app itself still works,
        // and DOWN would make a load balancer take it out of service.
        return Health.up()
                .withDetail("source", run.getSource())
                .withDetail("at", run.getStartedAt().toString())
                .withDetail("rowsSaved", run.getRowsSaved())
                .withDetail("stale", age.compareTo(STALE_AFTER) > 0)
                .build();
    }
}
