package com.mandiwatch.service;

import com.mandiwatch.config.MandiProperties;
import com.mandiwatch.domain.AlertCondition;
import com.mandiwatch.domain.DailyPrice;
import com.mandiwatch.domain.Farmer;
import com.mandiwatch.domain.Language;
import com.mandiwatch.domain.Notification;
import com.mandiwatch.domain.PriceAlert;
import com.mandiwatch.domain.Village;
import com.mandiwatch.repository.ImportRunRepository;
import com.mandiwatch.repository.NotificationRepository;
import com.mandiwatch.repository.PriceAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Texts farmers whose conditions are met.
 * <p>
 * Two triggers, both handled only after the triggering transaction has committed:
 * <ul>
 *   <li>{@link PricesImportedEvent}: a new bulletin was saved, check every active alert;</li>
 *   <li>{@link AlertActivatedEvent}: one alert was created or resumed, check just that one.</li>
 * </ul>
 * Each alert that fires is handled in its own small transaction (see {@link #claimAndNotify}).
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);
    private static final DateTimeFormatter SMS_DATE = DateTimeFormatter.ofPattern("dd-MM");

    private final PriceAlertRepository alertRepository;
    private final NotificationRepository notificationRepository;
    private final ImportRunRepository importRunRepository;
    private final LatestPrices latestPrices;
    private final SmsSender smsSender;
    private final MessageSource messages;
    private final TransactionTemplate newTransaction;
    private final double roadFactor;

    public AlertService(PriceAlertRepository alertRepository,
                        NotificationRepository notificationRepository,
                        ImportRunRepository importRunRepository,
                        LatestPrices latestPrices,
                        SmsSender smsSender,
                        MessageSource messages,
                        PlatformTransactionManager transactionManager,
                        MandiProperties properties) {
        this.alertRepository = alertRepository;
        this.notificationRepository = notificationRepository;
        this.importRunRepository = importRunRepository;
        this.latestPrices = latestPrices;
        this.smsSender = smsSender;
        this.messages = messages;
        this.roadFactor = properties.transport().roadFactor();

        // REQUIRES_NEW, not the default REQUIRED: these listeners run in the AFTER_COMMIT phase,
        // where the old transaction's resources are still bound to the thread. REQUIRED would
        // quietly join that finished transaction and nothing we write would ever be committed.
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** AFTER_COMMIT (the default phase): only look at prices once they are really saved. */
    @TransactionalEventListener
    public void onPricesImported(PricesImportedEvent event) {
        int sent = checkAlerts(alertRepository.findActiveWithFarmer());
        if (event.importRunId() != null) {
            newTransaction.executeWithoutResult(status -> importRunRepository.findById(event.importRunId())
                    .ifPresent(run -> run.setAlertsSent(sent)));
        }
    }

    /**
     * A farmer just added or resumed an alert. Only that alert is checked. The old code ran
     * the check for every farmer's alerts here, inside the farmer's own transaction.
     */
    @TransactionalEventListener
    public void onAlertActivated(AlertActivatedEvent event) {
        alertRepository.findWithFarmerById(event.alertId())
                .filter(PriceAlert::isActive)
                .ifPresent(alert -> checkAlerts(List.of(alert)));
    }

    /**
     * Deliberately not @Transactional and not public: it's only called from inside this class,
     * and a call through "this" never goes through Spring's proxy, so an annotation here would
     * be silently ignored (the self-invocation trap). Transactions are opened explicitly per alert.
     *
     * @return number of SMS sent
     */
    int checkAlerts(List<PriceAlert> alerts) {
        // several farmers often watch the same crop, so look its prices up once
        Map<Long, List<DailyPrice>> pricesByCommodity = new HashMap<>();
        int sent = 0;

        for (PriceAlert alert : alerts) {
            List<DailyPrice> prices = pricesByCommodity.computeIfAbsent(
                    alert.getCommodity().getId(), id -> latestPrices.forCommodity(alert.getCommodity()));
            Optional<Match> match = findMatch(alert, prices);
            if (match.isEmpty() || match.get().price().getPriceDate().equals(alert.getLastTriggeredFor())) {
                continue; // nothing matches, or we already told them about this bulletin
            }
            try {
                Boolean notified = newTransaction.execute(status -> claimAndNotify(alert.getId(), match.get()));
                if (Boolean.TRUE.equals(notified)) {
                    sent++;
                }
            } catch (ConcurrencyFailureException e) {
                // Another import updated this alert at the same moment (@Version mismatch or row lock).
                // That one sends the SMS; our transaction rolled back before sending anything.
                log.info("Alert {} is being handled by a concurrent import, skipping", alert.getId());
            }
        }
        if (sent > 0) {
            log.info("Sent {} price alert(s)", sent);
        }
        return sent;
    }

    /**
     * Runs inside its own transaction. Order matters:
     * <ol>
     *   <li>re-read the alert in this transaction (the one passed around may be stale);</li>
     *   <li>mark it as triggered and flush, so the versioned UPDATE runs right now;
     *       if another thread got there first this throws and no SMS goes out;</li>
     *   <li>only then send the SMS and log it.</li>
     * </ol>
     */
    private boolean claimAndNotify(Long alertId, Match match) {
        PriceAlert alert = alertRepository.findById(alertId).orElse(null);
        LocalDate priceDate = match.price().getPriceDate();
        if (alert == null || !alert.isActive() || priceDate.equals(alert.getLastTriggeredFor())) {
            return false;
        }
        alert.setLastTriggeredFor(priceDate);
        alertRepository.flush();
        notify(alert, match);
        return true;
    }

    record Match(DailyPrice price, int roadKm) {
    }

    /**
     * Among mandis within the farmer's radius that meet the condition, pick the one
     * that matters most: the highest price for an ABOVE alert, the lowest for BELOW.
     */
    Optional<Match> findMatch(PriceAlert alert, List<DailyPrice> prices) {
        Village home = alert.getFarmer().getVillage();
        Comparator<Match> byPrice = Comparator.comparingInt(m -> m.price().getModalPrice());

        var candidates = prices.stream()
                .filter(p -> p.getMandi().hasLocation())
                .map(p -> new Match(p, (int) Math.round(GeoUtils.roadKm(home.getLatitude(), home.getLongitude(),
                        p.getMandi().getLatitude(), p.getMandi().getLongitude(), roadFactor))))
                .filter(m -> m.roadKm() <= alert.getRadiusKm())
                .filter(m -> alert.getCondition().isMet(m.price().getModalPrice(), alert.getThreshold()));

        return alert.getCondition() == AlertCondition.ABOVE ? candidates.max(byPrice) : candidates.min(byPrice);
    }

    private void notify(PriceAlert alert, Match match) {
        Farmer farmer = alert.getFarmer();
        String text = buildMessage(alert, match);
        boolean ok = smsSender.send(farmer.getPhone(), text);
        notificationRepository.save(new Notification(farmer, text, smsSender.channelName(), ok));
    }

    String buildMessage(PriceAlert alert, Match match) {
        Language lang = alert.getFarmer().getLanguage();
        Locale locale = switch (lang) {
            case TELUGU -> Locale.of("te");
            case HINDI -> Locale.of("hi");
            default -> Locale.ENGLISH;
        };
        String crop = lang == Language.TELUGU && alert.getCommodity().getLocalName() != null
                ? alert.getCommodity().getLocalName()
                : alert.getCommodity().getName();
        DailyPrice p = match.price();
        String key = alert.getCondition() == AlertCondition.ABOVE ? "sms.alert.above" : "sms.alert.below";
        Object[] args = {
                crop,
                String.valueOf(p.getModalPrice()),
                p.getMandi().getName(),
                String.valueOf(match.roadKm()),
                p.getPriceDate().format(SMS_DATE),
                String.valueOf(alert.getThreshold())
        };
        return messages.getMessage(key, args, locale);
    }
}
