package com.example.resilience.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final StringRedisTemplate redis;
    public RateLimitFilter(StringRedisTemplate redis) { this.redis = redis; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/")) { chain.doFilter(request, response); return; }
        String identity = request.getRemoteAddr();
        long window = Instant.now().getEpochSecond() / 60;
        String key = "rate:" + identity + ":" + window;
        try {
            Long count = redis.execute(new org.springframework.data.redis.core.script.DefaultRedisScript<Long>(
                    "local n=redis.call('incr',KEYS[1]); if n==1 then redis.call('expire',KEYS[1],120) end; return n", Long.class),
                    java.util.List.of(key));
            if (count != null && count > 120) {
                response.setStatus(429);
                response.setHeader("Retry-After", Long.toString(60 - Instant.now().getEpochSecond() % 60));
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"code\":\"RATE_LIMITED\",\"message\":\"请求过于频繁\"}");
                return;
            }
        } catch (RuntimeException ignored) {
            // 限流基础设施故障时 fail-open，核心业务仍可用，并应由 Redis 健康监控告警。
        }
        chain.doFilter(request, response);
    }
}
