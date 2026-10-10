package com.mandiwatch.service;

import com.mandiwatch.domain.AlertCondition;
import com.mandiwatch.domain.Commodity;
import com.mandiwatch.domain.Farmer;
import com.mandiwatch.domain.Language;
import com.mandiwatch.domain.PriceAlert;
import com.mandiwatch.domain.Village;
import com.mandiwatch.repository.CommodityRepository;
import com.mandiwatch.repository.FarmerRepository;
import com.mandiwatch.repository.PriceAlertRepository;
import com.mandiwatch.repository.VillageRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FarmerService {

    private final FarmerRepository farmerRepository;
    private final VillageRepository villageRepository;
    private final CommodityRepository commodityRepository;
    private final PriceAlertRepository alertRepository;
    private final ApplicationEventPublisher events;

    public FarmerService(FarmerRepository farmerRepository,
                         VillageRepository villageRepository,
                         CommodityRepository commodityRepository,
                         PriceAlertRepository alertRepository,
                         ApplicationEventPublisher events) {
        this.farmerRepository = farmerRepository;
        this.villageRepository = villageRepository;
        this.commodityRepository = commodityRepository;
        this.alertRepository = alertRepository;
        this.events = events;
    }

    /**
     * existsByPhone gives a friendly early answer, but on its own it's check-then-act:
     * two requests with the same number can both pass it. The unique constraint on
     * farmer.phone is the real guard; saveAndFlush makes its violation surface here,
     * where we can turn it into a meaningful exception, rather than at commit time.
     */
    @Transactional
    public Farmer register(String name, String phone, Long villageId, Language language) {
        if (farmerRepository.existsByPhone(phone)) {
            throw new DuplicatePhoneException(phone);
        }
        Village village = villageRepository.findById(villageId)
                .orElseThrow(() -> new NotFoundException("Village " + villageId));
        try {
            return farmerRepository.saveAndFlush(new Farmer(name.trim(), phone, village, language));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatePhoneException(phone);
        }
    }

    /**
     * Saves the alert. The event is delivered after this transaction commits, and
     * AlertService then checks just this alert against today's prices. That way the SMS
     * can never go out for an alert whose save was rolled back.
     *
     * @return id of the new alert
     */
    @Transactional
    public Long addAlert(Long farmerId, Long commodityId, AlertCondition condition, int threshold, int radiusKm) {
        Farmer farmer = farmerRepository.findById(farmerId)
                .orElseThrow(() -> new NotFoundException("Farmer " + farmerId));
        Commodity commodity = commodityRepository.findById(commodityId)
                .orElseThrow(() -> new NotFoundException("Commodity " + commodityId));
        PriceAlert alert = new PriceAlert(farmer, commodity, condition, threshold, radiusKm);
        farmer.getAlerts().add(alert);
        alertRepository.save(alert);
        events.publishEvent(new AlertActivatedEvent(alert.getId()));
        return alert.getId();
    }

    /** True once an SMS has gone out for this alert (used for the "SMS sent" message after saving). */
    @Transactional(readOnly = true)
    public boolean hasFired(Long alertId) {
        return alertRepository.findById(alertId).map(a -> a.getLastTriggeredFor() != null).orElse(false);
    }

    @Transactional
    public void toggleAlert(Long farmerId, Long alertId) {
        PriceAlert alert = findOwnedAlert(farmerId, alertId);
        alert.setActive(!alert.isActive());
        if (alert.isActive()) {
            // resumed: today's prices may already match
            events.publishEvent(new AlertActivatedEvent(alert.getId()));
        }
    }

    @Transactional
    public void deleteAlert(Long farmerId, Long alertId) {
        PriceAlert alert = findOwnedAlert(farmerId, alertId);
        alert.getFarmer().getAlerts().remove(alert);
    }

    /** Farmer with village, alerts and their crops already loaded, ready for the detail page. */
    @Transactional(readOnly = true)
    public Farmer getFarmer(Long id) {
        return farmerRepository.findWithAlertsById(id).orElseThrow(() -> new NotFoundException("Farmer " + id));
    }

    @Transactional(readOnly = true)
    public List<Farmer> listFarmers() {
        return farmerRepository.findAllByOrderByRegisteredAtDesc();
    }

    private PriceAlert findOwnedAlert(Long farmerId, Long alertId) {
        PriceAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new NotFoundException("Alert " + alertId));
        if (!alert.getFarmer().getId().equals(farmerId)) {
            throw new NotFoundException("Alert " + alertId);
        }
        return alert;
    }
}
