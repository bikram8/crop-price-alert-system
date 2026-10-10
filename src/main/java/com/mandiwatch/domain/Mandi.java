package com.mandiwatch.domain;

import jakarta.persistence.*;

/**
 * A wholesale market yard (APMC mandi).
 * Coordinates are optional because mandis that arrive through the
 * government feed don't carry a location. Those mandis still show up
 * in price lists, but the best-mandi calculator skips them until
 * someone fills in latitude/longitude on the Mandis page.
 */
@Entity
@Table(name = "mandi", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "district", "state"}))
public class Mandi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String district;

    @Column(nullable = false)
    private String state;

    private Double latitude;
    private Double longitude;

    /** Market fee + commission charged on the sale value, in percent. */
    private double feePercent = 1.0;

    protected Mandi() {
    }

    public Mandi(String name, String district, String state) {
        this.name = name;
        this.district = district;
        this.state = state;
    }

    public Mandi(String name, String district, String state, Double latitude, Double longitude, double feePercent) {
        this(name, district, state);
        this.latitude = latitude;
        this.longitude = longitude;
        this.feePercent = feePercent;
    }

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDistrict() { return district; }
    public String getState() { return state; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public double getFeePercent() { return feePercent; }

    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setFeePercent(double feePercent) { this.feePercent = feePercent; }

    /** Same id-based rule as {@link PriceAlert#equals(Object)}. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Mandi other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Mandi.class.hashCode();
    }
}
