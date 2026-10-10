package com.mandiwatch.repository;

import java.time.LocalDate;

/** Like {@link DailyAverage} but for every commodity at once, so callers need one query instead of one per crop. */
public record CommodityDailyAverage(Long commodityId, LocalDate date, Double average) {
}
