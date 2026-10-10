package com.mandiwatch.repository;

import com.mandiwatch.domain.Commodity;
import com.mandiwatch.domain.DailyPrice;
import com.mandiwatch.domain.Mandi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyPriceRepository extends JpaRepository<DailyPrice, Long> {

    @Query("select max(p.priceDate) from DailyPrice p")
    Optional<LocalDate> findLatestDate();

    @Query("""
            select p from DailyPrice p
            join fetch p.mandi m
            join fetch p.commodity c
            where p.priceDate = :date
              and (:commodityId is null or c.id = :commodityId)
              and (:district is null or m.district = :district)
            order by c.name, p.modalPrice desc
            """)
    List<DailyPrice> findForDay(@Param("date") LocalDate date,
                                @Param("commodityId") Long commodityId,
                                @Param("district") String district);

    /**
     * Prices of one commodity across all mandis in a date window.
     * The best-mandi calculator and the alert checker pick the newest row per mandi from this.
     */
    @Query("""
            select p from DailyPrice p
            join fetch p.mandi
            where p.commodity = :commodity
              and p.priceDate between :from and :to
            order by p.priceDate desc
            """)
    List<DailyPrice> findForCommodityBetween(@Param("commodity") Commodity commodity,
                                             @Param("from") LocalDate from,
                                             @Param("to") LocalDate to);

    @Query("""
            select p from DailyPrice p
            where p.commodity = :commodity and p.mandi = :mandi
              and p.priceDate >= :from
            order by p.priceDate
            """)
    List<DailyPrice> findHistory(@Param("commodity") Commodity commodity,
                                 @Param("mandi") Mandi mandi,
                                 @Param("from") LocalDate from);

    @Query("""
            select new com.mandiwatch.repository.DailyAverage(p.priceDate, avg(p.modalPrice))
            from DailyPrice p
            where p.commodity = :commodity and p.priceDate >= :from
            group by p.priceDate
            order by p.priceDate
            """)
    List<DailyAverage> findDailyAverage(@Param("commodity") Commodity commodity, @Param("from") LocalDate from);

    /** Daily averages for all commodities in one query (the home page used to run one query per crop). */
    @Query("""
            select new com.mandiwatch.repository.CommodityDailyAverage(p.commodity.id, p.priceDate, avg(p.modalPrice))
            from DailyPrice p
            where p.priceDate >= :from
            group by p.commodity.id, p.priceDate
            order by p.priceDate
            """)
    List<CommodityDailyAverage> findDailyAveragesForAll(@Param("from") LocalDate from);

    /**
     * Every stored row in a date range, loaded once before an import so the import can check
     * "does this (mandi, crop, date) already exist?" in memory instead of one SELECT per row.
     */
    @Query("select p from DailyPrice p where p.priceDate between :from and :to")
    List<DailyPrice> findAllBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select distinct p.mandi.district from DailyPrice p order by p.mandi.district")
    List<String> findDistricts();
}
