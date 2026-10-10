package com.mandiwatch.web;

import com.mandiwatch.domain.Farmer;
import com.mandiwatch.domain.Language;
import com.mandiwatch.repository.CommodityRepository;
import com.mandiwatch.repository.NotificationRepository;
import com.mandiwatch.repository.VillageRepository;
import com.mandiwatch.service.DuplicatePhoneException;
import com.mandiwatch.service.FarmerService;
import com.mandiwatch.service.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Web slice test: only the MVC layer for FarmerController is started (no database, no services).
 * Everything the controller depends on is replaced with a Mockito mock via @MockitoBean.
 * <p>
 * addFilters = false skips the Spring Security filter chain; this test is about binding,
 * validation and redirects. Security rules are tested in SecurityAndApiTest.
 */
@WebMvcTest(FarmerController.class)
@AutoConfigureMockMvc(addFilters = false)
class FarmerControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean FarmerService farmerService;
    @MockitoBean VillageRepository villageRepository;
    @MockitoBean CommodityRepository commodityRepository;
    @MockitoBean NotificationRepository notificationRepository;

    @Test
    void listPageShowsEmptyForm() throws Exception {
        mvc.perform(get("/farmers"))
                .andExpect(status().isOk())
                .andExpect(view().name("farmers"))
                .andExpect(model().attributeExists("farmerForm", "farmers", "villages"));
    }

    @Test
    void invalidPhoneIsRejectedWithoutCallingTheService() throws Exception {
        mvc.perform(post("/farmers").param("name", "Ramesh").param("phone", "12345").param("villageId", "1")
                        .param("language", "TELUGU"))
                .andExpect(status().isOk())
                .andExpect(view().name("farmers"))
                .andExpect(model().attributeHasFieldErrors("farmerForm", "phone"));

        verify(farmerService, never()).register(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void duplicatePhoneShowsAsFieldError() throws Exception {
        when(farmerService.register(anyString(), anyString(), anyLong(), any()))
                .thenThrow(new DuplicatePhoneException("9876543210"));

        mvc.perform(post("/farmers").param("name", "Ramesh").param("phone", "9876543210").param("villageId", "1")
                        .param("language", "TELUGU"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("farmerForm", "phone", "duplicate"));
    }

    @Test
    void validRegistrationRedirectsToTheNewFarmer() throws Exception {
        Farmer saved = mock(Farmer.class);
        when(saved.getId()).thenReturn(7L);
        when(saved.getName()).thenReturn("Ramesh");
        when(farmerService.register("Ramesh", "9876543210", 1L, Language.TELUGU)).thenReturn(saved);

        mvc.perform(post("/farmers").param("name", "Ramesh").param("phone", "9876543210").param("villageId", "1")
                        .param("language", "TELUGU"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/farmers/7"));
    }

    @Test
    void unknownFarmerIs404() throws Exception {
        when(farmerService.getFarmer(99L)).thenThrow(new NotFoundException("Farmer 99"));

        mvc.perform(get("/farmers/99")).andExpect(status().isNotFound());
    }
}
