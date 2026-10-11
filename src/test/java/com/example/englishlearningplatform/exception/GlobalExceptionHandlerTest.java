package com.example.englishlearningplatform.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

class GlobalExceptionHandlerTest {

    private static final String SECRET = "SECRET_users_pkey_detail";

    private final Logger handlerLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private ListAppender<ILoggingEvent> logAppender;

    record BodyDto(@NotBlank String name) {
    }

    @RestController
    static class ThrowingController {

        // ── Lớp 2: exception chưa có handler riêng, lớp cha phải bắt ──
        @GetMapping("/t/required-param")
        String required(@RequestParam String q) {
            return q;
        }

        @GetMapping("/t/validated-param")
        String validated(@RequestParam @Size(max = 3) String q) {
            return q;
        }

        @GetMapping("/t/int-param")
        String intParam(@RequestParam int size) {
            return "ok";
        }

        // ── Lưới cuối và lỗi DB ──
        @GetMapping("/t/data-integrity")
        String dataIntegrity() {
            throw new DataIntegrityViolationException(SECRET);
        }

        @GetMapping("/t/boom")
        String boom() {
            throw new IllegalStateException(SECRET);
        }

        // ── Lỗi nghiệp vụ ──
        @GetMapping("/t/illegal-argument")
        String illegalArgument() {
            throw new IllegalArgumentException("bad input");
        }

        // ── Body / upload ──
        @PostMapping("/t/body")
        String body(@Valid @RequestBody BodyDto dto) {
            return dto.name();
        }

        @PostMapping("/t/upload")
        String upload(@RequestParam("file") MultipartFile file) {
            return "ok";
        }

        @PostMapping("/t/too-large")
        String tooLarge() {
            throw new MaxUploadSizeExceededException(10L * 1024 * 1024);
        }

        @GetMapping("/t/missing-path-var")
        String missingPathVar(@PathVariable("leakyPathVarName") String id) {
            return id;
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        logAppender = new ListAppender<>();
        logAppender.start();
        handlerLogger.addAppender(logAppender);
    }

    private ResultActions assertError(ResultActions result, int status) throws Exception {
        return result
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ═══════════ Lớp 2: trước đây bị Exception.class nuốt thành 500 ═══════════

    @Test
    void missingRequestParam_returns400() throws Exception {
        assertError(mockMvc.perform(get("/t/required-param")), 400);
    }

    @Test
    void methodValidation_returns400() throws Exception {
        assertError(mockMvc.perform(get("/t/validated-param").param("q", "abcdef")), 400);
    }

    @Test
    void methodValidation_validValuePasses() throws Exception {
        mockMvc.perform(get("/t/validated-param").param("q", "abc"))
                .andExpect(status().isOk());
    }

    // ═══════════ Lưới cuối: 500 và không lộ thông tin ═══════════

    @Test
    void dataIntegrityViolation_returns500_withoutLeakingDetails() throws Exception {
        assertError(mockMvc.perform(get("/t/data-integrity")), 500)
                .andExpect(jsonPath("$.message", is("Internal server error")))
                .andExpect(content().string(not(containsString(SECRET))));
    }

    @Test
    void unexpectedException_returns500_withoutLeakingDetails() throws Exception {
        assertError(mockMvc.perform(get("/t/boom")), 500)
                .andExpect(jsonPath("$.message", is("Internal server error")))
                .andExpect(content().string(not(containsString(SECRET))));
    }

    @Test
    void missingPathVariable_returns500_throughSpringMvcParent() throws Exception {
        assertError(mockMvc.perform(get("/t/missing-path-var")), 500)
                .andExpect(jsonPath("$.message", is("Internal server error")))
                .andExpect(content().string(not(containsString("leakyPathVarName"))));

        assertThat(logAppender.list).anyMatch(e -> e.getLevel() == Level.ERROR
                && e.getFormattedMessage().contains("Server error from Spring MVC"));
    }
    // ═══════════ Lớp 1: đã có handler riêng, khoá lại để không ai làm hỏng
    // ═══════════

    @Test
    void typeMismatch_returns400_withoutEchoingTheValue() throws Exception {
        assertError(mockMvc.perform(get("/t/int-param").param("size", "abc")), 400)
                .andExpect(jsonPath("$.message", is("Invalid value for parameter: size")))
                .andExpect(content().string(not(containsString("abc"))));
    }

    @Test
    void emptyBody_returns400_withProjectMessage() throws Exception {
        assertError(mockMvc.perform(post("/t/body").contentType(MediaType.APPLICATION_JSON)), 400)
                .andExpect(jsonPath("$.message", is("Malformed or missing request body")));
    }

    @Test
    void malformedJson_returns400_withoutParserDetails() throws Exception {
        assertError(mockMvc.perform(post("/t/body")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":")), 400)
                .andExpect(jsonPath("$.message", is("Malformed or missing request body")))
                .andExpect(content().string(not(containsString("Unexpected"))))
                .andExpect(content().string(not(containsString("tools.jackson"))));
    }

    @Test
    void unsupportedContentType_returns415() throws Exception {
        assertError(mockMvc.perform(post("/t/body")
                .contentType(MediaType.TEXT_PLAIN).content("hello")), 415);
    }

    @Test
    void wrongHttpMethod_returns405_andKeepsAllowHeader() throws Exception {
        assertError(mockMvc.perform(delete("/t/boom")), 405)
                .andExpect(header().string("Allow", containsString("GET")));
    }

    @Test
    void invalidBody_returns400_inFieldColonMessageFormat() throws Exception {
        assertError(mockMvc.perform(post("/t/body")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}")), 400)
                .andExpect(jsonPath("$.message", startsWith("name:")));
    }

    @Test
    void missingFilePart_returns400() throws Exception {
        assertError(mockMvc.perform(multipart("/t/upload")
                .file(new MockMultipartFile("wrong", "a.txt", "text/plain", "hi".getBytes()))), 400);
    }

    @Test
    void notMultipartRequest_returns400_withProjectMessage() throws Exception {
        assertError(mockMvc.perform(post("/t/upload")), 400)
                .andExpect(jsonPath("$.message",
                        is("Request must be multipart/form-data with a 'file' part")));
    }

    @Test
    void maxUploadSizeExceeded_returns413_notTheMultipartHandlers400() throws Exception {
        assertError(mockMvc.perform(post("/t/too-large")), 413);
    }

    @Test
    void illegalArgument_returns400_withItsOwnMessage() throws Exception {
        assertError(mockMvc.perform(get("/t/illegal-argument")), 400)
                .andExpect(jsonPath("$.message", is("bad input")));
    }

    @AfterEach
    void detachLogAppender() {
        handlerLogger.detachAppender(logAppender);
    }
}