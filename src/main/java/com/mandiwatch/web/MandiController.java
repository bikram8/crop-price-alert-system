package com.mandiwatch.web;

import com.mandiwatch.domain.Mandi;
import com.mandiwatch.service.MandiService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Thin controller: HTTP in, HTTP out. Validation is declared on MandiForm, the transaction is in MandiService. */
@Controller
public class MandiController {

    private final MandiService mandiService;

    public MandiController(MandiService mandiService) {
        this.mandiService = mandiService;
    }

    @GetMapping("/mandis")
    public String list(Model model) {
        model.addAttribute("mandis", mandiService.listAll());
        return "mandis";
    }

    /** Mandis created from the API feed have no coordinates; this is where they get them. Admin only. */
    @PostMapping("/mandis/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute MandiForm form,
                         BindingResult binding,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("error", binding.getAllErrors().getFirst().getDefaultMessage());
            return "redirect:/mandis";
        }
        Mandi mandi = mandiService.updateLocation(id, form.getLatitude(), form.getLongitude(), form.getFeePercent());
        redirect.addFlashAttribute("message", "Updated " + mandi.getName() + ".");
        return "redirect:/mandis";
    }
}
