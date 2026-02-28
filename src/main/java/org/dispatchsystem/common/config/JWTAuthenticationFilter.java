package org.dispatchsystem.common.config;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService customUserDetailsService;

    public JWTAuthenticationFilter(JwtUtils jwtUtils, CustomUserDetailsService customUserDetailsService) {
        this.jwtUtils = jwtUtils;
        this.customUserDetailsService = customUserDetailsService;
    }
    @Override

    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return request.getRequestURI().startsWith("/ws");
    }


    @Override

    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1) Check the Authorization header
        System.out.println("request -- "+ request.getRequestURI());
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No JWT in header; just continue the chain (maybe endpoint is permitAll)
            filterChain.doFilter(request, response);
            return;
        }

        // 2) Extract the token
        String token = authHeader.substring(7); // Remove "Bearer "

        try {
            // 3) Validate token; get the email (subject)
            String email = jwtUtils.validateTokenAndGetEmail(token);

            // 4) Load the user from DB
            var userDetails = customUserDetailsService.loadUserByUsername(email);

            // 5) Create an Authentication object
            var authToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            );

            // 6) Store it in the SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authToken);

        } catch (Exception ex) {
            // Token invalid or expired
            System.out.println("JWT Authentication failed: " + ex.getMessage());
            // Optionally set response.setStatus(401) here if you want immediate error
        }

        // Continue filtering
        filterChain.doFilter(request, response);
    }
}