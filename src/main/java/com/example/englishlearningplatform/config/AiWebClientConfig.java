package com.example.englishlearningplatform.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiWebClientConfig {

    @Bean
    public WebClient xkiroWebClient(AiProperties props) {
        HttpClient httpClient = HttpClient.create()
                // Connect timeout: thời gian tối đa để MỞ được kết nối TCP tới xKiro.
                // Nếu handshake không xong trong khoảng này -> fail ngay, không chờ thêm.
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.getConnectTimeoutMs())
                // Read timeout: SAU KHI đã kết nối xong, thời gian tối đa chờ dữ liệu response.
                // Cơ chế khác hẳn connect timeout — kết nối mở nhanh nhưng model xử lý chậm
                // vẫn phải bị chặn ở đây. Đúng yêu cầu FR-2.6 (2 timeout riêng biệt).
                .doOnConnected(conn -> conn.addHandlerLast(
                        new ReadTimeoutHandler(props.getReadTimeoutMs(), TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }
}