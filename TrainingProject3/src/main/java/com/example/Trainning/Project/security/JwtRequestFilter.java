package com.example.Trainning.Project.security;

import com.example.Trainning.Project.service.CustomUserDetailsService;
import com.example.Trainning.Project.service.RateLimitService; // Import RateLimitService
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus; // Import HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class); // Initialize logger

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private RateLimitService rateLimitService; // Inject RateLimitService

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String requestUri = request.getRequestURI();
        String remoteAddr = request.getRemoteAddr();
        String method = request.getMethod();

        if (requestUri.startsWith("/api/")&& !requestUri.startsWith("/api/user/info")&&!requestUri.startsWith("/api/admin/")) {
            if (!rateLimitService.allowRequest(remoteAddr)) {
                logger.warn("Rate limit exceeded for IP: {} on URI: {}", remoteAddr, requestUri);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // Set 429 status code
                response.getWriter().write("Too many requests. Please try again later.");
                return;
            }
            logger.debug("Rate limit checked for IP: {} on URI: {}. Remaining requests within limit.", remoteAddr, requestUri);
        }


        logger.info("Request received: Method={}, URI={}, From IP={}", method, requestUri, remoteAddr);

        final String requestTokenHeader = request.getHeader("Authorization");

        String username = null;
        String jwtToken = null;

        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                username = jwtTokenUtil.getUsernameFromToken(jwtToken);
                logger.debug("Extracted Username = {}", username);
            } catch (ExpiredJwtException e) {
                logger.warn("JWT Token has expired for request URI: {}. Error: {}", requestUri, e.getMessage());
                // Consider adding a specific response or status for expired tokens if not handled by other filters
            } catch (Exception e) {
                logger.error("Error parsing JWT Token for request URI: {}. Error: {}", requestUri, e.getMessage());
            }
        } else {
            logger.debug("No Authorization header or does not begin with 'Bearer ' for request URI: {}", requestUri);
        }

        // Only authenticate if username is found and no existing authentication in SecurityContext
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            // Re-validate token with UserDetails for robustness
            if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

                logger.info("Set SecurityContext for user: {} (Authorities: {})", username, userDetails.getAuthorities());
            } else {
                logger.warn("JWT Token validation failed for user: {} (URI: {})", username, requestUri);
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Request processed: Method={}, URI={}, Status={}, Duration={}ms",
                    method, requestUri, response.getStatus(), duration);
        }
    }
}