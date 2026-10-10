package com.mandiwatch.service;

import com.mandiwatch.domain.Commodity;
import com.mandiwatch.repository.CommodityDailyAverage;
import com.mandiwatch.repository.CommodityRepository;
import com.mandiwatch.repository.DailyPriceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The "what moved this week" box on the home page. */
@Service
public class MarketSummaryService {

    private final CommodityRepository commodityRepository;
    private final DailyPriceRepository priceRepository;

    public MarketSummaryService(CommodityRepository commodityRepository, DailyPriceRepository priceRepository) {
        this.commodityRepository = commodityRepository;
        this.priceRepository = priceRepository;
    }

    public record Mover(Commodity commodity, int averageNow, int averageWeekAgo, double changePercent) {

        public boolean isUp() {
            return changePercent > 0;
        }
    }

    /**
     * Two queries in total, however many crops there are: all commodities, and all daily
     * averages for the last 10 days. (Before: one averages query per commodity, the N+1 pattern.)
     */
    @Transactional(readOnly = true)
    public List<Mover> weeklyMovers(int limit) {
        LocalDate latest = priceRepository.findLatestDate().orElse(null);
        if (latest == null) {
            return List.of();
        }
        LocalDate weekAgo = latest.minusDays(7);

        Map<Long, Commodity> commodities = commodityRepository.findAll().stream()
                .collect(Collectors.toMap(Commodity::getId, Function.identity()));
        // rows arrive ordered by date, and groupingBy keeps that order inside each list
        Map<Long, List<CommodityDailyAverage>> byCommodity = priceRepository
                .findDailyAveragesForAll(latest.minusDays(10)).stream()
                .collect(Collectors.groupingBy(CommodityDailyAverage::commodityId));

        List<Mover> movers = new ArrayList<>();
        byCommodity.forEach((commodityId, rows) -> {
            Double now = null;
            Double before = null;
            for (CommodityDailyAverage row : rows) {
                if (row.date().equals(latest)) {
                    now = row.average();
                }
                if (!row.date().isAfter(weekAgo)) {
                    before = row.average(); // keeps the closest date on or before a week ago
                }
            }
            if (now != null && before != null && before > 0) {
                double change = Math.round((now - before) / before * 1000) / 10.0;
                movers.add(new Mover(commodities.get(commodityId), (int) Math.round(now), (int) Math.round(before), change));
            }
        });
        movers.sort(Comparator.comparingDouble((Mover m) -> Math.abs(m.changePercent())).reversed());
        return movers.size() > limit ? movers.subList(0, limit) : movers;
    }
}
