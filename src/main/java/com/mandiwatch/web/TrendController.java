package com.mandiwatch.web;

import com.mandiwatch.domain.Commodity;
import com.mandiwatch.domain.Mandi;
import com.mandiwatch.repository.CommodityRepository;
import com.mandiwatch.repository.MandiRepository;
import com.mandiwatch.service.NotFoundException;
import com.mandiwatch.service.TrendService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class TrendController {

    private static final List<Integer> ALLOWED_DAYS = List.of(15, 30, 60, 90);

    private final TrendService trendService;
    private final CommodityRepository commodityRepository;
    private final MandiRepository mandiRepository;

    public TrendController(TrendService trendService,
                           CommodityRepository commodityRepository,
                           MandiRepository mandiRepository) {
        this.trendService = trendService;
        this.commodityRepository = commodityRepository;
        this.mandiRepository = mandiRepository;
    }

    @GetMapping("/trends")
    public String trends(@RequestParam(required = false) Long commodityId,
                         @RequestParam(required = false) Long mandiId,
                         @RequestParam(defaultValue = "30") int days,
                         Model model) {
        List<Commodity> commodities = commodityRepository.findAllByOrderByNameAsc();
        if (!ALLOWED_DAYS.contains(days)) {
            days = 30;
        }
        model.addAttribute("commodities", commodities);
        model.addAttribute("mandis", mandiRepository.findAllByOrderByStateAscDistrictAscNameAsc());
        model.addAttribute("allowedDays", ALLOWED_DAYS);
        model.addAttribute("days", days);

        if (commodityId == null && !commodities.isEmpty()) {
            commodityId = commodities.get(0).getId();
        }
        if (commodityId == null) {
            return "trends";
        }

        Long id = commodityId;
        Commodity commodity = commodityRepository.findById(id).orElseThrow(() -> new NotFoundException("Commodity " + id));
        Mandi mandi = mandiId == null ? null
                : mandiRepository.findById(mandiId).orElseThrow(() -> new NotFoundException("Mandi " + mandiId));

        model.addAttribute("commodityId", commodityId);
        model.addAttribute("mandiId", mandiId);
        model.addAttribute("commodity", commodity);
        model.addAttribute("mandi", mandi);
        TrendService.Trend trend = trendService.trend(commodity, mandi, days);
        model.addAttribute("trend", trend);

        // plain lists for the chart script
        DateTimeFormatter label = DateTimeFormatter.ofPattern("d MMM");
        model.addAttribute("chartLabels", trend.points().stream().map(p -> p.date().format(label)).toList());
        model.addAttribute("chartPrices", trend.points().stream().map(TrendService.Point::price).toList());
        model.addAttribute("chartAverages", trend.points().stream().map(TrendService.Point::movingAvg7).toList());
        return "trends";
    }
}
