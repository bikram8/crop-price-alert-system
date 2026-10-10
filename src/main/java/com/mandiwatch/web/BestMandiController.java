package com.mandiwatch.web;

import com.mandiwatch.config.MandiProperties;
import com.mandiwatch.domain.Commodity;
import com.mandiwatch.domain.Village;
import com.mandiwatch.repository.CommodityRepository;
import com.mandiwatch.repository.VillageRepository;
import com.mandiwatch.service.BestMandiService;
import com.mandiwatch.service.NotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class BestMandiController {

    private final BestMandiService bestMandiService;
    private final CommodityRepository commodityRepository;
    private final VillageRepository villageRepository;
    private final MandiProperties.Transport transport;

    public BestMandiController(BestMandiService bestMandiService,
                               CommodityRepository commodityRepository,
                               VillageRepository villageRepository,
                               MandiProperties properties) {
        this.bestMandiService = bestMandiService;
        this.commodityRepository = commodityRepository;
        this.villageRepository = villageRepository;
        this.transport = properties.transport();
    }

    /**
     * Plain GET with query params, so a result can be bookmarked or shared
     * ("check where to sell my tomatoes: /best-mandi?...").
     */
    @GetMapping("/best-mandi")
    public String bestMandi(@RequestParam(required = false) Long commodityId,
                            @RequestParam(required = false) Long villageId,
                            @RequestParam(defaultValue = "20") double quantity,
                            @RequestParam(required = false) Double rate,
                            @RequestParam(defaultValue = "150") int radius,
                            Model model) {
        double ratePerKm = rate == null ? transport.ratePerKmPerQuintal() : rate;

        model.addAttribute("commodities", commodityRepository.findAllByOrderByNameAsc());
        model.addAttribute("villages", villageRepository.findAllByOrderByNameAsc());
        model.addAttribute("commodityId", commodityId);
        model.addAttribute("villageId", villageId);
        model.addAttribute("quantity", quantity);
        model.addAttribute("rate", ratePerKm);
        model.addAttribute("radius", radius);
        model.addAttribute("hamali", transport.hamaliPerQuintal());

        if (commodityId != null && villageId != null) {
            if (quantity <= 0 || quantity > 10_000 || ratePerKm < 0 || radius <= 0) {
                model.addAttribute("error", "Check the numbers: quantity must be more than 0, rate can't be negative.");
                return "best-mandi";
            }
            Commodity commodity = commodityRepository.findById(commodityId)
                    .orElseThrow(() -> new NotFoundException("Commodity " + commodityId));
            Village village = villageRepository.findById(villageId)
                    .orElseThrow(() -> new NotFoundException("Village " + villageId));

            var request = new BestMandiService.Request(commodity, village.getLatitude(), village.getLongitude(),
                    quantity, ratePerKm, radius);
            model.addAttribute("commodity", commodity);
            model.addAttribute("village", village);
            model.addAttribute("result", bestMandiService.findBest(request));
        }
        return "best-mandi";
    }
}
