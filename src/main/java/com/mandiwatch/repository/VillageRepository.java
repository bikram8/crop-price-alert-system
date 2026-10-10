package com.mandiwatch.repository;

import com.mandiwatch.domain.Village;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VillageRepository extends JpaRepository<Village, Long> {

    List<Village> findAllByOrderByNameAsc();
}
