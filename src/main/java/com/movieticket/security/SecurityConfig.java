package com.movieticket.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Central Spring Security configuration.
 *
 * <ul>
 *   <li>Public users can browse cities, theaters, shows, and show seats.</li>
 *   <li>Only ADMIN users can call paths below {@code /api/admin}.</li>
 *   <li>Every other endpoint requires HTTP Basic authentication.</li>
 * </ul>
 *
 * <p>HTTP Basic was chosen because this take-home only requires basic role-based access
 * control. The client sends an email and password with every protected request. Spring loads
 * the user from the database and compares the password with its BCrypt hash. This avoids JWT
 * token generation and filtering while still identifying the booking owner correctly.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // This is a stateless REST API, so it does not use browser forms or CSRF tokens.
                .csrf(AbstractHttpConfigurer::disable)
                // H2's development console uses an iframe from the same application.
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/cities/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/theaters/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/shows/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Booking endpoints fall through to this rule: both authenticated
                        // customers and admins have a valid identity, while anonymous users do not.
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                // Enables: Authorization: Basic base64(email:password)
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        // Load the user by email from the database and compare the supplied password
        // against its BCrypt hash during login.
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
