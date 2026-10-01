package com.example.resilience.config;

import com.example.resilience.security.JwtAuthenticationFilter;
import com.example.resilience.security.RateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return PasswordEncoderFactories.createDelegatingPasswordEncoder(); }

    @Bean UserDetailsService userDetailsService(com.example.resilience.repository.UserRepository users) {
        return username -> users.findByUsername(username)
                .map(user -> User.withUsername(user.getUsername()).password(user.getPasswordHash())
                        .roles(user.getRole()).disabled(!user.isEnabled()).build())
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    @Bean AuthenticationManager authenticationManager(UserDetailsService service, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(service);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean SecurityFilterChain security(HttpSecurity http, JwtAuthenticationFilter jwt, RateLimitFilter rateLimit) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401); response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"请先登录\"}");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403); response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"没有操作权限\"}");
                        }))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/assets/**", "/favicon.svg",
                                "/api/auth/login", "/actuator/health", "/error").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimit, JwtAuthenticationFilter.class)
                .build();
    }
}
