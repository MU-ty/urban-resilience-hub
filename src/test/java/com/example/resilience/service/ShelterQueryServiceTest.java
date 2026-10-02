package com.example.resilience.service;

import com.example.resilience.domain.Shelter;
import com.example.resilience.repository.ShelterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ShelterQueryServiceTest {
    @Test
    @SuppressWarnings("unchecked")
    void coordinatesSurviveCacheRoundTrip() {
        var repository = mock(ShelterRepository.class);
        var redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        var station = mock(Shelter.class);
        when(station.getId()).thenReturn(1L);
        when(station.getName()).thenReturn("象山|共享站");
        when(station.getDistrict()).thenReturn("象山区");
        when(station.getCapacity()).thenReturn(500);
        when(station.getOccupancy()).thenReturn(120);
        when(station.getLatitude()).thenReturn(new BigDecimal("25.2600000"));
        when(station.getLongitude()).thenReturn(new BigDecimal("110.2800000"));
        when(repository.findByDistrictOrderByOccupancyAsc("象山区")).thenReturn(List.of(station));
        var service = new ShelterQueryService(repository, redis);
        var fromDatabase = service.byDistrict("象山区");
        var encoded = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(values).set(eq("shelters:v2:district:象山区"), encoded.capture(), any(Duration.class));
        when(values.get("shelters:v2:district:象山区")).thenReturn(encoded.getValue());
        assertThat(service.byDistrict("象山区")).isEqualTo(fromDatabase);
        assertThat(fromDatabase.getFirst().latitude()).isEqualByComparingTo("25.26");
        verify(repository, times(1)).findByDistrictOrderByOccupancyAsc("象山区");
    }
}
