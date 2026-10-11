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

    @Transactional(readOnly = true)
    public List<UserProgressResponse> getMyProgress() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));

        return userProgressRepository.findByUser_Id(user.getId()).stream()
                .map(UserProgressResponse::from)
                .collect(Collectors.toList());
    }
}