package com.mandiwatch.service;

import java.time.LocalDate;

/** Published after new prices are stored. */
public record PricesImportedEvent(Long importRunId, LocalDate latestDate) {
}
