package com.mandiwatch.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RawPriceRowTest {

    @Test
    void readsCsvDownloadHeaders() {
        RawPriceRow row = RawPriceRow.fromMap(Map.of(
                "State", "Telangana", "District", "Hyderabad", "Market", "Bowenpally",
                "Commodity", "Tomato", "Arrival_Date", "03/10/2026",
                "Min_x0020_Price", "1500", "Max_x0020_Price", "2100", "Modal_x0020_Price", "1800"));

        assertThat(row.market()).isEqualTo("Bowenpally");
        assertThat(row.modalPrice()).isEqualTo("1800");
        assertThat(row.parseDate()).isEqualTo(LocalDate.of(2026, 10, 3));
    }

    @Test
    void readsApiJsonWithNumbers() {
        RawPriceRow row = RawPriceRow.fromMap(Map.of(
                "state", "Telangana", "district", "Warangal", "market", "Warangal",
                "commodity", "Cotton", "arrival_date", "2026-10-03",
                "min_price", 7000, "max_price", 7400.0, "modal_price", "7,250"));

        assertThat(RawPriceRow.parsePrice(row.maxPrice())).isEqualTo(7400);
        assertThat(RawPriceRow.parsePrice(row.modalPrice())).isEqualTo(7250);
        assertThat(row.parseDate()).isEqualTo(LocalDate.of(2026, 10, 3));
    }

    @Test
    void badValuesBecomeNull() {
        assertThat(RawPriceRow.parsePrice("NA")).isNull();
        RawPriceRow row = new RawPriceRow("s", "d", "m", "c", null, "31/31/2026", "1", "2", "1");
        assertThat(row.parseDate()).isNull();
    }
}
