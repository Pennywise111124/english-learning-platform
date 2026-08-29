package com.example.englishchat.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Filter này chạy TRƯỚC mọi request, kiểm tra header Authorization: Bearer <token>
// Đây chính là nơi thực thi FR-2.7/2.2 (ownership qua JWT) ở tầng thấp nhất — mọi API phía sau
// đều tin tưởng vào SecurityContext đã được filter này thiết lập đúng.
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // TODO: implement theo các bước sau:
        // 1. Đọc header "Authorization" từ request
        // 2. Nếu không có, hoặc không bắt đầu bằng "Bearer ", cho request đi tiếp
        //    (filterChain.doFilter(request, response)) rồi return — đây có thể là endpoint public
        //    (VD: /api/auth/login) không cần token
        // 3. Nếu có, cắt lấy phần token sau "Bearer "
        // 4. Dùng jwtUtil.validateToken(token) kiểm tra hợp lệ
        // 5. Nếu hợp lệ: lấy username qua jwtUtil.extractUsername(token),
        //    load UserDetails qua userDetailsService, tạo Authentication object,
        //    set vào SecurityContextHolder.getContext().setAuthentication(...)
        // 6. Luôn gọi filterChain.doFilter(request, response) ở cuối để request tiếp tục
        //    (kể cả khi token không hợp lệ — để SecurityConfig phía sau tự quyết định chặn hay không,
        //    không tự ý trả lỗi ở đây)

        filterChain.doFilter(request, response); // TODO: xóa dòng này sau khi implement xong logic thật
    }
}
