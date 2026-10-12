package com.example.englishlearningplatform.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.englishlearningplatform.config.RateLimitProperties;
import com.example.englishlearningplatform.dto.auth.*;
import com.example.englishlearningplatform.ratelimit.RateLimitService;
import com.example.englishlearningplatform.service.AuthService;
import com.example.englishlearningplatform.util.ClientIpResolver;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RateLimitService rateLimitService;
    private final RateLimitProperties rateLimitProperties;

    public AuthController(AuthService authService, RateLimitService rateLimitService,
            RateLimitProperties rateLimitProperties) {
        this.authService = authService;
        this.rateLimitService = rateLimitService;
        this.rateLimitProperties = rateLimitProperties;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        rateLimitService.checkRegister(clientIp(httpRequest));
        authService.register(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        rateLimitService.checkLogin(clientIp(httpRequest));
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@RequestBody Map<String, String> body) {
        String newAccessToken = authService.refreshAccessToken(body.get("refreshToken"));
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    private String clientIp(HttpServletRequest httpRequest) {
        return ClientIpResolver.resolve(httpRequest.getRemoteAddr(),
                httpRequest.getHeader("X-Forwarded-For"), rateLimitProperties.trustedProxy());
    }
}
