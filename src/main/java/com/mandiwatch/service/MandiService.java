package com.mandiwatch.service;

import com.mandiwatch.domain.Mandi;
import com.mandiwatch.repository.MandiRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Mandi editing used to live in MandiController with @Transactional on the controller
 * method. Transactions belong to the service layer: the controller handles HTTP, the
 * service owns the unit of work and can be reused (and tested) without a web request.
 */
@Service
public class MandiService {

    private final MandiRepository mandiRepository;

    public MandiService(MandiRepository mandiRepository) {
        this.mandiRepository = mandiRepository;
    }

    @Transactional(readOnly = true)
    public List<Mandi> listAll() {
        return mandiRepository.findAllByOrderByStateAscDistrictAscNameAsc();
    }

    /** Values are already validated by MandiForm. No save() call: the entity is managed, dirty checking writes it. */
    @Transactional
    public Mandi updateLocation(Long id, Double latitude, Double longitude, double feePercent) {
        Mandi mandi = mandiRepository.findById(id).orElseThrow(() -> new NotFoundException("Mandi " + id));
        mandi.setLatitude(latitude);
        mandi.setLongitude(longitude);
        mandi.setFeePercent(feePercent);
        return mandi;
    }
}
