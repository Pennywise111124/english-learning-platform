package com.example.englishchat.service;

import com.example.englishchat.dto.auth.*;
import com.example.englishchat.entity.RefreshToken;
import com.example.englishchat.entity.Role;
import com.example.englishchat.entity.User;
import com.example.englishchat.repository.RefreshTokenRepository;
import com.example.englishchat.repository.UserRepository;
import com.example.englishchat.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        PasswordEncoder passwordEncoder,
                        JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * TODO: implement theo các bước:
     * 1. Kiểm tra username/email đã tồn tại chưa (userRepository.existsByUsername/existsByEmail)
     *    → nếu có, ném exception phù hợp (VD: IllegalArgumentException hoặc custom exception,
     *    để GlobalExceptionHandler xử lý trả về status code đúng, VD 409 Conflict)
     * 2. Hash password bằng passwordEncoder.encode(rawPassword) — KHÔNG BAO GIỜ lưu password gốc
     * 3. Tạo User mới với role mặc định = Role.USER (đúng FR-1.1)
     * 4. Lưu vào DB qua userRepository.save(...)
     */
    public void register(RegisterRequest request) {
        // TODO: implement
        throw new UnsupportedOperationException("TODO: implement register");
    }

    /**
     * TODO: implement theo các bước:
     * 1. Tìm User theo username (userRepository.findByUsername), nếu không có → lỗi xác thực
     * 2. Kiểm tra password: passwordEncoder.matches(rawPassword, user.getPasswordHash())
     *    nếu sai → lỗi xác thực (KHÔNG nói rõ "sai username" hay "sai password" riêng biệt,
     *    tránh lộ thông tin username có tồn tại hay không — nguyên tắc tương tự 404 ở mục 2.3 Requirements)
     * 3. Sinh accessToken + refreshToken qua jwtUtil
     * 4. Lưu refreshToken vào DB (tạo RefreshToken entity, set expiresAt = now + refreshTokenExpirationMs)
     * 5. Trả về AuthResponse(accessToken, refreshToken, UserResponse.from(user))
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // TODO: implement
        throw new UnsupportedOperationException("TODO: implement login");
    }

    /**
     * TODO: implement theo các bước — dùng khi Access Token hết hạn, đổi lấy Access Token mới
     * mà không cần đăng nhập lại:
     * 1. Tìm RefreshToken trong DB theo chuỗi token (refreshTokenRepository.findByToken)
     * 2. Kiểm tra: tồn tại, chưa bị revoke, chưa hết hạn (so sánh expiresAt với LocalDateTime.now())
     *    → nếu vi phạm điều kiện nào, ném lỗi xác thực (bắt buộc đăng nhập lại)
     * 3. Lấy User liên quan, sinh Access Token mới
     * 4. (Tùy chọn nâng cao) Cân nhắc xoay vòng refresh token: revoke token cũ, sinh token mới —
     *    để lại tìm hiểu thêm nếu muốn, MVP có thể tái sử dụng refresh token cũ tới khi hết hạn
     */
    @Transactional
    public String refreshAccessToken(String refreshTokenValue) {
        // TODO: implement
        throw new UnsupportedOperationException("TODO: implement refreshAccessToken");
    }
}
