package com.doch.common;

import java.io.IOException;

import jakarta.annotation.Nonnull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.doch.User.service.JWTUtils;
import com.doch.User.service.UserDetailServiceImp;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Component
public class JWTFilter extends OncePerRequestFilter{

    private final JWTUtils jwtUtils;

    private final UserDetailServiceImp userService;

    public JWTFilter(JWTUtils jwtService, UserDetailServiceImp userDetailsService) {
        this.jwtUtils = jwtService;
        this.userService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request,response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            String username = jwtUtils.extractUserName(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userService.loadUserByUsername(username);

                // Disabled / suspended accounts lose access immediately, even with an unexpired token.
                if (userDetails.isEnabled() && jwtUtils.isTokenValide(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException
                 | org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            // Expired, malformed or orphaned token: continue unauthenticated. Protected endpoints answer 401/403,
            // public endpoints (catalogue, tracking) keep working.
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);

    }
    
}
