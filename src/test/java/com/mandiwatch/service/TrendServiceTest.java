package com.mandiwatch.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrendServiceTest {

    private final TrendService service = new TrendService(null, java.time.Clock.systemDefaultZone());

    @Test
    void steadyRiseIsProjectedForward() {
        List<Integer> prices = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            prices.add(2000 + i * 10); // +10 a day
        }
        // last value 2130, 7 more days at +10 = 2200
        assertThat(TrendService.projectAhead(prices, 14, 7)).isEqualTo(2200);
    }

    @Test
    void needsEnoughPointsToProject() {
        assertThat(TrendService.projectAhead(List.of(1, 2, 3), 14, 7)).isNull();
    }

    @Test
    void summaryDetectsRisingPrices() {
        List<LocalDate> dates = new ArrayList<>();
        List<Integer> prices = new ArrayList<>();
        LocalDate start = LocalDate.of(2026, 9, 1);
        for (int i = 0; i < 14; i++) {
            dates.add(start.plusDays(i));
            prices.add(i < 7 ? 2000 : 2200);
        }
        TrendService.Trend t = service.summarise(dates, prices);

        assertThat(t.weekChangePercent()).isEqualTo(10.0);
        assertThat(t.direction()).isEqualTo("rising");
        assertThat(t.lowest()).isEqualTo(2000);
        assertThat(t.highest()).isEqualTo(2200);
        assertThat(t.points().get(5).movingAvg7()).isNull();
        assertThat(t.points().get(6).movingAvg7()).isEqualTo(2000);
    }
}
