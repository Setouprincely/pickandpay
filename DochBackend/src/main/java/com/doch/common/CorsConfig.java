package com.doch.common;

// import java.util.Arrays;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Value("${application.cors.allowed-origin-patterns:http://localhost:3000,http://localhost:5173,http://localhost:5174}")
    private String allowedOriginPatterns;

    @Bean
    public CorsFilter corsFilter(){
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 1. Allow credentials (cookies/auth headers)
        config.setAllowCredentials(true);

        // Comma-separated origin patterns support local dev ports and deployed frontends.
        config.setAllowedOriginPatterns(Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());

        // 3. ALLOW ALL HEADERS
        config.setAllowedHeaders(List.of("*"));

        // 4. ALLOW ALL HTTP METHODS
        config.setAllowedMethods(List.of("*"));

        config.setMaxAge(3600L);
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
