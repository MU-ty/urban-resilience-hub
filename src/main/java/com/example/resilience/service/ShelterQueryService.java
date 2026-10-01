package com.example.resilience.service;

import com.example.resilience.domain.Shelter;
import com.example.resilience.repository.ShelterRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.util.*;

@Service
public class ShelterQueryService {
    private final ShelterRepository shelters;
    private final StringRedisTemplate redis;
    public ShelterQueryService(ShelterRepository shelters, StringRedisTemplate redis) {
        this.shelters = shelters; this.redis = redis;
    }

    @Transactional(readOnly = true)
    public List<ShelterView> byDistrict(String district) {
        String key = "shelters:district:" + district;
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) return decode(cached); // cache-aside 命中
        } catch (RuntimeException ignored) { }

        List<ShelterView> result = shelters.findByDistrictOrderByOccupancyAsc(district).stream()
                .map(s -> new ShelterView(s.getId(), s.getName(), s.getDistrict(), s.getCapacity(), s.getOccupancy()))
                .toList();
        try {
            // 空结果也短暂缓存，防缓存穿透；TTL 加随机抖动，避免雪崩。
            redis.opsForValue().set(key, encode(result), Duration.ofSeconds(270 + new Random().nextInt(61)));
        } catch (RuntimeException ignored) { }
        return result;
    }

    private String encode(List<ShelterView> values) {
        return values.stream().map(v -> "%d|%s|%s|%d|%d".formatted(v.id(), escape(v.name()), escape(v.district()), v.capacity(), v.occupancy()))
                .collect(java.util.stream.Collectors.joining("\n"));
    }
    private List<ShelterView> decode(String value) {
        if (value.isEmpty()) return List.of();
        return Arrays.stream(value.split("\n")).map(line -> line.split("\\|", -1))
                .map(p -> new ShelterView(Long.parseLong(p[0]), unescape(p[1]), unescape(p[2]), Integer.parseInt(p[3]), Integer.parseInt(p[4])))
                .toList();
    }
    private String escape(String s) { return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    private String unescape(String s) { return new String(Base64.getUrlDecoder().decode(s), java.nio.charset.StandardCharsets.UTF_8); }
    public record ShelterView(Long id, String name, String district, int capacity, int occupancy) {}
}
