package com.mandiwatch.repository;

import com.mandiwatch.domain.Mandi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MandiRepository extends JpaRepository<Mandi, Long> {

    Optional<Mandi> findByNameIgnoreCaseAndDistrictIgnoreCaseAndStateIgnoreCase(String name, String district, String state);

    List<Mandi> findAllByOrderByStateAscDistrictAscNameAsc();
}
