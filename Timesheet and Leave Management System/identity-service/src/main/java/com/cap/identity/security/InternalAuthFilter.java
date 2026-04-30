package com.cap.identity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class InternalAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String role   = request.getHeader("X-User-Role");

        if (userId != null && role != null
                && !userId.isBlank() && !role.isBlank()) {

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority(
                                    "ROLE_" + role))
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/auth/login") ||
               path.equals("/auth/signup") ||
               path.equals("/auth/forgot-password") ||
               path.equals("/auth/reset-password") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/actuator/health");
    }
}