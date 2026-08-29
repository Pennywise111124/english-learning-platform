package com.example.englishchat.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// TODO(bạn code phần này): dùng thư viện jjwt (đã hướng dẫn thêm vào pom.xml)
// import io.jsonwebtoken.*;
// import io.jsonwebtoken.security.Keys;
// import javax.crypto.SecretKey;
// import java.util.Date;

@Component
public class JwtUtil {

    // Đọc từ application.yml — KHÔNG hardcode secret trong code
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    /**
     * TODO: Sinh Access Token cho user.
     * Gợi ý cấu trúc:
     *   - subject: username hoặc userId
     *   - claim thêm: role (để lấy quyền mà không cần query DB lại)
     *   - issuedAt: now
     *   - expiration: now + accessTokenExpirationMs
     *   - ký bằng secret (HS256), dùng Keys.hmacShaKeyFor(secret.getBytes())
     */
    public String generateAccessToken(String username, String role) {
        // TODO: implement
        throw new UnsupportedOperationException("TODO: implement generateAccessToken");
    }

    /**
     * TODO: Sinh Refresh Token.
     * Lưu ý: Refresh Token nên "đơn giản" hơn Access Token (không cần chứa role,
     * chỉ cần subject + expiration dài hơn nhiều, VD 7-30 ngày so với Access Token ~15-30 phút).
     * Sau khi sinh xong chuỗi token, nhớ LƯU vào bảng refresh_tokens (qua RefreshTokenRepository)
     * để có thể revoke sau này — đây là lý do bảng refresh_tokens tồn tại thay vì chỉ dựa vào JWT tự hết hạn.
     */
    public String generateRefreshToken(String username) {
        // TODO: implement
        throw new UnsupportedOperationException("TODO: implement generateRefreshToken");
    }

    /**
     * TODO: Parse username (subject) ra khỏi token.
     * Dùng để biết request này thuộc user nào — chỗ này sẽ dùng trong JwtAuthenticationFilter.
     */
    public String extractUsername(String token) {
        // TODO: implement
        throw new UnsupportedOperationException("TODO: implement extractUsername");
    }

    /**
     * TODO: Kiểm tra token có hợp lệ không (chữ ký đúng + chưa hết hạn).
     * Bắt exception (ExpiredJwtException, SignatureException...) và trả về false
     * thay vì để exception văng ra ngoài — tránh lộ raw exception ra response (đúng FR-2.6 tinh thần chung).
     */
    public boolean validateToken(String token) {
        // TODO: implement
        throw new UnsupportedOperationException("TODO: implement validateToken");
    }
}
