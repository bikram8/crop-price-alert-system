package com.mandiwatch.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommodityNormalizerTest {

    private final CommodityNormalizer normalizer = new CommodityNormalizer();

    @Test
    void mapsGovernmentSpellingsToOneName() {
        assertThat(normalizer.normalize("Paddy(Dhan)(Common)")).isEqualTo("Paddy");
        assertThat(normalizer.normalize("  PADDY ")).isEqualTo("Paddy");
        assertThat(normalizer.normalize("Dhan")).isEqualTo("Paddy");
        assertThat(normalizer.normalize("Bhindi(Ladies Finger)")).isEqualTo("Bhindi");
    }

    @Test
    void keepsUnknownCropsButFixesCase() {
        assertThat(normalizer.normalize("CLUSTER   BEANS")).isEqualTo("Cluster Beans");
    }

    @Test
    void blankIsNull() {
        assertThat(normalizer.normalize("  ")).isNull();
        assertThat(normalizer.normalize(null)).isNull();
    }
}
