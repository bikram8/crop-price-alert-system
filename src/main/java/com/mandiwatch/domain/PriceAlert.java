package com.mandiwatch.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "price_alert")
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id")
    private Farmer farmer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "commodity_id")
    private Commodity commodity;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_condition", nullable = false)
    private AlertCondition condition;

    /** Rupees per quintal. */
    private int threshold;

    private int radiusKm;

    private boolean active = true;

    /**
     * Price date that last triggered this alert. Stops the same bulletin
     * from sending the same SMS twice if the data is imported again.
     */
    private LocalDate lastTriggeredFor;

    /**
     * Optimistic locking. Every UPDATE becomes "... where id=? and version=?" and bumps the
     * version. If two imports evaluate the same alert at once, the second UPDATE matches no
     * row, Hibernate throws, and that transaction rolls back before it sends a duplicate SMS.
     */
    @Version
    private Long version;

    protected PriceAlert() {
    }

    public PriceAlert(Farmer farmer, Commodity commodity, AlertCondition condition, int threshold, int radiusKm) {
        this.farmer = farmer;
        this.commodity = commodity;
        this.condition = condition;
        this.threshold = threshold;
        this.radiusKm = radiusKm;
    }

    public String describe() {
        return commodity.getName() + " " + condition.getPhrase() + " ₹" + threshold + "/qtl within " + radiusKm + " km";
    }

    public Long getId() { return id; }
    public Farmer getFarmer() { return farmer; }
    public Commodity getCommodity() { return commodity; }
    public AlertCondition getCondition() { return condition; }
    public int getThreshold() { return threshold; }
    public int getRadiusKm() { return radiusKm; }
    public boolean isActive() { return active; }
    public LocalDate getLastTriggeredFor() { return lastTriggeredFor; }

    public Long getVersion() { return version; }

    public void setActive(boolean active) { this.active = active; }
    public void setLastTriggeredFor(LocalDate lastTriggeredFor) { this.lastTriggeredFor = lastTriggeredFor; }

    /**
     * Two alerts are equal when they have the same database id. A new, unsaved alert
     * (id == null) is only equal to itself. instanceof (not getClass()) so a Hibernate
     * proxy subclass still compares equal to the real object.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PriceAlert other)) return false;
        return id != null && id.equals(other.getId());
    }

    /**
     * Constant on purpose: the id is null before persist and set after, so a hash based on
     * the id would change while the object sits in a HashSet and the set would lose it.
     */
    @Override
    public int hashCode() {
        return PriceAlert.class.hashCode();
    }
}
