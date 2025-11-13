package com.multichunk.demo.components;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class WebhookSecurityInterceptor implements HandlerInterceptor {

    @Value("${minio.webhook-token}")
    private String webhookToken;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if(!request.getRequestURI().contains("/webhook")) {
            return true; // Not a webhook request, proceed normally
        }

        String authHeader = request.getHeader("Authorization");

        if(authHeader == null || !authHeader.equals("Bearer " + webhookToken)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Unauthorized");
            return false; // Unauthorized
        }

        return true; // Authorized
    }
}
