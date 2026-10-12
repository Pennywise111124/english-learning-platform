package com.example.englishlearningplatform.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class RateLimitPropertiesValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    private static RateLimitProperties with(String field, int value) {
        int login = 1;
        int register = 1;
        int chat = 1;
        switch (field) {
            case "loginPerMinute" -> login = value;
            case "registerPerHour" -> register = value;
            case "chatPerMinute" -> chat = value;
            default -> throw new IllegalArgumentException(field);
        }
        return new RateLimitProperties(login, register, chat, false);
    }

    @ParameterizedTest(name = "{0} = {1} -> đúng 1 vi phạm")
    @CsvSource({
            "loginPerMinute, 0", "loginPerMinute, -1",
            "registerPerHour, 0", "registerPerHour, -1",
            "chatPerMinute, 0", "chatPerMinute, -1" })
    void nonPositiveValue_hasExactlyOneViolationOnThatField(String field, int value) {
        Set<ConstraintViolation<RateLimitProperties>> violations = validator.validate(with(field, value));

        assertEquals(1, violations.size());
        assertEquals(field, violations.iterator().next().getPropertyPath().toString());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void allOnes_isValid_regardlessOfTrustedProxy(boolean trustedProxy) {
        assertTrue(validator.validate(new RateLimitProperties(1, 1, 1, trustedProxy)).isEmpty());
    }
}