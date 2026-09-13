package com.example.englishlearningplatform.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.englishlearningplatform.dto.progress.UserProgressResponse;
import com.example.englishlearningplatform.entity.User;
import com.example.englishlearningplatform.exception.ResourceNotFoundException;
import com.example.englishlearningplatform.repository.UserProgressRepository;
import com.example.englishlearningplatform.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserProgressService {

    private final UserProgressRepository userProgressRepository;
    private final UserRepository userRepository;

    public UserProgressService(UserProgressRepository userProgressRepository, UserRepository userRepository) {
        this.userProgressRepository = userProgressRepository;
        this.userRepository = userRepository;
    }

    // FR-5.2: userId LUÔN lấy từ JWT — endpoint này không nhận bất kỳ tham số
    // userId nào từ Controller, đây là lần thứ 3 pattern "lấy user hiện tại từ
    // SecurityContext" lặp lại (đã có ở submitQuiz, getMyAttempts) — đáng cân
    // nhắc tách thành 1 helper dùng chung sau này (VD: @AuthenticationPrincipal
    // custom resolver), nhưng chưa bắt buộc phải làm ngay ở M3.
    @Transactional(readOnly = true)
    public List<UserProgressResponse> getMyProgress() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại!"));

        return userProgressRepository.findByUser_Id(user.getId()).stream()
                .map(UserProgressResponse::from)
                .collect(Collectors.toList());
    }
}