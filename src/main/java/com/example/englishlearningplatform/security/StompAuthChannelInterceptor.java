package com.example.englishlearningplatform.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.security.access.AccessDeniedException;

import com.example.englishlearningplatform.service.ChatService;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final ChatService chatService; // cần cho bước check ownership lúc SUBSCRIBE

    public StompAuthChannelInterceptor(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService,
            ChatService chatService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.chatService = chatService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new AuthenticationCredentialsNotFoundException("Thiếu hoặc sai định dạng JWT");
            }

            String token = authHeader.substring(7);

            if (!jwtUtil.validateToken(token)) {
                throw new AuthenticationCredentialsNotFoundException("JWT không hợp lệ");
            }

            String username = jwtUtil.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails,
                    null, userDetails.getAuthorities());

            accessor.setUser(authentication);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();

            if (destination != null && destination.startsWith("/topic/conversations/")) {
                String remainder = destination.substring("/topic/conversations/".length());

                int nextSlash = remainder.indexOf('/');
                String conversationIdStr = nextSlash >= 0 ? remainder.substring(0, nextSlash) : remainder;

                try {
                    Long conversationId = Long.parseLong(conversationIdStr);

                    if (accessor.getUser() == null || accessor.getUser().getName() == null) {
                        throw new AuthenticationCredentialsNotFoundException("Chưa xác thực người dùng");
                    }

                    String username = accessor.getUser().getName();
                    boolean isOwner = chatService.isOwner(username, conversationId);

                    if (!isOwner) {
                        throw new AccessDeniedException("Bạn không có quyền truy cập vào cuộc trò chuyện này");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Định dạng conversationId không hợp lệ");
                }
            }
        }

        return message;
    }
}