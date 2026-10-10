package com.mandiwatch.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Everything under "mandi:" in application.yml.
 * <p>
 * {@code @Validated} makes a bad value (say page-size: 0) stop the app at startup
 * with a clear message, instead of failing much later at runtime.
 * {@code @Valid} on each component is needed so validation goes into the nested records.
 */
@Validated
@ConfigurationProperties(prefix = "mandi")
public record MandiProperties(@Valid @NotNull Api api,
                              @Valid @NotNull Transport transport,
                              @Valid @NotNull Sample sample,
                              @Valid @NotNull Admin admin) {

    /**
     * data.gov.in settings. The key can be left empty: the app then runs on
     * sample data and CSV uploads only.
     */
    public record Api(@NotBlank String baseUrl,
                      String key,
                      String state,
                      @NotBlank String stateFilterParam,
                      @Positive int pageSize,
                      @Positive int maxPages,
                      boolean scheduled,
                      @NotBlank String cron,
                      @NotNull Duration connectTimeout,
                      @NotNull Duration readTimeout) {

        public boolean hasKey() {
            return key != null && !key.isBlank();
        }
    }

    /**
     * Defaults for the net-price calculation. The farmer can override the rate on the form.
     *
     * @param ratePerKmPerQuintal what a hired tempo/truck costs per km for each quintal carried
     * @param roadFactor          straight-line distance is multiplied by this to approximate road distance
     * @param hamaliPerQuintal    loading/unloading labour charge
     * @param freshnessDays       ignore mandi prices older than this many days before the latest bulletin
     */
    public record Transport(@PositiveOrZero double ratePerKmPerQuintal,
                            @DecimalMin("1.0") double roadFactor,
                            @PositiveOrZero int hamaliPerQuintal,
                            @Min(0) int freshnessDays) {
    }

    public record Sample(boolean enabled, @Positive int days) {
    }

    /** The single admin login that protects import, mandi editing and the H2 console. */
    public record Admin(@NotBlank String username, @NotBlank String password) {
    }
}
