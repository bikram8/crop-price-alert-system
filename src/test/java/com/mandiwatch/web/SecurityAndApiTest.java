package com.mandiwatch.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full application context with the real SecurityFilterChain, driven through MockMvc
 * (no real HTTP port). Covers who can reach what, CSRF, login, and the API's error format.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAndApiTest {

    @Autowired MockMvc mvc;

    @Test
    void publicPagesNeedNoLogin() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/best-mandi")).andExpect(status().isOk());
    }

    @Test
    void importPageRedirectsAnonymousUserToLogin() throws Exception {
        mvc.perform(get("/import"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanOpenImportAndOutbox() throws Exception {
        mvc.perform(get("/import")).andExpect(status().isOk());
        mvc.perform(get("/notifications")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void postWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/import/api")).andExpect(status().isForbidden());
    }

    @Test
    void adminLoginWorksWithConfiguredPassword() throws Exception {
        mvc.perform(formLogin().user("admin").password("admin123")).andExpect(authenticated().withRoles("ADMIN"));
        mvc.perform(formLogin().user("admin").password("wrong")).andExpect(unauthenticated());
    }

    @Test
    void anonymousCannotEditMandi() throws Exception {
        mvc.perform(post("/mandis/1").with(csrf()).param("feePercent", "1.0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void invalidApiParameterGivesProblemDetail() throws Exception {
        mvc.perform(get("/api/best-mandi").param("commodity", "Tomato").param("villageId", "1").param("quantity", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unknownCommodityGivesProblemDetail404() throws Exception {
        mvc.perform(get("/api/prices").param("commodity", "Dragonfruit"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Commodity 'Dragonfruit' not found"));
    }

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
