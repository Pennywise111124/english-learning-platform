package com.example.englishlearningplatform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.example.englishlearningplatform.security.StompAuthChannelInterceptor;
import com.example.englishlearningplatform.security.StompErrorHandler;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final StompErrorHandler stompErrorHandler;

    public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor,
            StompErrorHandler stompErrorHandler) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
        this.stompErrorHandler = stompErrorHandler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.setErrorHandler(stompErrorHandler);

        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*") // TODO: siết lại domain thật khi có Frontend deploy, tránh "*" ở
                                               // production
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue"); // client subscribe /topic/conversations/{id}
        registry.setApplicationDestinationPrefixes("/app"); // client gửi tới /app/chat.sendMessage
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}