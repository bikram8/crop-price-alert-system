package com.mandiwatch.web;

import com.mandiwatch.domain.Commodity;
import com.mandiwatch.domain.DailyPrice;
import com.mandiwatch.domain.Village;
import com.mandiwatch.repository.CommodityRepository;
import com.mandiwatch.repository.DailyPriceRepository;
import com.mandiwatch.repository.VillageRepository;
import com.mandiwatch.config.MandiProperties;
import com.mandiwatch.service.BestMandiService;
import com.mandiwatch.service.NotFoundException;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * JSON endpoints, handy for showing the backend on its own (Postman / curl)
 * and for anyone who wants to build a mobile app on top.
 * <p>
 * Constraint annotations directly on @RequestParam are enforced by Spring MVC's built-in
 * method validation (Spring 6.1+). A violation becomes a 400 with a ProblemDetail body,
 * see ApiExceptionHandler. Before, quantity=0 reached a division by zero.
 */
@RestController
@RequestMapping("/api")
public class PriceApiController {

    private final DailyPriceRepository priceRepository;
    private final CommodityRepository commodityRepository;
    private final VillageRepository villageRepository;
    private final BestMandiService bestMandiService;
    private final MandiProperties.Transport transport;

    public PriceApiController(DailyPriceRepository priceRepository,
                              CommodityRepository commodityRepository,
                              VillageRepository villageRepository,
                              BestMandiService bestMandiService,
                              MandiProperties properties) {
        this.priceRepository = priceRepository;
        this.commodityRepository = commodityRepository;
        this.villageRepository = villageRepository;
        this.bestMandiService = bestMandiService;
        this.transport = properties.transport();
    }

    public record PriceDto(LocalDate date, String state, String district, String market,
                           String commodity, int minPrice, int maxPrice, int modalPrice) {

        static PriceDto from(DailyPrice p) {
            return new PriceDto(p.getPriceDate(), p.getMandi().getState(), p.getMandi().getDistrict(),
                    p.getMandi().getName(), p.getCommodity().getName(),
                    p.getMinPrice(), p.getMaxPrice(), p.getModalPrice());
        }
    }

    public record OfferDto(String market, String district, LocalDate priceDate, int modalPrice, int roadKm,
                           long gross, long transport, long marketFee, long hamali, long net) {
    }

    public record BestMandiDto(String commodity, String from, double quantityQuintals,
                               double ratePerKmPerQuintal, String bestMarket, long extraOverNearest,
                               List<OfferDto> offers) {
    }

    @GetMapping("/prices")
    public List<PriceDto> prices(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 @RequestParam(required = false) String commodity,
                                 @RequestParam(required = false) String district) {
        LocalDate day = date != null ? date : priceRepository.findLatestDate().orElse(null);
        if (day == null) {
            return List.of();
        }
        Long commodityId = commodity == null ? null
                : commodityRepository.findByNameIgnoreCase(commodity)
                .orElseThrow(() -> new NotFoundException("Commodity '" + commodity + "'")).getId();
        return priceRepository.findForDay(day, commodityId, district).stream().map(PriceDto::from).toList();
    }

    @GetMapping("/commodities")
    public List<String> commodities() {
        return commodityRepository.findAllByOrderByNameAsc().stream().map(Commodity::getName).toList();
    }

    @GetMapping("/best-mandi")
    public BestMandiDto bestMandi(@RequestParam @NotBlank String commodity,
                                  @RequestParam Long villageId,
                                  @RequestParam(defaultValue = "20") @Positive @DecimalMax("10000") double quantity,
                                  @RequestParam(required = false) @DecimalMin("0") Double rate,
                                  @RequestParam(defaultValue = "150") @Min(1) @Max(500) int radius) {
        Commodity c = commodityRepository.findByNameIgnoreCase(commodity)
                .orElseThrow(() -> new NotFoundException("Commodity '" + commodity + "'"));
        Village v = villageRepository.findById(villageId)
                .orElseThrow(() -> new NotFoundException("Village " + villageId));
        double ratePerKm = rate == null ? transport.ratePerKmPerQuintal() : rate;

        var result = bestMandiService.findBest(new BestMandiService.Request(
                c, v.getLatitude(), v.getLongitude(), quantity, ratePerKm, radius));

        List<OfferDto> offers = result.offers().stream()
                .map(o -> new OfferDto(o.mandi().getName(), o.mandi().getDistrict(), o.priceDate(), o.modalPrice(),
                        o.roadKm(), o.grossAmount(), o.transportCost(), o.marketFee(), o.hamali(), o.netAmount()))
                .toList();
        return new BestMandiDto(c.getName(), v.getLabel(), quantity, ratePerKm,
                result.best() == null ? null : result.best().mandi().getName(),
                result.extraOverNearest(), offers);
    }
}
