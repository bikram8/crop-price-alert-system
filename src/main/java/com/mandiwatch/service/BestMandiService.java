package com.mandiwatch.service;

import com.mandiwatch.config.MandiProperties;
import com.mandiwatch.domain.Commodity;
import com.mandiwatch.domain.DailyPrice;
import com.mandiwatch.domain.Mandi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Answers the farmer's real question: "where will I take home the most money?"
 * The mandi with the highest price is often not the answer once you pay to
 * get your crop there.
 */
@Service
public class BestMandiService {

    private final LatestPrices latestPrices;
    private final MandiProperties.Transport transport;

    public BestMandiService(LatestPrices latestPrices, MandiProperties properties) {
        this.latestPrices = latestPrices;
        this.transport = properties.transport();
    }

    public record Request(Commodity commodity,
                          double fromLat,
                          double fromLon,
                          double quantityQuintals,
                          double ratePerKmPerQuintal,
                          int radiusKm) {
    }

    public record Offer(Mandi mandi,
                        LocalDate priceDate,
                        int modalPrice,
                        int roadKm,
                        long grossAmount,
                        long transportCost,
                        long marketFee,
                        long hamali,
                        long netAmount) {

        public long getNetPerQuintal(double quantity) {
            return Math.round(netAmount / quantity);
        }
    }

    public record Result(List<Offer> offers,
                         Offer best,
                         Offer nearest,
                         long extraOverNearest,
                         int mandisWithoutLocation) {

        public boolean isEmpty() {
            return offers.isEmpty();
        }
    }

    @Transactional(readOnly = true)
    public Result findBest(Request req) {
        List<Offer> offers = new ArrayList<>();
        int withoutLocation = 0;

        for (DailyPrice price : latestPrices.forCommodity(req.commodity())) {
            Mandi mandi = price.getMandi();
            if (!mandi.hasLocation()) {
                withoutLocation++;
                continue;
            }
            double km = GeoUtils.roadKm(req.fromLat(), req.fromLon(),
                    mandi.getLatitude(), mandi.getLongitude(), transport.roadFactor());
            if (km > req.radiusKm()) {
                continue;
            }
            offers.add(calculate(price, km, req.quantityQuintals(), req.ratePerKmPerQuintal()));
        }

        offers.sort(Comparator.comparingLong(Offer::netAmount).reversed());

        Offer best = offers.isEmpty() ? null : offers.get(0);
        Offer nearest = offers.stream().min(Comparator.comparingInt(Offer::roadKm)).orElse(null);
        long extra = (best == null || nearest == null) ? 0 : best.netAmount() - nearest.netAmount();
        return new Result(offers, best, nearest, extra, withoutLocation);
    }

    Offer calculate(DailyPrice price, double roadKm, double quantity, double ratePerKm) {
        long gross = Math.round(price.getModalPrice() * quantity);
        long transportCost = Math.round(roadKm * ratePerKm * quantity);
        long fee = Math.round(gross * price.getMandi().getFeePercent() / 100.0);
        long hamali = Math.round(transport.hamaliPerQuintal() * quantity);
        long net = gross - transportCost - fee - hamali;
        return new Offer(price.getMandi(), price.getPriceDate(), price.getModalPrice(),
                (int) Math.round(roadKm), gross, transportCost, fee, hamali, net);
    }
}
