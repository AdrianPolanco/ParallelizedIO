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
        System.out.println("Intercepting request to: " + request.getRequestURI());
        if(!request.getRequestURI().contains("/webhook")) {
            System.out.println("Not a webhook request, proceeding without checks.");
            return true; // Not a webhook request, proceed normally
        }

        String authHeader = request.getHeader("Authorization");

        if(authHeader == null || !authHeader.equals("Bearer " + webhookToken)) {
            System.out.println("Unauthorized webhook request.");
            System.out.println("Expected token: " + webhookToken);
            System.out.println("Received token: " + authHeader);
            System.out.println("Headers: " + request.getHeaderNames());
            System.out.println("Full request: " + request);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Unauthorized");
            return false; // Unauthorized
        }

        System.out.println("Authorized webhook request.");
        return true; // Authorized
    }
}
