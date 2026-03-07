package com.financemanagerai.file_processing_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.*;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // Allow preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public endpoints
                        .requestMatchers("/public/**").permitAll()

                        // Swagger docs
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Admin endpoints (use hasAuthority since Keycloak sends "ADMIN" not "ROLE_ADMIN")
                        .requestMatchers("/admin/**").hasAnyAuthority("ADMIN", "ROLE_ADMIN")

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                ));

        return http.build();
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();

        return jwt -> {
            Set<GrantedAuthority> authorities = new HashSet<>(scopesConverter.convert(jwt));

            // Extract role from direct claim
            addRole(authorities, jwt.getClaimAsString("role"));

            // Extract roles from realm_access (Keycloak)
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null) {
                Object roles = realmAccess.get("roles");
                if (roles instanceof Collection<?> roleCollection) {
                    roleCollection.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .forEach(role -> addRole(authorities, role));
                }
            }

            String principalName = jwt.getClaimAsString("preferred_username");
            return new JwtAuthenticationToken(
                    jwt,
                    authorities,
                    principalName != null ? principalName : jwt.getSubject()
            );
        };
    }

    private static void addRole(Set<GrantedAuthority> authorities, String role) {
        if (role == null || role.isBlank()) {
            return;
        }

        // Normalize role: add both with and without ROLE_ prefix for compatibility
        String normalized = role.startsWith("ROLE_") ? role.substring(5) : role;
        authorities.add(new SimpleGrantedAuthority(normalized));
        authorities.add(new SimpleGrantedAuthority("ROLE_" + normalized));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:3001",
                "http://localhost:8080",
                "http://localhost:8081",
                "http://localhost:8082"
        ));
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"
        ));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}