package com.multichunk.demo.configuration;

import com.multichunk.demo.components.WebhookSecurityInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final WebhookSecurityInterceptor webhookSecurityInterceptor;
    public WebConfig(WebhookSecurityInterceptor webhookSecurityInterceptor) {
        this.webhookSecurityInterceptor = webhookSecurityInterceptor;
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(webhookSecurityInterceptor).addPathPatterns("/files/*/webhook");
    }
}
