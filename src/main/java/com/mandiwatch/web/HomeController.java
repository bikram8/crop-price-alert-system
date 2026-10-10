package com.mandiwatch.web;

import com.mandiwatch.domain.DailyPrice;
import com.mandiwatch.repository.CommodityRepository;
import com.mandiwatch.repository.DailyPriceRepository;
import com.mandiwatch.repository.FarmerRepository;
import com.mandiwatch.repository.MandiRepository;
import com.mandiwatch.service.MarketSummaryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
public class HomeController {

    private final DailyPriceRepository priceRepository;
    private final CommodityRepository commodityRepository;
    private final MandiRepository mandiRepository;
    private final FarmerRepository farmerRepository;
    private final MarketSummaryService summaryService;

    public HomeController(DailyPriceRepository priceRepository,
                          CommodityRepository commodityRepository,
                          MandiRepository mandiRepository,
                          FarmerRepository farmerRepository,
                          MarketSummaryService summaryService) {
        this.priceRepository = priceRepository;
        this.commodityRepository = commodityRepository;
        this.mandiRepository = mandiRepository;
        this.farmerRepository = farmerRepository;
        this.summaryService = summaryService;
    }

    @GetMapping("/")
    public String home(@RequestParam(required = false) Long commodityId,
                       @RequestParam(required = false) String district,
                       Model model) {
        LocalDate latest = priceRepository.findLatestDate().orElse(null);
        String districtFilter = (district == null || district.isBlank()) ? null : district;

        List<DailyPrice> prices = latest == null
                ? List.of()
                : priceRepository.findForDay(latest, commodityId, districtFilter);

        model.addAttribute("latestDate", latest);
        model.addAttribute("prices", prices);
        model.addAttribute("commodities", commodityRepository.findAllByOrderByNameAsc());
        model.addAttribute("districts", priceRepository.findDistricts());
        model.addAttribute("selectedCommodity", commodityId);
        model.addAttribute("selectedDistrict", districtFilter);
        model.addAttribute("movers", summaryService.weeklyMovers(6));
        model.addAttribute("mandiCount", mandiRepository.count());
        model.addAttribute("farmerCount", farmerRepository.count());
        model.addAttribute("rowCount", priceRepository.count());
        return "home";
    }
}
