package com.mandiwatch.service;

import com.mandiwatch.config.MandiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pulls the "Current Daily Price of Various Commodities from Various Markets (Mandi)"
 * dataset from the Open Government Data platform, page by page.
 */
@Component
public class DataGovClient {

    private static final Logger log = LoggerFactory.getLogger(DataGovClient.class);

    private final RestClient restClient;
    private final MandiProperties.Api config;

    public DataGovClient(RestClient.Builder builder, MandiProperties properties) {
        this.config = properties.api();
        // Without timeouts a server that accepts the connection and then never answers
        // would block the calling thread (and the nightly @Scheduled job) forever.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(config.connectTimeout());
        requestFactory.setReadTimeout(config.readTimeout());
        this.restClient = builder.requestFactory(requestFactory).build();
    }

    /** Shape of the JSON the API returns; we only need these two fields. */
    record Page(Integer total, List<Map<String, Object>> records) {
    }

    /**
     * Retries up to 2 more times (2s, then 4s) on network problems and 5xx answers, which are
     * usually temporary. Not on 4xx: a wrong API key won't fix itself by asking again.
     * <p>
     * Placed on this public method, which callers reach through the Spring proxy. Putting it
     * on a private per-page helper called from here would do nothing (self-invocation).
     */
    @Retryable(includes = {ResourceAccessException.class, HttpServerErrorException.class},
            maxRetries = 2, delay = 2000, multiplier = 2)
    public List<RawPriceRow> fetchTodaysPrices() {
        if (!config.hasKey()) {
            throw new IllegalStateException("No data.gov.in API key set. Put it in DATA_GOV_IN_KEY or mandi.api.key.");
        }

        List<RawPriceRow> rows = new ArrayList<>();
        int offset = 0;
        for (int page = 0; page < config.maxPages(); page++) {
            URI uri = buildUri(offset);
            Page body = restClient.get().uri(uri).retrieve().body(Page.class);

            if (body == null || body.records() == null || body.records().isEmpty()) {
                break;
            }
            body.records().forEach(r -> rows.add(RawPriceRow.fromMap(r)));
            offset += body.records().size();
            log.info("data.gov.in page {}: got {} records (total so far {})", page + 1, body.records().size(), offset);

            if (body.total() != null && offset >= body.total()) {
                break;
            }
        }
        return rows;
    }

    private URI buildUri(int offset) {
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(config.baseUrl())
                .queryParam("api-key", config.key())
                .queryParam("format", "json")
                .queryParam("limit", config.pageSize())
                .queryParam("offset", offset);
        if (config.state() != null && !config.state().isBlank()) {
            b.queryParam(config.stateFilterParam(), config.state());
        }
        return b.encode().build().toUri();
    }
}
