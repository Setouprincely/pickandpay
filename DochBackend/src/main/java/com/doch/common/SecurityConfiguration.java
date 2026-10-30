package com.doch.common;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.doch.User.service.UserDetailServiceImp;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    @Autowired
    private UserDetailServiceImp userDetailService;
    @Autowired
    private JWTFilter jwtFilter;
    @Autowired
    private AuthRateLimitFilter authRateLimitFilter;

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(userDetailService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        return daoAuthenticationProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests( request -> request.requestMatchers("/api/auth/**", "/api/media/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                    // Add the WebSocket tracking path to permitAll
                    // We use /** because SockJS adds suffixes like /info and /websocket
                    .requestMatchers("/ws-tracking/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/tracking/**").permitAll()

                    // PUBLIC WEBHOOKS for Fapshi
                    .requestMatchers(HttpMethod.POST, "/api/payments/webhook/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/roles", "/api/roles/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/product/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/category/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/permissions", "/api/permissions/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/users/sellers/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/product/seller/**").permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated())
            .sessionManagement(manager -> manager.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, e) -> {
                res.setStatus(401);
                res.setContentType("application/json");
                res.getWriter().write("{\"success\":false,\"statusCode\":401,\"errorType\":\"Unauthorized\",\"message\":\"Authentication required. Please log in.\"}");
            }))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(authRateLimitFilter, JWTFilter.class);
            return http.build();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception{
        return authenticationConfiguration.getAuthenticationManager();
    }

}
