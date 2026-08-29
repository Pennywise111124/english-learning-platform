package com.example.englishchat.controller;

import com.example.englishchat.dto.auth.*;
import com.example.englishchat.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // TODO(bạn tự thêm nếu cần khi test refresh token):
    // @PostMapping("/refresh")
    // public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
    //     String newAccessToken = authService.refreshAccessToken(body.get("refreshToken"));
    //     return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    // }
}
