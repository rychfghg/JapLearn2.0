package japlearn.demo.Config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    // Comma separated allow-list, see application.properties / the
    // ALLOWED_ORIGINS env var. Only these origins get CORS headers back;
    // everything else is silently refused by the browser before it can read
    // a response. This replaces the old "*" wildcard that used to live on
    // UserController alone (and did not cover any other controller).
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // This is a stateless JSON API — no server-side session cookie is
            // ever issued or read, so there is nothing for a CSRF token to
            // protect. The real cross-origin defense is the CORS allow-list
            // below, plus the per-endpoint rate limiting in RateLimitingFilter.
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .headers(headers -> headers
                // Clickjacking protection: this API's responses must never be framed.
                .frameOptions(frame -> frame.deny())
                // Stops browsers from MIME-sniffing responses into executable content.
                .contentTypeOptions(Customizer.withDefaults())
                // Forces HTTPS for a year, including subdomains, once a browser has
                // seen it once over HTTPS (Render/Vercel already terminate TLS).
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                // Defense in depth for the one endpoint that returns raw HTML
                // (/api/users/confirm) — blocks it from ever pulling in or
                // running third-party scripts if that response is ever framed
                // or embedded somewhere unexpected.
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(
                        "default-src 'self'; " +
                        "img-src 'self' https: data:; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "script-src 'none'; " +
                        "frame-ancestors 'none'"))
            )
            .authorizeHttpRequests(auth -> auth
                // NOTE: this backend has no session/JWT auth layer yet, so every
                // controller (game content, scores, user management, etc.) is
                // still reachable without a login. Locking specific endpoints
                // down further needs the RN app, the admin portal, and this
                // config to move together — see RateLimitingFilter and
                // UserService for the mitigations that ARE safe to ship without
                // breaking the existing clients (mass-assignment / privilege
                // escalation fix, reset-token expiry, brute-force throttling).
                .anyRequest().permitAll()
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        // No cookies/sessions are used, so credentialed CORS is unnecessary
        // and left off to keep the policy as tight as possible.
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
