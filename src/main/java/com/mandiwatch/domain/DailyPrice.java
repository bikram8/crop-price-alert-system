package com.mandiwatch.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * One row of the daily bulletin: what a commodity sold for in one mandi on one day.
 * Prices are in rupees per quintal (100 kg), same as Agmarknet.
 */
@Entity
@Table(name = "daily_price",
        uniqueConstraints = @UniqueConstraint(columnNames = {"mandi_id", "commodity_id", "price_date"}),
        indexes = {
                @Index(name = "idx_price_date", columnList = "price_date"),
                @Index(name = "idx_price_commodity_date", columnList = "commodity_id, price_date")
        })
public class DailyPrice {

    /**
     * SEQUENCE, not IDENTITY: with IDENTITY the database hands out the id during the INSERT,
     * so Hibernate must run each INSERT immediately and can't batch them. With a sequence
     * Hibernate reserves 50 ids in one call (allocationSize) and sends inserts in batches of 50.
     * This is the table that gets thousands of rows per import, so it's the one that matters.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "daily_price_seq")
    @SequenceGenerator(name = "daily_price_seq", sequenceName = "daily_price_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "mandi_id")
    private Mandi mandi;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "commodity_id")
    private Commodity commodity;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    private String variety;

    private int minPrice;
    private int maxPrice;
    private int modalPrice;

    protected DailyPrice() {
    }

    public DailyPrice(Mandi mandi, Commodity commodity, LocalDate priceDate) {
        this.mandi = mandi;
        this.commodity = commodity;
        this.priceDate = priceDate;
    }

    public void updatePrices(String variety, int minPrice, int maxPrice, int modalPrice) {
        this.variety = variety;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.modalPrice = modalPrice;
    }

    public Long getId() { return id; }
    public Mandi getMandi() { return mandi; }
    public Commodity getCommodity() { return commodity; }
    public LocalDate getPriceDate() { return priceDate; }
    public String getVariety() { return variety; }
    public int getMinPrice() { return minPrice; }
    public int getMaxPrice() { return maxPrice; }
    public int getModalPrice() { return modalPrice; }
}
