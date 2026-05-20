package com.waterpark.tickershow.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(eh -> eh.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .authorizeHttpRequests(auth -> auth

                // ── Auth ──────────────────────────────────────────────────────────
                .requestMatchers("/auth/**").permitAll()

                // ── Show Types (read-only, public) ────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/show-types").permitAll()

                // ── Shows (public browse) ─────────────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/shows").permitAll()
                .requestMatchers(HttpMethod.GET, "/shows/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/shows/search").permitAll()
                // Operator/Admin manage shows
                .requestMatchers(HttpMethod.GET, "/shows/my").hasAnyRole("OPERATOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/shows").hasAnyRole("OPERATOR", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/shows/{id}").hasAnyRole("OPERATOR", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/shows/{id}/draft").hasAnyRole("OPERATOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/shows/{id}/submit").hasAnyRole("OPERATOR", "ADMIN")
                // Manager workflow
                .requestMatchers(HttpMethod.GET, "/shows/manage/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/shows/{id}/review").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/shows/{id}/publish").hasAnyRole("MANAGER", "ADMIN")

                // ── Venues (public browse, manager CRUD) ──────────────────────────
                .requestMatchers(HttpMethod.GET, "/venues").permitAll()
                .requestMatchers(HttpMethod.GET, "/venues/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/venues/all").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/venues").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/venues/{id}").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/venues/{id}/toggle-status").hasAnyRole("MANAGER", "ADMIN")

                // ── Zones (public browse, manager CRUD) ───────────────────────────
                .requestMatchers(HttpMethod.GET, "/venues/{venueId}/zones").permitAll()
                .requestMatchers(HttpMethod.GET, "/venues/{venueId}/zones/{zoneId}").permitAll()
                .requestMatchers(HttpMethod.POST, "/venues/{venueId}/zones").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/venues/{venueId}/zones/{zoneId}").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/venues/{venueId}/zones/{zoneId}").hasAnyRole("MANAGER", "ADMIN")

                // ── Schedules (public browse) ─────────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/schedules/show/{showId}/available").permitAll()
                .requestMatchers(HttpMethod.GET, "/schedules/show/{showId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/schedules/{id}").permitAll()
                // Operator manage schedules
                .requestMatchers(HttpMethod.POST, "/schedules").hasAnyRole("OPERATOR", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/schedules/{id}").hasAnyRole("OPERATOR", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/schedules/{id}/cancel").hasAnyRole("OPERATOR", "MANAGER", "ADMIN")
                // Manager review schedules
                .requestMatchers(HttpMethod.GET, "/schedules/manage/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/schedules/show/{showId}/pending").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/schedules/{id}/review").hasAnyRole("MANAGER", "ADMIN")

                // ── User management ───────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/users").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/users").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/users/{id}/status").hasAnyRole("MANAGER", "ADMIN")

                // ── Role & Permission — ADMIN only ────────────────────────────────
                .requestMatchers("/roles/**").hasRole("ADMIN")
                .requestMatchers("/permissions/**").hasRole("ADMIN")

                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
