package com.mandiwatch.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GeoUtilsTest {

    @Test
    void hyderabadToWarangalIsAbout130kmStraightLine() {
        // Bowenpally to Warangal market yard
        double km = GeoUtils.haversineKm(17.4710, 78.4790, 17.9880, 79.5600);
        assertThat(km).isCloseTo(128, within(8.0));
    }

    @Test
    void roadDistanceIsScaled() {
        double straight = GeoUtils.haversineKm(17.0, 78.0, 17.5, 78.5);
        assertThat(GeoUtils.roadKm(17.0, 78.0, 17.5, 78.5, 1.3)).isCloseTo(straight * 1.3, within(0.001));
    }

    @Test
    void samePointIsZero() {
        assertThat(GeoUtils.haversineKm(17.3, 78.4, 17.3, 78.4)).isZero();
    }
}
