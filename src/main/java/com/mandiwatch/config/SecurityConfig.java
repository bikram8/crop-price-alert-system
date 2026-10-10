package com.mandiwatch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Public: prices, best mandi, trends, farmer self-registration and the JSON API.
 * Admin only: data import, editing mandis, the SMS outbox and the H2 console.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/import/**", "/notifications", "/h2-console/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/mandis/**").hasRole("ADMIN")
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .formLogin(Customizer.withDefaults())
                .logout(logout -> logout.logoutSuccessUrl("/"))
                // The JSON API is read-only (GET), so CSRF is irrelevant there.
                // The H2 console posts its own forms without our CSRF token.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/h2-console/**"))
                // The H2 console is built from frames on the same origin.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    /** Stores bcrypt hashes ("{bcrypt}$2a$..."), and still accepts other formats tagged with their id. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * One admin account from configuration. The plain password from application.yml / env
     * is hashed once at startup; only the hash is kept in memory.
     */
    @Bean
    public UserDetailsService users(MandiProperties properties, PasswordEncoder encoder) {
        MandiProperties.Admin admin = properties.admin();
        return new InMemoryUserDetailsManager(User.withUsername(admin.username())
                .password(encoder.encode(admin.password()))
                .roles("ADMIN")
                .build());
    }
}
