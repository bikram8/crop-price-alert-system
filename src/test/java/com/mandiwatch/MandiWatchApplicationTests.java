package com.mandiwatch;

import com.mandiwatch.domain.AlertCondition;
import com.mandiwatch.domain.Farmer;
import com.mandiwatch.domain.ImportRun;
import com.mandiwatch.domain.Language;
import com.mandiwatch.repository.CommodityRepository;
import com.mandiwatch.repository.DailyPriceRepository;
import com.mandiwatch.repository.ImportRunRepository;
import com.mandiwatch.repository.NotificationRepository;
import com.mandiwatch.repository.VillageRepository;
import com.mandiwatch.service.CsvPriceParser;
import com.mandiwatch.service.FarmerService;
import com.mandiwatch.service.PriceImportService;
import com.mandiwatch.service.SampleDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end check of the main story: sample data loads, a farmer sets an alert,
 * a new bulletin is uploaded, and the farmer gets an SMS in Telugu.
 */
@SpringBootTest
@ActiveProfiles("test") // in-memory DB and 20 sample days, from src/test/resources/application-test.yml
class MandiWatchApplicationTests {

    @Autowired SampleDataService sampleData;
    @Autowired CsvPriceParser csvParser;
    @Autowired PriceImportService importService;
    @Autowired FarmerService farmerService;
    @Autowired VillageRepository villages;
    @Autowired CommodityRepository commodities;
    @Autowired DailyPriceRepository prices;
    @Autowired NotificationRepository notifications;
    @Autowired ImportRunRepository runs;

    @Test
    void uploadedBulletinTriggersAlertInFarmersLanguage() throws Exception {
        assertThat(prices.count()).isGreaterThan(500);

        Long chevella = villages.findAllByOrderByNameAsc().stream()
                .filter(v -> v.getName().equals("Chevella")).findFirst().orElseThrow().getId();
        Long tomato = commodities.findByNameIgnoreCase("Tomato").orElseThrow().getId();

        Farmer farmer = farmerService.register("Ramesh", "9876543210", chevella, Language.TELUGU);
        // a very high threshold so nothing fires on the existing data
        farmerService.addAlert(farmer.getId(), tomato, AlertCondition.ABOVE, 50_000, 100);
        long before = notifications.count();

        // demo bulletin pushes tomato up 30%; lower the bar to something it will clear
        farmerService.addAlert(farmer.getId(), tomato, AlertCondition.ABOVE, 100, 100);
        long afterImmediateCheck = notifications.count();
        assertThat(afterImmediateCheck).isEqualTo(before + 1);

        String csv = sampleData.demoBulletinCsv();
        ImportRun run = importService.importRows("test upload",
                csvParser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))));

        assertThat(run.getRowsSkipped()).isZero();
        assertThat(notifications.count()).isEqualTo(afterImmediateCheck + 1);
        assertThat(runs.findAlertsSent(run.getId())).isEqualTo(1);

        String sms = notifications.findAllNewestFirst().get(0).getMessage();
        assertThat(sms).contains("టమాటా").contains("మండీవాచ్");

        // importing the same bulletin again must not text the farmer twice
        importService.importRows("test upload again",
                csvParser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))));
        assertThat(notifications.count()).isEqualTo(afterImmediateCheck + 1);
    }

    @Test
    void badRowsAreSkippedWithReason() throws Exception {
        String csv = "State,District,Market,Commodity,Arrival_Date,Min_Price,Max_Price,Modal_Price\n"
                + "Telangana,Hyderabad,Bowenpally,Onion,01/09/2026,2000,2600,2400\n"
                + "Telangana,Hyderabad,Bowenpally,Onion,not-a-date,2000,2600,2400\n"
                + "Telangana,Hyderabad,Bowenpally,Onion,02/09/2026,2000,2600,9999\n";
        ImportRun run = importService.importRows("bad rows",
                csvParser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))));

        assertThat(run.getRowsSaved()).isEqualTo(1);
        assertThat(run.getRowsSkipped()).isEqualTo(2);
        assertThat(run.getNote()).contains("bad date").contains("outside min-max");
    }

    @Test
    void sameRowTwiceInOneFileIsStoredOnceWithTheLastValue() throws Exception {
        String csv = "State,District,Market,Commodity,Arrival_Date,Min_Price,Max_Price,Modal_Price\n"
                + "Telangana,Hyderabad,Bowenpally,Beetroot,05/09/2026,2000,2600,2400\n"
                + "Telangana,Hyderabad,Bowenpally,Beetroot,05/09/2026,2000,2600,2500\n";
        long before = prices.count();

        importService.importRows("duplicate rows",
                csvParser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))));

        assertThat(prices.count()).isEqualTo(before + 1);
        Long beetroot = commodities.findByNameIgnoreCase("Beetroot").orElseThrow().getId();
        assertThat(prices.findForDay(java.time.LocalDate.of(2026, 9, 5), beetroot, null))
                .singleElement()
                .satisfies(p -> assertThat(p.getModalPrice()).isEqualTo(2500));
    }
}
