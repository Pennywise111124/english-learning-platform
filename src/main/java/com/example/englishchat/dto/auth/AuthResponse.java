package com.example.englishchat.dto.auth;

// Khớp đúng shape đã chốt trong FE_Handoff_Brief.md mục 8:
// { accessToken, refreshToken, user: {...} }
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private UserResponse user;

    public AuthResponse(String accessToken, String refreshToken, UserResponse user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public UserResponse getUser() { return user; }
}
