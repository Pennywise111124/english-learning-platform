package com.example.englishlearningplatform.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.example.englishlearningplatform.exception.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(JsonAccessDeniedHandler.class);

    private static final String MESSAGE = "You do not have permission to access this resource";

    private final JsonMapper jsonMapper;

    public JsonAccessDeniedHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException ex) throws IOException {
        log.warn("Access denied: {} {}", request.getMethod(), request.getRequestURI());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse body = new ErrorResponse(
                MESSAGE,
                HttpStatus.FORBIDDEN.value());

        jsonMapper.writeValue(response.getOutputStream(), body);
    }
}