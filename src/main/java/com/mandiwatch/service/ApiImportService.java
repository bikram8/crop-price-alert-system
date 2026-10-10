package com.mandiwatch.service;

import com.mandiwatch.domain.ImportRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/** Glue between the HTTP client and the import logic, with failures recorded instead of thrown. */
@Service
public class ApiImportService {

    private static final Logger log = LoggerFactory.getLogger(ApiImportService.class);
    static final String SOURCE = "data.gov.in API";

    private final DataGovClient client;
    private final PriceImportService importService;

    public ApiImportService(DataGovClient client, PriceImportService importService) {
        this.client = client;
        this.importService = importService;
    }

    public ImportRun runImport() {
        try {
            List<RawPriceRow> rows = client.fetchTodaysPrices();
            return importService.importRows(SOURCE, rows);
        } catch (RuntimeException e) {
            // missing key, HTTP error after retries ran out, bad JSON...: record it, don't crash the caller
            log.warn("API import failed: {}", e.getMessage());
            return importService.recordFailure(SOURCE, e.getMessage());
        }
    }
}
