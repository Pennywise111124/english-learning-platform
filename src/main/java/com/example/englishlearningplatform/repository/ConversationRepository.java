package com.example.englishlearningplatform.repository;

import com.example.englishlearningplatform.entity.Conversation;
import com.example.englishlearningplatform.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Page<Conversation> findByUser(User user, Pageable pageable);

    Optional<Conversation> findByIdAndUser(Long id, User user);
}