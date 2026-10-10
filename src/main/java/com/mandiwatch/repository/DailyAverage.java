package com.mandiwatch.repository;

import java.time.LocalDate;

/**
 * Average modal price of one commodity on one day.
 * Filled by a JPQL constructor expression ("select new ...DailyAverage(...)"),
 * which replaces the old List&lt;Object[]&gt; and its casts.
 */
public record DailyAverage(LocalDate date, Double average) {
}
