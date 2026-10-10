package com.mandiwatch.service;

import com.mandiwatch.config.MandiProperties;
import com.mandiwatch.domain.Commodity;
import com.mandiwatch.domain.Mandi;
import com.mandiwatch.domain.Village;
import com.mandiwatch.repository.CommodityRepository;
import com.mandiwatch.repository.DailyPriceRepository;
import com.mandiwatch.repository.MandiRepository;
import com.mandiwatch.repository.VillageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Seeds Telangana mandis, villages and two months of price history the first
 * time the app starts, so every page has something to show without internet.
 *
 * The prices are generated, not real: each crop has a base price, a monthly
 * swing and some daily noise, and each mandi sits a few percent above or below
 * the state average. That's enough to make the best-mandi and trend screens
 * behave like they would on live data.
 */
@Service
public class SampleDataService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDataService.class);
    private static final DateTimeFormatter BULLETIN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String STATE = "Telangana";

    private final MandiRepository mandiRepository;
    private final CommodityRepository commodityRepository;
    private final VillageRepository villageRepository;
    private final DailyPriceRepository priceRepository;
    private final PriceImportService importService;
    private final MandiProperties.Sample config;
    private final Clock clock;

    public SampleDataService(MandiRepository mandiRepository,
                             CommodityRepository commodityRepository,
                             VillageRepository villageRepository,
                             DailyPriceRepository priceRepository,
                             PriceImportService importService,
                             MandiProperties properties,
                             Clock clock) {
        this.mandiRepository = mandiRepository;
        this.commodityRepository = commodityRepository;
        this.villageRepository = villageRepository;
        this.priceRepository = priceRepository;
        this.importService = importService;
        this.config = properties.sample();
        this.clock = clock;
    }

    /** base ₹/qtl, how much it swings over a month (fraction), slow drift per day (fraction) */
    private record CropProfile(String name, String telugu, int basePrice, double swing, double drift) {
    }

    private static final List<CropProfile> CROPS = List.of(
            new CropProfile("Tomato", "టమాటా", 1800, 0.25, 0.004),
            new CropProfile("Onion", "ఉల్లిపాయ", 2400, 0.12, 0.002),
            new CropProfile("Potato", "బంగాళాదుంప", 2200, 0.06, 0.0),
            new CropProfile("Green Chilli", "పచ్చిమిర్చి", 3500, 0.18, -0.002),
            new CropProfile("Brinjal", "వంకాయ", 2000, 0.15, 0.001),
            new CropProfile("Cotton", "పత్తి", 7200, 0.04, 0.0005),
            new CropProfile("Paddy", "వరి", 2300, 0.02, 0.0),
            new CropProfile("Maize", "మొక్కజొన్న", 2100, 0.05, -0.001),
            new CropProfile("Turmeric", "పసుపు", 13000, 0.06, 0.001),
            new CropProfile("Dry Chillies", "ఎండు మిర్చి", 16000, 0.07, -0.0005));

    private static final List<String> VEG = List.of("Tomato", "Onion", "Potato", "Green Chilli", "Brinjal");

    /** Typed seed rows instead of Object[][]: no casts, and the compiler checks every field. */
    private record MandiSeed(String name, String district, double lat, double lon, double feePercent, List<String> crops) {
    }

    private record VillageSeed(String name, String district, double lat, double lon) {
    }

    private static List<String> veg(String... extra) {
        List<String> all = new ArrayList<>(VEG);
        all.addAll(List.of(extra));
        return List.copyOf(all);
    }

    private static final List<MandiSeed> MANDIS = List.of(
            new MandiSeed("Bowenpally", "Hyderabad", 17.4710, 78.4790, 1.0, VEG),
            new MandiSeed("Gudimalkapur", "Hyderabad", 17.3780, 78.4300, 1.0, VEG),
            new MandiSeed("Kothapet", "Rangareddy", 17.3690, 78.5400, 1.0, VEG),
            new MandiSeed("Warangal", "Warangal", 17.9880, 79.5600, 1.0, veg("Cotton", "Maize", "Turmeric", "Dry Chillies")),
            new MandiSeed("Nizamabad", "Nizamabad", 18.6725, 78.0941, 1.0, List.of("Tomato", "Onion", "Paddy", "Maize", "Turmeric")),
            new MandiSeed("Khammam", "Khammam", 17.2473, 80.1514, 1.0, List.of("Tomato", "Green Chilli", "Cotton", "Dry Chillies", "Paddy")),
            new MandiSeed("Karimnagar", "Karimnagar", 18.4386, 79.1288, 1.0, List.of("Tomato", "Onion", "Brinjal", "Paddy", "Maize")),
            new MandiSeed("Mahabubnagar", "Mahabubnagar", 16.7488, 78.0035, 1.0, List.of("Tomato", "Onion", "Green Chilli", "Paddy", "Cotton")),
            new MandiSeed("Siddipet", "Siddipet", 18.1018, 78.8520, 1.0, List.of("Tomato", "Brinjal", "Maize", "Cotton")),
            new MandiSeed("Nalgonda", "Nalgonda", 17.0575, 79.2671, 1.0, List.of("Tomato", "Onion", "Paddy", "Cotton")),
            new MandiSeed("Suryapet", "Suryapet", 17.1405, 79.6236, 1.0, List.of("Paddy", "Cotton", "Dry Chillies", "Tomato")),
            new MandiSeed("Sangareddy", "Sangareddy", 17.6140, 78.0816, 1.0, veg("Maize")),
            new MandiSeed("Vikarabad", "Vikarabad", 17.3381, 77.9044, 1.0, List.of("Tomato", "Green Chilli", "Brinjal", "Maize")),
            new MandiSeed("Shadnagar", "Rangareddy", 17.0717, 78.2050, 1.0, List.of("Tomato", "Onion", "Green Chilli", "Maize")));

    private static final List<VillageSeed> VILLAGES = List.of(
            new VillageSeed("Chevella", "Rangareddy", 17.3067, 78.1356),
            new VillageSeed("Moinabad", "Rangareddy", 17.3260, 78.2600),
            new VillageSeed("Jangaon", "Jangaon", 17.7236, 79.1520),
            new VillageSeed("Medak", "Medak", 18.0466, 78.2635),
            new VillageSeed("Bhongir", "Yadadri Bhuvanagiri", 17.5110, 78.8889),
            new VillageSeed("Kamareddy", "Kamareddy", 18.3205, 78.3373),
            new VillageSeed("Zaheerabad", "Sangareddy", 17.6814, 77.6071),
            new VillageSeed("Miryalaguda", "Nalgonda", 16.8722, 79.5625),
            new VillageSeed("Jagtial", "Jagtial", 18.7948, 78.9135),
            new VillageSeed("Wanaparthy", "Wanaparthy", 16.3624, 78.0622),
            new VillageSeed("Kodad", "Suryapet", 16.9978, 79.9653),
            new VillageSeed("Gajwel", "Siddipet", 17.8517, 78.6828));

    @Override
    public void run(ApplicationArguments args) {
        if (!config.enabled() || commodityRepository.count() > 0) {
            return;
        }
        log.info("Empty database, loading sample Telangana data ({} days)", config.days());
        seedReferenceData();
        LocalDate end = LocalDate.now(clock).minusDays(1);
        importService.importRows("Sample data (generated)", generateRows(end.minusDays(config.days() - 1L), end, 0.0));
    }

    private void seedReferenceData() {
        for (CropProfile crop : CROPS) {
            commodityRepository.save(new Commodity(crop.name(), crop.telugu()));
        }
        for (MandiSeed m : MANDIS) {
            mandiRepository.save(new Mandi(m.name(), m.district(), STATE, m.lat(), m.lon(), m.feePercent()));
        }
        for (VillageSeed v : VILLAGES) {
            villageRepository.save(new Village(v.name(), v.district(), v.lat(), v.lon()));
        }
    }

    /**
     * Generates bulletin rows for a date range.
     *
     * @param tomatoBoost extra fraction added to tomato prices, used by the demo
     *                    bulletin so that a fresh upload trips the tomato alerts
     */
    List<RawPriceRow> generateRows(LocalDate from, LocalDate to, double tomatoBoost) {
        Map<String, CropProfile> crops = new LinkedHashMap<>();
        CROPS.forEach(c -> crops.put(c.name(), c));
        LocalDate origin = LocalDate.of(2026, 1, 1); // fixed origin keeps the curves the same for a given date

        List<RawPriceRow> rows = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            long t = day.toEpochDay() - origin.toEpochDay();
            for (MandiSeed m : MANDIS) {
                String mandi = m.name();
                // floorMod, not Math.abs(hash) % 17: Math.abs(Integer.MIN_VALUE) is still negative
                double mandiFactor = 0.92 + Math.floorMod(mandi.hashCode(), 17) / 100.0; // 0.92 .. 1.08
                for (String cropName : m.crops()) {
                    CropProfile crop = crops.get(cropName);
                    Random noise = new Random(day.toEpochDay() * 31 + mandi.hashCode() * 17L + cropName.hashCode());
                    // markets are closed some days; skip about 1 in 12 rows so the data has gaps like the real feed
                    if (noise.nextInt(12) == 0 && tomatoBoost == 0.0) {
                        continue;
                    }
                    double phase = (cropName.hashCode() % 30) / 30.0 * 2 * Math.PI;
                    double seasonal = 1 + crop.swing() * Math.sin(2 * Math.PI * t / 30.0 + phase);
                    double trend = 1 + crop.drift() * 60 * Math.sin(2 * Math.PI * t / 240.0);
                    double daily = 1 + (noise.nextDouble() - 0.5) * 0.06;
                    double boost = cropName.equals("Tomato") ? 1 + tomatoBoost : 1.0;

                    int modal = roundTo10(crop.basePrice() * mandiFactor * seasonal * trend * daily * boost);
                    int min = roundTo10(modal * (0.82 + noise.nextDouble() * 0.06));
                    int max = roundTo10(modal * (1.08 + noise.nextDouble() * 0.06));

                    rows.add(new RawPriceRow(STATE, m.district(), mandi, cropName, "Other",
                            day.format(BULLETIN_DATE), String.valueOf(min), String.valueOf(max), String.valueOf(modal)));
                }
            }
        }
        return rows;
    }

    /**
     * A ready-made "next day" bulletin as CSV, in the same column layout as the
     * data.gov.in download. Tomato is pushed up ~30% so an upload visibly sets off alerts.
     */
    public String demoBulletinCsv() {
        LocalDate latest = priceRepository.findLatestDate().orElse(LocalDate.now(clock).minusDays(1));
        LocalDate next = latest.plusDays(1);
        StringBuilder csv = new StringBuilder("State,District,Market,Commodity,Variety,Arrival_Date,Min_Price,Max_Price,Modal_Price\n");
        for (RawPriceRow r : generateRows(next, next, 0.30)) {
            csv.append(String.join(",", r.state(), r.district(), r.market(), r.commodity(), r.variety(),
                    r.arrivalDate(), r.minPrice(), r.maxPrice(), r.modalPrice())).append('\n');
        }
        return csv.toString();
    }

    private static int roundTo10(double v) {
        return (int) (Math.round(v / 10.0) * 10);
    }
}
