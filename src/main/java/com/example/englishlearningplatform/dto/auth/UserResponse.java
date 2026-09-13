package com.example.englishlearningplatform.dto.auth;

import com.example.englishlearningplatform.entity.User;

public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String role;
    private String avatarUrl;

    public static UserResponse from(User user) {
        UserResponse dto = new UserResponse();
        dto.id = user.getId();
        dto.username = user.getUsername();
        dto.email = user.getEmail();
        dto.role = user.getRole().name();
        dto.avatarUrl = user.getAvatarUrl();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}