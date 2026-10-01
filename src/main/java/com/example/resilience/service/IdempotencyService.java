package com.example.resilience.service;

import com.example.resilience.common.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class IdempotencyService {
    private final StringRedisTemplate redis;
    public IdempotencyService(StringRedisTemplate redis) { this.redis = redis; }

    public Lease acquire(String scope, String key) {
        if (key == null || key.isBlank() || key.length() > 100) {
            throw new BusinessException("IDEMPOTENCY_KEY_REQUIRED", "需要有效的 Idempotency-Key 请求头", HttpStatus.BAD_REQUEST);
        }
        String redisKey = "idem:" + scope + ":" + key;
        String owner = java.util.UUID.randomUUID().toString();
        Boolean acquired = redis.opsForValue().setIfAbsent(redisKey, owner, Duration.ofMinutes(2));
        if (!Boolean.TRUE.equals(acquired)) throw BusinessException.conflict("该请求正在处理或已经提交");
        return new Lease(redis, redisKey, owner);
    }

    public static final class Lease implements AutoCloseable {
        private final StringRedisTemplate redis;
        private final String key;
        private final String owner;
        private boolean committed;
        Lease(StringRedisTemplate redis, String key, String owner) { this.redis = redis; this.key = key; this.owner = owner; }
        public void commit(String resourceId) {
            redis.execute(new org.springframework.data.redis.core.script.DefaultRedisScript<Long>(
                    "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('set',KEYS[1],ARGV[2],'EX',86400) and 1 else return 0 end", Long.class),
                    java.util.List.of(key), owner, "DONE:" + resourceId);
            committed = true;
        }
        @Override public void close() {
            if (!committed) redis.execute(new org.springframework.data.redis.core.script.DefaultRedisScript<Long>(
                    "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end", Long.class),
                    java.util.List.of(key), owner);
        }
    }
}
