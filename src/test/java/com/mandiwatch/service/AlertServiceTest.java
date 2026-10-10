package com.mandiwatch.service;

import com.mandiwatch.config.MandiProperties;
import com.mandiwatch.domain.AlertCondition;
import com.mandiwatch.domain.Commodity;
import com.mandiwatch.domain.DailyPrice;
import com.mandiwatch.domain.Farmer;
import com.mandiwatch.domain.Language;
import com.mandiwatch.domain.Mandi;
import com.mandiwatch.domain.PriceAlert;
import com.mandiwatch.domain.Village;
import com.mandiwatch.repository.ImportRunRepository;
import com.mandiwatch.repository.NotificationRepository;
import com.mandiwatch.repository.PriceAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Plain unit test: no Spring context, every collaborator is a Mockito mock.
 * Runs in milliseconds and pins down the alert rules exactly.
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 3);

    @Mock PriceAlertRepository alertRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock ImportRunRepository importRunRepository;
    @Mock LatestPrices latestPrices;
    @Mock SmsSender smsSender;
    @Mock MessageSource messages;
    @Mock PlatformTransactionManager transactionManager; // TransactionTemplate just calls our callback

    private AlertService service;

    private final Commodity tomato = new Commodity("Tomato", "టమాటా");
    private final Village chevella = new Village("Chevella", "Rangareddy", 17.3067, 78.1356);
    private final Farmer farmer = new Farmer("Ramesh", "9876543210", chevella, Language.TELUGU);

    // ~1 km, ~52 km and ~200 km (road) from Chevella, plus one mandi with no location
    private final DailyPrice nearCheap = price(new Mandi("Near", "D", "S", 17.31, 78.14, 1.0), 2000);
    private final DailyPrice midDear = price(new Mandi("Bowenpally", "D", "S", 17.4710, 78.4790, 1.0), 2600);
    private final DailyPrice farDearest = price(new Mandi("Warangal", "D", "S", 17.9880, 79.5600, 1.0), 3000);
    private final DailyPrice noLocation = price(new Mandi("Unknown", "D", "S"), 9000);
    private final List<DailyPrice> prices = List.of(nearCheap, midDear, farDearest, noLocation);

    @BeforeEach
    void setUp() {
        MandiProperties props = new MandiProperties(null, new MandiProperties.Transport(1.0, 1.3, 30, 3), null, null);
        service = new AlertService(alertRepository, notificationRepository, importRunRepository,
                latestPrices, smsSender, messages, transactionManager, props);
    }

    @Test
    void aboveAlertPicksHighestPriceInsideRadius() {
        PriceAlert alert = new PriceAlert(farmer, tomato, AlertCondition.ABOVE, 2500, 100);

        Optional<AlertService.Match> match = service.findMatch(alert, prices);

        // Warangal pays more but is outside 100 km; the mandi without a location is ignored
        assertThat(match).get().extracting(m -> m.price().getMandi().getName()).isEqualTo("Bowenpally");
    }

    @Test
    void belowAlertPicksLowestPrice() {
        PriceAlert alert = new PriceAlert(farmer, tomato, AlertCondition.BELOW, 2100, 100);

        assertThat(service.findMatch(alert, prices))
                .get().extracting(m -> m.price().getMandi().getName()).isEqualTo("Near");
    }

    @Test
    void noMatchWhenThresholdNotReached() {
        PriceAlert alert = new PriceAlert(farmer, tomato, AlertCondition.ABOVE, 2700, 100);

        assertThat(service.findMatch(alert, prices)).isEmpty();
    }

    @Test
    void sameBulletinIsNotSentTwice() {
        PriceAlert alert = new PriceAlert(farmer, tomato, AlertCondition.ABOVE, 2500, 100);
        alert.setLastTriggeredFor(TODAY);
        when(latestPrices.forCommodity(tomato)).thenReturn(prices);

        assertThat(service.checkAlerts(List.of(alert))).isZero();
        verifyNoInteractions(smsSender);
    }

    @Test
    void alertIsMarkedAndFlushedBeforeTheSmsGoesOut() {
        PriceAlert alert = new PriceAlert(farmer, tomato, AlertCondition.ABOVE, 2500, 100);
        when(latestPrices.forCommodity(tomato)).thenReturn(prices);
        when(alertRepository.findById(any())).thenReturn(Optional.of(alert));
        when(smsSender.send(anyString(), any())).thenReturn(true);

        int sent = service.checkAlerts(List.of(alert));

        assertThat(sent).isEqualTo(1);
        assertThat(alert.getLastTriggeredFor()).isEqualTo(TODAY);
        InOrder order = inOrder(alertRepository, smsSender);
        order.verify(alertRepository).flush();          // claim the alert (versioned UPDATE)...
        order.verify(smsSender).send(anyString(), any()); // ...then text the farmer
        verify(notificationRepository).save(any());
    }

    @Test
    void concurrentUpdateMeansNoSms() {
        PriceAlert alert = new PriceAlert(farmer, tomato, AlertCondition.ABOVE, 2500, 100);
        when(latestPrices.forCommodity(tomato)).thenReturn(prices);
        when(alertRepository.findById(any())).thenReturn(Optional.of(alert));
        // what Spring Data throws when the @Version check fails during flush
        doThrow(new ObjectOptimisticLockingFailureException(PriceAlert.class, 1L)).when(alertRepository).flush();

        assertThat(service.checkAlerts(List.of(alert))).isZero();
        verify(smsSender, never()).send(anyString(), any());
    }

    private DailyPrice price(Mandi mandi, int modal) {
        DailyPrice p = new DailyPrice(mandi, tomato, TODAY);
        p.updatePrices("Local", modal - 200, modal + 200, modal);
        return p;
    }
}
