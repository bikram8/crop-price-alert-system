package com.mandiwatch.domain;

import jakarta.persistence.*;

/** A farmer's home location. Used as the starting point for transport cost. */
@Entity
@Table(name = "village")
public class Village {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String district;

    private double latitude;
    private double longitude;

    protected Village() {
    }

    public Village(String name, String district, double latitude, double longitude) {
        this.name = name;
        this.district = district;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDistrict() { return district; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    public String getLabel() {
        return name + ", " + district;
    }
}
