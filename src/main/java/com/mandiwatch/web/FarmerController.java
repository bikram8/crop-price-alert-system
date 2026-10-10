package com.mandiwatch.web;

import com.mandiwatch.domain.AlertCondition;
import com.mandiwatch.domain.Farmer;
import com.mandiwatch.domain.Language;
import com.mandiwatch.repository.CommodityRepository;
import com.mandiwatch.repository.NotificationRepository;
import com.mandiwatch.repository.VillageRepository;
import com.mandiwatch.service.DuplicatePhoneException;
import com.mandiwatch.service.FarmerService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FarmerController {

    private final FarmerService farmerService;
    private final VillageRepository villageRepository;
    private final CommodityRepository commodityRepository;
    private final NotificationRepository notificationRepository;

    public FarmerController(FarmerService farmerService,
                            VillageRepository villageRepository,
                            CommodityRepository commodityRepository,
                            NotificationRepository notificationRepository) {
        this.farmerService = farmerService;
        this.villageRepository = villageRepository;
        this.commodityRepository = commodityRepository;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/farmers")
    public String list(Model model) {
        if (!model.containsAttribute("farmerForm")) {
            model.addAttribute("farmerForm", new FarmerForm());
        }
        fillListModel(model);
        return "farmers";
    }

    @PostMapping("/farmers")
    public String register(@Valid @ModelAttribute("farmerForm") FarmerForm form,
                           BindingResult binding,
                           Model model,
                           RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            fillListModel(model);
            return "farmers";
        }
        try {
            Farmer farmer = farmerService.register(form.getName(), form.getPhone(), form.getVillageId(), form.getLanguage());
            redirect.addFlashAttribute("message", "Registered " + farmer.getName() + ". Now set up a price alert.");
            return "redirect:/farmers/" + farmer.getId();
        } catch (DuplicatePhoneException e) {
            binding.rejectValue("phone", "duplicate", e.getMessage());
            fillListModel(model);
            return "farmers";
        }
    }

    @GetMapping("/farmers/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Farmer farmer = farmerService.getFarmer(id);
        if (!model.containsAttribute("alertForm")) {
            model.addAttribute("alertForm", new AlertForm());
        }
        model.addAttribute("farmer", farmer);
        model.addAttribute("commodities", commodityRepository.findAllByOrderByNameAsc());
        model.addAttribute("conditions", AlertCondition.values());
        model.addAttribute("notifications", notificationRepository.findByFarmerOrderByCreatedAtDesc(farmer));
        return "farmer-detail";
    }

    @PostMapping("/farmers/{id}/alerts")
    public String addAlert(@PathVariable Long id,
                           @Valid @ModelAttribute("alertForm") AlertForm form,
                           BindingResult binding,
                           RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("org.springframework.validation.BindingResult.alertForm", binding);
            redirect.addFlashAttribute("alertForm", form);
            return "redirect:/farmers/" + id;
        }
        Long alertId = farmerService.addAlert(id, form.getCommodityId(), form.getCondition(), form.getThreshold(), form.getRadiusKm());
        // The after-commit listener has already run by the time addAlert() returns,
        // so this tells us whether today's prices matched straight away.
        redirect.addFlashAttribute("message", farmerService.hasFired(alertId)
                ? "Alert saved. Today's prices already match it, so an SMS went out (see below)."
                : "Alert saved. You'll get an SMS when a bulletin matches it.");
        return "redirect:/farmers/" + id;
    }

    @PostMapping("/farmers/{id}/alerts/{alertId}/toggle")
    public String toggleAlert(@PathVariable Long id, @PathVariable Long alertId) {
        farmerService.toggleAlert(id, alertId);
        return "redirect:/farmers/" + id;
    }

    @PostMapping("/farmers/{id}/alerts/{alertId}/delete")
    public String deleteAlert(@PathVariable Long id, @PathVariable Long alertId, RedirectAttributes redirect) {
        farmerService.deleteAlert(id, alertId);
        redirect.addFlashAttribute("message", "Alert removed.");
        return "redirect:/farmers/" + id;
    }

    private void fillListModel(Model model) {
        model.addAttribute("farmers", farmerService.listFarmers());
        model.addAttribute("villages", villageRepository.findAllByOrderByNameAsc());
        model.addAttribute("languages", Language.values());
    }
}
