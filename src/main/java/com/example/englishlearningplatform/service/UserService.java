package com.example.englishlearningplatform.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.englishlearningplatform.dto.auth.ChangePasswordRequest;
import com.example.englishlearningplatform.dto.auth.UserResponse;
import com.example.englishlearningplatform.entity.User;
import com.example.englishlearningplatform.event.FileDeletionEvent;
import com.example.englishlearningplatform.exception.ResourceNotFoundException;
import com.example.englishlearningplatform.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, FileStorageService fileStorageService,
            ApplicationEventPublisher eventPublisher, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.eventPublisher = eventPublisher;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getMyProfile() {
        return UserResponse.from(getCurrentUser());
    }

    @Transactional
    public UserResponse updateMyAvatar(MultipartFile file) {
        User user = getCurrentUser();
        String oldAvatarUrl = user.getAvatarUrl();

        String avatarUrl = fileStorageService.storeImage(file, "avatars");
        user.setAvatarUrl(avatarUrl);
        User updatedUser = userRepository.save(user);

        if (oldAvatarUrl != null && !oldAvatarUrl.isBlank()) {
            eventPublisher.publishEvent(new FileDeletionEvent(oldAvatarUrl));
        }

        return UserResponse.from(updatedUser);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect!");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }
}