package com.example.englishchat.dto.auth;

import com.example.englishchat.entity.User;

// DTO riêng cho response — KHÔNG bao giờ trả trực tiếp Entity User ra ngoài
// (Entity có passwordHash, không được lộ ra API — đúng nguyên tắc Layered Architecture + DTO)
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String role;

    public static UserResponse from(User user) {
        UserResponse dto = new UserResponse();
        dto.id = user.getId();
        dto.username = user.getUsername();
        dto.email = user.getEmail();
        dto.role = user.getRole().name();
        return dto;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}
