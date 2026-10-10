package com.mandiwatch.service;

import com.mandiwatch.domain.Commodity;
import com.mandiwatch.domain.DailyPrice;
import com.mandiwatch.domain.ImportRun;
import com.mandiwatch.domain.Mandi;
import com.mandiwatch.repository.CommodityRepository;
import com.mandiwatch.repository.DailyPriceRepository;
import com.mandiwatch.repository.ImportRunRepository;
import com.mandiwatch.repository.MandiRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Cleans raw rows and stores them. All three sources (API, CSV, sample data)
 * end up here, so the validation rules live in exactly one place.
 */
@Service
public class PriceImportService {

    private static final Logger log = LoggerFactory.getLogger(PriceImportService.class);

    /** Anything outside this band is almost certainly a typo in the bulletin (₹/quintal). */
    private static final int MIN_SANE_PRICE = 50;
    private static final int MAX_SANE_PRICE = 200_000;

    private static final int MAX_REASONS_IN_NOTE = 5;

    private final MandiRepository mandiRepository;
    private final CommodityRepository commodityRepository;
    private final DailyPriceRepository priceRepository;
    private final ImportRunRepository importRunRepository;
    private final CommodityNormalizer normalizer;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public PriceImportService(MandiRepository mandiRepository,
                              CommodityRepository commodityRepository,
                              DailyPriceRepository priceRepository,
                              ImportRunRepository importRunRepository,
                              CommodityNormalizer normalizer,
                              ApplicationEventPublisher events,
                              Clock clock) {
        this.mandiRepository = mandiRepository;
        this.commodityRepository = commodityRepository;
        this.priceRepository = priceRepository;
        this.importRunRepository = importRunRepository;
        this.normalizer = normalizer;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public ImportRun importRows(String source, List<RawPriceRow> rows) {
        ImportRun run = new ImportRun(source);

        // Small caches so a 10,000-row file doesn't hit the DB for the same mandi 10,000 times.
        Map<String, Mandi> mandiCache = new HashMap<>();
        Map<String, Commodity> commodityCache = new HashMap<>();

        int saved = 0;
        List<String> reasons = new ArrayList<>();
        TreeSet<LocalDate> dates = new TreeSet<>();

        // Pass 1: validate everything and note which dates the file covers.
        List<RawPriceRow> valid = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            RawPriceRow row = rows.get(i);
            String problem = validate(row);
            if (problem != null) {
                if (reasons.size() < MAX_REASONS_IN_NOTE) {
                    reasons.add("row " + (i + 1) + ": " + problem);
                }
                continue;
            }
            valid.add(row);
            dates.add(row.parseDate());
        }

        // Load every existing price in that date range with ONE query. The old code ran
        // findByMandiAndCommodityAndPriceDate for every row: 10,000 rows = 10,000 SELECTs.
        Map<String, DailyPrice> existing = new HashMap<>();
        if (!dates.isEmpty()) {
            for (DailyPrice p : priceRepository.findAllBetween(dates.first(), dates.last())) {
                existing.put(priceKey(p.getMandi().getId(), p.getCommodity().getId(), p.getPriceDate()), p);
            }
        }

        // Pass 2: upsert. New rows are persisted (and batched, thanks to the sequence id);
        // existing ones are managed entities, so changing them is enough for Hibernate to UPDATE.
        for (RawPriceRow row : valid) {
            Mandi mandi = mandiCache.computeIfAbsent(mandiKey(row), k -> findOrCreateMandi(row));
            String commodityName = normalizer.normalize(row.commodity());
            Commodity commodity = commodityCache.computeIfAbsent(commodityName, this::findOrCreateCommodity);
            LocalDate date = row.parseDate();

            int min = RawPriceRow.parsePrice(row.minPrice());
            int max = RawPriceRow.parsePrice(row.maxPrice());
            int modal = RawPriceRow.parsePrice(row.modalPrice());

            String key = priceKey(mandi.getId(), commodity.getId(), date);
            DailyPrice price = existing.get(key);
            if (price == null) {
                price = new DailyPrice(mandi, commodity, date);
                priceRepository.save(price);
                existing.put(key, price); // the same row twice in one file updates, not duplicates
            }
            price.updatePrices(row.variety(), Math.min(min, max), Math.max(min, max), modal);
            saved++;
        }

        int skipped = rows.size() - saved;
        String note = reasons.isEmpty() ? null : "Skipped " + skipped + " row(s). " + String.join("; ", reasons)
                + (skipped > reasons.size() ? " ..." : "");
        run.finish(rows.size(), saved, skipped, saved > 0 || rows.isEmpty(), note);
        importRunRepository.save(run);

        log.info("Import from {}: read={}, saved={}, skipped={}", source, rows.size(), saved, skipped);

        if (saved > 0) {
            // Alert checking listens for this and runs after the transaction commits.
            events.publishEvent(new PricesImportedEvent(run.getId(), dates.last()));
        }
        return run;
    }

    @Transactional
    public ImportRun recordFailure(String source, String reason) {
        ImportRun run = new ImportRun(source);
        run.finish(0, 0, 0, false, reason);
        return importRunRepository.save(run);
    }

    /** Returns null when the row is usable, otherwise a short human-readable reason. */
    String validate(RawPriceRow row) {
        if (isBlank(row.market()) || isBlank(row.district()) || isBlank(row.state())) {
            return "missing market/district/state";
        }
        if (isBlank(row.commodity())) {
            return "missing commodity";
        }
        if (row.parseDate() == null) {
            return "bad date '" + row.arrivalDate() + "'";
        }
        Integer modal = RawPriceRow.parsePrice(row.modalPrice());
        Integer min = RawPriceRow.parsePrice(row.minPrice());
        Integer max = RawPriceRow.parsePrice(row.maxPrice());
        if (modal == null || min == null || max == null) {
            return "price not a number";
        }
        if (modal < MIN_SANE_PRICE || modal > MAX_SANE_PRICE) {
            return "modal price " + modal + " looks wrong";
        }
        if (modal < Math.min(min, max) || modal > Math.max(min, max)) {
            return "modal price " + modal + " outside min-max range";
        }
        if (row.parseDate().isAfter(LocalDate.now(clock).plusDays(1))) {
            return "date in the future";
        }
        return null;
    }

    private Mandi findOrCreateMandi(RawPriceRow row) {
        String name = tidy(row.market());
        String district = tidy(row.district());
        String state = tidy(row.state());
        Optional<Mandi> existing = mandiRepository
                .findByNameIgnoreCaseAndDistrictIgnoreCaseAndStateIgnoreCase(name, district, state);
        return existing.orElseGet(() -> mandiRepository.save(new Mandi(name, district, state)));
    }

    private Commodity findOrCreateCommodity(String name) {
        return commodityRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> commodityRepository.save(new Commodity(name, null)));
    }

    private static String priceKey(Long mandiId, Long commodityId, LocalDate date) {
        return mandiId + "|" + commodityId + "|" + date;
    }

    private static String mandiKey(RawPriceRow row) {
        return (tidy(row.market()) + "|" + tidy(row.district()) + "|" + tidy(row.state())).toLowerCase();
    }

    private static String tidy(String s) {
        return s.trim().replaceAll("\\s+", " ");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
