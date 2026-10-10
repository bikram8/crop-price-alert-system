package com.mandiwatch.service;

import com.mandiwatch.domain.Commodity;
import com.mandiwatch.domain.DailyPrice;
import com.mandiwatch.domain.Mandi;
import com.mandiwatch.repository.DailyAverage;
import com.mandiwatch.repository.DailyPriceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Price history plus a few numbers a farmer can act on: is it going up or
 * down, and roughly where might it be next week.
 */
@Service
public class TrendService {

    private final DailyPriceRepository priceRepository;
    private final Clock clock;

    public TrendService(DailyPriceRepository priceRepository, Clock clock) {
        this.priceRepository = priceRepository;
        this.clock = clock;
    }

    public record Point(LocalDate date, int price, Integer movingAvg7) {
    }

    public record Trend(List<Point> points,
                        Integer latest,
                        Integer lowest,
                        Integer highest,
                        Double weekChangePercent,
                        String direction,
                        Integer projectedIn7Days) {

        public boolean hasData() {
            return !points.isEmpty();
        }
    }

    /**
     * @param mandi null means "average across all mandis"
     */
    @Transactional(readOnly = true)
    public Trend trend(Commodity commodity, Mandi mandi, int days) {
        LocalDate latest = priceRepository.findLatestDate().orElse(LocalDate.now(clock));
        LocalDate from = latest.minusDays(days - 1L);

        List<LocalDate> dates = new ArrayList<>();
        List<Integer> prices = new ArrayList<>();
        if (mandi == null) {
            for (DailyAverage row : priceRepository.findDailyAverage(commodity, from)) {
                dates.add(row.date());
                prices.add((int) Math.round(row.average()));
            }
        } else {
            for (DailyPrice p : priceRepository.findHistory(commodity, mandi, from)) {
                dates.add(p.getPriceDate());
                prices.add(p.getModalPrice());
            }
        }
        return summarise(dates, prices);
    }

    Trend summarise(List<LocalDate> dates, List<Integer> prices) {
        if (prices.isEmpty()) {
            return new Trend(List.of(), null, null, null, null, "no data", null);
        }

        List<Point> points = new ArrayList<>();
        for (int i = 0; i < prices.size(); i++) {
            points.add(new Point(dates.get(i), prices.get(i), movingAverage(prices, i, 7)));
        }

        int latest = prices.get(prices.size() - 1);
        int lowest = prices.stream().mapToInt(Integer::intValue).min().orElse(latest);
        int highest = prices.stream().mapToInt(Integer::intValue).max().orElse(latest);

        Double weekChange = null;
        if (prices.size() >= 14) {
            double thisWeek = average(prices.subList(prices.size() - 7, prices.size()));
            double lastWeek = average(prices.subList(prices.size() - 14, prices.size() - 7));
            weekChange = Math.round((thisWeek - lastWeek) / lastWeek * 1000) / 10.0;
        }

        String direction;
        if (weekChange == null) {
            direction = "not enough data";
        } else if (weekChange > 3) {
            direction = "rising";
        } else if (weekChange < -3) {
            direction = "falling";
        } else {
            direction = "steady";
        }

        Integer projected = projectAhead(prices, 14, 7);
        return new Trend(points, latest, lowest, highest, weekChange, direction, projected);
    }

    private static Integer movingAverage(List<Integer> prices, int index, int window) {
        if (index + 1 < window) {
            return null;
        }
        return (int) Math.round(average(prices.subList(index + 1 - window, index + 1)));
    }

    private static double average(List<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    /**
     * Fits a straight line (least squares) through the last {@code lookback} prices
     * and extends it {@code ahead} days. Deliberately simple: it only says which way
     * things are heading, it is not a real forecast.
     */
    static Integer projectAhead(List<Integer> prices, int lookback, int ahead) {
        if (prices.size() < lookback) {
            return null;
        }
        List<Integer> recent = prices.subList(prices.size() - lookback, prices.size());
        int n = recent.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (int x = 0; x < n; x++) {
            double y = recent.get(x);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += (double) x * x;
        }
        double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;
        double projected = intercept + slope * (n - 1 + ahead);
        return (int) Math.max(0, Math.round(projected));
    }
}
