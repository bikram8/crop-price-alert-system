package com.mandiwatch.repository;

import com.mandiwatch.domain.Commodity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommodityRepository extends JpaRepository<Commodity, Long> {

    Optional<Commodity> findByNameIgnoreCase(String name);

    List<Commodity> findAllByOrderByNameAsc();
}
