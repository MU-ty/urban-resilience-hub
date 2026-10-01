package com.example.resilience.security;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {
    private final JwtTokenService service = new JwtTokenService(
            "a-test-secret-that-is-definitely-longer-than-32-bytes", Duration.ofMinutes(5));

    @Test
    void shouldCreateAndVerifyToken() {
        String token = service.create("citizen", "CITIZEN");
        var principal = service.verify(token);
        assertThat(principal).isPresent();
        assertThat(principal.orElseThrow().username()).isEqualTo("citizen");
        assertThat(principal.orElseThrow().role()).isEqualTo("CITIZEN");
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = service.create("citizen", "CITIZEN");
        assertThat(service.verify(token.substring(0, token.length() - 2) + "xx")).isEmpty();
    }
}
