package com.example.resilience.config;

import com.example.resilience.security.JwtAuthenticationFilter;
import com.example.resilience.security.RateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {
    @Bean FilterRegistrationBean<JwtAuthenticationFilter> disableJwtAutoRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> bean = new FilterRegistrationBean<>(filter);
        bean.setEnabled(false);
        return bean;
    }
    @Bean FilterRegistrationBean<RateLimitFilter> disableRateLimitAutoRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> bean = new FilterRegistrationBean<>(filter);
        bean.setEnabled(false);
        return bean;
    }
}
