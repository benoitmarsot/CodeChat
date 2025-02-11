package com.unbumpkin.codechat.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class IpWhitelistFilter extends OncePerRequestFilter {

    private static final List<String> ALLOWED_IPS = Arrays.asList(
        "127.0.0.1", // localhost
        "0:0:0:0:0:0:0:1" // localhost IPv6
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {
        String clientIp = request.getRemoteAddr();
        //logger.info("Client IP: " + clientIp);

        if (isAllowed(clientIp)) {
            filterChain.doFilter(request, response);
        } else {
            logger.warn("Access denied for IP: " + clientIp);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
        }
    }

    private boolean isAllowed(String clientIp) {
        return ALLOWED_IPS.contains(clientIp);
    }
}