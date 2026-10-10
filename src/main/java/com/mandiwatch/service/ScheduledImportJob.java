package com.mandiwatch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Pulls the day's bulletin automatically. Off by default so the app doesn't
 * try to reach the internet during a demo; switch on with mandi.api.scheduled=true.
 * The feed fills up through the day, so the default cron runs in the evening (IST).
 */
@Component
@ConditionalOnProperty(name = "mandi.api.scheduled", havingValue = "true")
public class ScheduledImportJob {

    private static final Logger log = LoggerFactory.getLogger(ScheduledImportJob.class);

    private final ApiImportService apiImportService;

    public ScheduledImportJob(ApiImportService apiImportService) {
        this.apiImportService = apiImportService;
    }

    @Scheduled(cron = "${mandi.api.cron}", zone = "Asia/Kolkata")
    public void pullDailyBulletin() {
        log.info("Scheduled import starting");
        apiImportService.runImport();
    }
}
