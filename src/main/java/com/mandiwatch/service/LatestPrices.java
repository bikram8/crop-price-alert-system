package com.mandiwatch.service;

import com.mandiwatch.config.MandiProperties;
import com.mandiwatch.domain.Commodity;
import com.mandiwatch.domain.DailyPrice;
import com.mandiwatch.repository.DailyPriceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Not every mandi reports every crop every day. To compare mandis fairly we take
 * the newest price each mandi reported within the last few days of the latest bulletin.
 */
@Component
public class LatestPrices {

    private final DailyPriceRepository priceRepository;
    private final int freshnessDays;

    public LatestPrices(DailyPriceRepository priceRepository, MandiProperties properties) {
        this.priceRepository = priceRepository;
        this.freshnessDays = properties.transport().freshnessDays();
    }

    @Transactional(readOnly = true)
    public List<DailyPrice> forCommodity(Commodity commodity) {
        LocalDate latest = priceRepository.findLatestDate().orElse(null);
        if (latest == null) {
            return List.of();
        }
        List<DailyPrice> rows = priceRepository.findForCommodityBetween(
                commodity, latest.minusDays(freshnessDays), latest);

        // rows come newest first, so the first one we see per mandi wins
        Map<Long, DailyPrice> newestPerMandi = new LinkedHashMap<>();
        for (DailyPrice p : rows) {
            newestPerMandi.putIfAbsent(p.getMandi().getId(), p);
        }
        return new ArrayList<>(newestPerMandi.values());
    }
}
