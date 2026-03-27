package com.cap.identity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

        // Gateway already validated JWT
        // Just read the headers Gateway injected
        String userId = request.getHeader("X-User-Id");
        String role   = request.getHeader("X-User-Role");

        // if headers present — set authentication
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

        // continue regardless — SecurityConfig
        // decides if route needs auth or not
        filterChain.doFilter(request, response);
    }
}