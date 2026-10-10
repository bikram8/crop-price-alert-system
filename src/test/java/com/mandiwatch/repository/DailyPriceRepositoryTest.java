package com.mandiwatch.repository;

import com.mandiwatch.domain.Commodity;
import com.mandiwatch.domain.DailyPrice;
import com.mandiwatch.domain.Mandi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * JPA slice test: only entities, repositories and an embedded database are started.
 * Each test runs in a transaction that is rolled back at the end, so tests don't see
 * each other's data. This checks the hand-written JPQL, which a unit test can't.
 */
@DataJpaTest
class DailyPriceRepositoryTest {

    private static final LocalDate DAY1 = LocalDate.of(2026, 9, 1);
    private static final LocalDate DAY2 = LocalDate.of(2026, 9, 2);

    @Autowired TestEntityManager em;
    @Autowired DailyPriceRepository repository;

    private Commodity tomato;
    private Commodity onion;

    @BeforeEach
    void setUp() {
        Mandi hyd = em.persist(new Mandi("Bowenpally", "Hyderabad", "Telangana"));
        Mandi wgl = em.persist(new Mandi("Warangal", "Warangal", "Telangana"));
        tomato = em.persist(new Commodity("Tomato", null));
        onion = em.persist(new Commodity("Onion", null));

        save(hyd, tomato, DAY1, 2000);
        save(wgl, tomato, DAY1, 2400);
        save(hyd, tomato, DAY2, 2200);
        save(hyd, onion, DAY2, 3000);
        em.flush();
        em.clear(); // make the queries below really hit the database
    }

    @Test
    void findForDayWithoutFiltersReturnsEverythingThatDay() {
        assertThat(repository.findForDay(DAY1, null, null)).hasSize(2);
    }

    @Test
    void findForDayFiltersByCommodityAndDistrict() {
        assertThat(repository.findForDay(DAY2, onion.getId(), null))
                .extracting(DailyPrice::getModalPrice).containsExactly(3000);
        assertThat(repository.findForDay(DAY1, null, "Warangal"))
                .extracting(DailyPrice::getModalPrice).containsExactly(2400);
    }

    @Test
    void dailyAverageProjectionIsFilledByConstructorExpression() {
        assertThat(repository.findDailyAverage(tomato, DAY1))
                .extracting(DailyAverage::date, DailyAverage::average)
                .containsExactly(tuple(DAY1, 2200.0), tuple(DAY2, 2200.0));
    }

    @Test
    void averagesForAllCommoditiesComeFromOneQuery() {
        assertThat(repository.findDailyAveragesForAll(DAY2))
                .extracting(CommodityDailyAverage::commodityId, CommodityDailyAverage::average)
                .containsExactlyInAnyOrder(tuple(tomato.getId(), 2200.0), tuple(onion.getId(), 3000.0));
    }

    @Test
    void latestDateIsTheMaximum() {
        assertThat(repository.findLatestDate()).contains(DAY2);
    }

    private void save(Mandi mandi, Commodity commodity, LocalDate date, int modal) {
        DailyPrice p = new DailyPrice(mandi, commodity, date);
        p.updatePrices("Local", modal - 100, modal + 100, modal);
        em.persist(p);
    }
}
