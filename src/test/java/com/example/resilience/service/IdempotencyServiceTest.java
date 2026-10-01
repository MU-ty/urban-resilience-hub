package com.example.resilience.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IdempotencyServiceTest {
    @Test
    void shouldRejectDuplicateRequest() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        var service = new IdempotencyService(redis);
        assertThatThrownBy(() -> service.acquire("citizen", "same-key"))
                .hasMessageContaining("已经提交");
    }
}
