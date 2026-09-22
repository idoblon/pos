package com.springboot.POS.configuration;

import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.web.client.RestTemplate;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    // Comma separated list of allowed frontend origins, e.g.
    // CORS_ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com
    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthRateLimitFilter authRateLimitFilter) throws Exception {

        return http
                .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "script-src 'self' https://js.stripe.com blob:; " +
                                "frame-src https://js.stripe.com https://hooks.stripe.com; " +
                                "connect-src 'self' https://api.stripe.com"
                        ))
                )
                .authorizeHttpRequests(Authorize -> Authorize
                        .requestMatchers("/api/super-admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/public/**").permitAll()  // Public endpoints like store registration
                        .requestMatchers("/auth/forgot-password", "/auth/reset-password", "/auth/refresh").permitAll()
                        .requestMatchers("/api/restock-requests/**").authenticated()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(authRateLimitFilter, BasicAuthenticationFilter.class)
                .addFilterBefore(new JwtValidator(), BasicAuthenticationFilter.class)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(
                        cors -> cors.configurationSource(corsConfigurationSource())
                ).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        return new CorsConfigurationSource() {
            @Override
            public @Nullable CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                CorsConfiguration cfg = new CorsConfiguration();
                List<String> origins = Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
                cfg.setAllowedOrigins(origins);
                cfg.setAllowedMethods(Collections.singletonList("*"));
                cfg.setAllowCredentials(true);
                cfg.setAllowedHeaders(Collections.singletonList("*"));
                cfg.setExposedHeaders(Arrays.asList("Authorization"));
                cfg.setMaxAge(3600L);
                return cfg;
            }
         };
  }
}
