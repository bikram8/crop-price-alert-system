package com.mandiwatch.repository;

import com.mandiwatch.domain.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    @Query("select a from PriceAlert a join fetch a.farmer f join fetch f.village where a.active = true")
    List<PriceAlert> findActiveWithFarmer();

    @Query("select a from PriceAlert a join fetch a.farmer f join fetch f.village where a.id = :id")
    Optional<PriceAlert> findWithFarmerById(Long id);
}
