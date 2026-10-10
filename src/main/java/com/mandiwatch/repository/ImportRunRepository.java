package com.mandiwatch.repository;

import com.mandiwatch.domain.ImportRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ImportRunRepository extends JpaRepository<ImportRun, Long> {

    List<ImportRun> findTop20ByOrderByStartedAtDesc();

    /** Newest run that actually saved prices; used by the health check. */
    Optional<ImportRun> findFirstBySuccessTrueOrderByStartedAtDesc();

    /** Reads straight from the DB; the alert listener updates this column in its own transaction. */
    @Query("select r.alertsSent from ImportRun r where r.id = :id")
    Integer findAlertsSent(Long id);
}
