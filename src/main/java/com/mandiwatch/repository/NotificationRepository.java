package com.mandiwatch.repository;

import com.mandiwatch.domain.Farmer;
import com.mandiwatch.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("select n from Notification n join fetch n.farmer order by n.createdAt desc, n.id desc")
    List<Notification> findAllNewestFirst();

    /**
     * One page of the outbox. A "join fetch" query can't be turned into a count query
     * automatically, so the count is given separately.
     */
    @Query(value = "select n from Notification n join fetch n.farmer",
            countQuery = "select count(n) from Notification n")
    Page<Notification> findPageWithFarmer(Pageable pageable);

    List<Notification> findByFarmerOrderByCreatedAtDesc(Farmer farmer);
}
