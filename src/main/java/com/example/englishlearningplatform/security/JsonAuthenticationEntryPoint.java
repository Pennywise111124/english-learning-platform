package com.example.englishlearningplatform.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.example.englishlearningplatform.exception.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(JsonAuthenticationEntryPoint.class);

    private static final String MESSAGE = "You need to login to access this resource";

    private final JsonMapper jsonMapper;

    public JsonAuthenticationEntryPoint(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException ex) throws IOException {

        log.debug("Unauthenticated: {} {}", request.getMethod(), request.getRequestURI());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse body = new ErrorResponse(
                MESSAGE,
                HttpStatus.UNAUTHORIZED.value());

        jsonMapper.writeValue(response.getOutputStream(), body);
    }
}