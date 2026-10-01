package com.example.resilience.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JwtTokenService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final Pattern CLAIMS = Pattern.compile("\\{\"sub\":\"([A-Za-z0-9_.@-]{1,50})\",\"role\":\"([A-Z_]{1,30})\",\"exp\":(\\d+)}");
    private final byte[] secret;
    private final Duration ttl;

    public JwtTokenService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.ttl}") Duration ttl) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) throw new IllegalArgumentException("JWT secret 至少 32 字节");
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttl = ttl;
    }

    public String create(String username, String role) {
        String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        long exp = Instant.now().plus(ttl).getEpochSecond();
        String payload = encode("{\"sub\":\"%s\",\"role\":\"%s\",\"exp\":%d}".formatted(username, role, exp));
        String unsigned = header + "." + payload;
        return unsigned + "." + ENCODER.encodeToString(hmac(unsigned));
    }

    public Optional<Principal> verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return Optional.empty();
            if (!parts[0].equals(encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}"))) return Optional.empty();
            String unsigned = parts[0] + "." + parts[1];
            if (!java.security.MessageDigest.isEqual(hmac(unsigned), DECODER.decode(parts[2]))) return Optional.empty();
            Matcher matcher = CLAIMS.matcher(new String(DECODER.decode(parts[1]), StandardCharsets.UTF_8));
            if (!matcher.matches()) return Optional.empty();
            long exp = Long.parseLong(matcher.group(3));
            if (Instant.now().getEpochSecond() >= exp) return Optional.empty();
            return Optional.of(new Principal(matcher.group(1), matcher.group(2), Instant.ofEpochSecond(exp)));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private String encode(String value) { return ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8)); }
    private byte[] hmac(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) { throw new IllegalStateException("无法签发令牌", e); }
    }

    public record Principal(String username, String role, Instant expiresAt) {}
}
