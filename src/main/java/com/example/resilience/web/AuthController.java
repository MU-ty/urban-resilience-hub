package com.example.resilience.web;

import com.example.resilience.common.ApiResponse;
import com.example.resilience.security.JwtTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService tokens;
    public AuthController(AuthenticationManager authenticationManager, JwtTokenService tokens) {
        this.authenticationManager = authenticationManager; this.tokens = tokens;
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest body) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(body.username(), body.password()));
        String role = auth.getAuthorities().iterator().next().getAuthority().replaceFirst("^ROLE_", "");
        return ApiResponse.ok(new TokenResponse(tokens.create(auth.getName(), role), "Bearer"));
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record TokenResponse(String accessToken, String tokenType) {}
}
