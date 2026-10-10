package com.mandiwatch.web;

import com.mandiwatch.domain.Notification;
import com.mandiwatch.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The SMS outbox (admin only). Used to load every message ever sent in one list;
 * now it's paged: /notifications?page=0&size=25 (page numbers start at 0).
 */
@Controller
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/notifications")
    public String notifications(@PageableDefault(size = 25, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
                                Pageable pageable,
                                Model model) {
        Page<Notification> page = notificationRepository.findPageWithFarmer(pageable);
        model.addAttribute("page", page);
        model.addAttribute("notifications", page.getContent());
        return "notifications";
    }
}
