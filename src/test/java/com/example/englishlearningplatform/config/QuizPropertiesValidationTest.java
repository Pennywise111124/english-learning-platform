package com.example.englishlearningplatform.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;

class QuizPropertiesValidationTest {

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

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, 101 })
    void passScoreOutOfRange_hasExactlyOneViolation(int value) {
        Set<ConstraintViolation<QuizProperties>> violations = validator.validate(new QuizProperties(value));

        assertEquals(1, violations.size());
        assertEquals("passScore", violations.iterator().next().getPropertyPath().toString());
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 70, 100 })
    void passScoreInRange_hasNoViolation(int value) {
        assertTrue(validator.validate(new QuizProperties(value)).isEmpty());
    }
}