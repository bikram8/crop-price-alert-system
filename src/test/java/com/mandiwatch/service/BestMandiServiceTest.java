package com.mandiwatch.service;

import com.mandiwatch.config.MandiProperties;
import com.mandiwatch.domain.Commodity;
import com.mandiwatch.domain.DailyPrice;
import com.mandiwatch.domain.Mandi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BestMandiServiceTest {

    private final MandiProperties props = new MandiProperties(
            null, new MandiProperties.Transport(1.0, 1.3, 30, 3), null, null);
    private final BestMandiService service = new BestMandiService(null, props);

    @Test
    void netAmountSubtractsAllCosts() {
        Mandi mandi = new Mandi("Bowenpally", "Hyderabad", "Telangana", 17.47, 78.47, 1.0);
        DailyPrice price = new DailyPrice(mandi, new Commodity("Tomato", null), LocalDate.of(2026, 10, 3));
        price.updatePrices("Local", 1800, 2400, 2000);

        // 20 quintals, 50 km road, ₹1/km/qtl
        BestMandiService.Offer offer = service.calculate(price, 50, 20, 1.0);

        assertThat(offer.grossAmount()).isEqualTo(40_000);   // 2000 x 20
        assertThat(offer.transportCost()).isEqualTo(1_000);  // 50 x 1 x 20
        assertThat(offer.marketFee()).isEqualTo(400);        // 1% of 40,000
        assertThat(offer.hamali()).isEqualTo(600);           // 30 x 20
        assertThat(offer.netAmount()).isEqualTo(38_000);
        assertThat(offer.getNetPerQuintal(20)).isEqualTo(1_900);
    }

    @Test
    void fartherHigherPriceCanLoseToNearerLowerPrice() {
        Mandi near = new Mandi("Near", "D", "S", 0.0, 0.0, 1.0);
        Mandi far = new Mandi("Far", "D", "S", 0.0, 0.0, 1.0);
        DailyPrice nearPrice = new DailyPrice(near, new Commodity("Tomato", null), LocalDate.now());
        nearPrice.updatePrices(null, 1900, 2100, 2000);
        DailyPrice farPrice = new DailyPrice(far, new Commodity("Tomato", null), LocalDate.now());
        farPrice.updatePrices(null, 2000, 2200, 2100);

        long nearNet = service.calculate(nearPrice, 10, 20, 1.5).netAmount();
        long farNet = service.calculate(farPrice, 150, 20, 1.5).netAmount();

        // ₹100/qtl more, but 140 extra km at ₹1.5 costs ₹210/qtl
        assertThat(nearNet).isGreaterThan(farNet);
    }
}
