package com.supplybase.partners.config;

import tools.jackson.databind.ObjectMapper;
import com.supplybase.partners.common.web.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

/**
 * Session-cookie auth (HttpOnly, backed by Spring Session JDBC for durability
 * across restarts) with CSRF protection via a readable-by-JS cookie + header
 * pair, the standard pattern for a same-site SPA talking to a Spring Security
 * backend. CORS is restricted to an explicit allowlist. All 401/403 responses
 * are JSON (this is an API, never a login-page redirect).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final String allowedOrigins;

    public SecurityConfig(ObjectMapper objectMapper,
                           @Value("${supplybase.cors.allowed-origins}") String allowedOrigins) {
        this.objectMapper = objectMapper;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookieName("XSRF-TOKEN");
        csrfTokenRepository.setHeaderName("X-XSRF-TOKEN");

        http
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        // Server-to-server webhooks authenticate via signature, not a browser session/CSRF token.
                        .ignoringRequestMatchers("/api/v1/webhooks/**"))
                .addFilterAfter(new CsrfCookieEagerLoadFilter(), CsrfFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/catalog/**").permitAll()
                        .requestMatchers("/api/v1/webhooks/**").permitAll()
                        // Coarse gate: any staff role may reach /admin/**; each admin controller further
                        // restricts its own endpoints via @PreAuthorize (e.g. REVIEWER only touches
                        // verification/screening, TRAINER only training, FINANCE only money).
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "REVIEWER", "TRAINER", "FINANCE")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJsonError(response, HttpStatus.UNAUTHORIZED, "Authentication is required.", request.getRequestURI()))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJsonError(response, HttpStatus.FORBIDDEN, "You do not have permission to perform this action.", request.getRequestURI())));

        return http.build();
    }

    private void writeJsonError(HttpServletResponse response, HttpStatus status, String message, String path) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(), message, path);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    /**
     * Spring Security's CsrfToken is loaded lazily by default: the XSRF-TOKEN
     * cookie is only written once something actually reads the token's value.
     * A pure JSON API never triggers that read on its own, so a SPA's very
     * first GET would come back with no cookie to echo on its first POST.
     * Forcing token.getToken() here (the pattern from Spring Security's own
     * SPA CSRF docs) guarantees the cookie is set on every response.
     */
    private static final class CsrfCookieEagerLoadFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            Object attr = request.getAttribute(CsrfToken.class.getName());
            if (attr instanceof Supplier<?> supplier) {
                supplier.get();
            } else if (attr instanceof CsrfToken token) {
                token.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "Accept"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
