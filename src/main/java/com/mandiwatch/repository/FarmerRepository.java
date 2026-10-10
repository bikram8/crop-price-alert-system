package com.mandiwatch.repository;

import com.mandiwatch.domain.Farmer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FarmerRepository extends JpaRepository<Farmer, Long> {

    boolean existsByPhone(String phone);

    /**
     * The list page shows each farmer's village and alert count. Without the entity graph
     * that's 1 query for farmers + 1 per farmer for alerts (the N+1 problem).
     */
    @EntityGraph(attributePaths = {"village", "alerts"})
    List<Farmer> findAllByOrderByRegisteredAtDesc();

    /** Everything the farmer detail page touches, loaded in one go (open-in-view is off). */
    @EntityGraph(attributePaths = {"village", "alerts", "alerts.commodity"})
    Optional<Farmer> findWithAlertsById(Long id);
}
