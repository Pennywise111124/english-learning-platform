package com.example.englishlearningplatform.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClientIpResolverTest {

    private static final String REMOTE = "1.1.1.1";

    // cột: trusted | remoteAddr | forwardedFor | expected
    @ParameterizedTest(name = "trusted={0}, remote={1}, xff={2} -> {3}")
    @CsvSource(nullValues = "NULL", value = {
            // 1. không tin proxy: header bị bỏ qua
            "false, 1.1.1.1, '1.2.3.4', 1.1.1.1",
            "false, 1.1.1.1, '1.2.3.4, 5.6.7.8', 1.1.1.1",
            // 2. tin proxy, header null
            "true, 1.1.1.1, NULL, 1.1.1.1",
            // 3. tin proxy, header rỗng / chỉ khoảng trắng
            "true, 1.1.1.1, '', 1.1.1.1",
            "true, 1.1.1.1, '   ', 1.1.1.1",
            // 4. một IP
            "true, 1.1.1.1, '9.9.9.9', 9.9.9.9",
            // 5. nhiều proxy: lấy phần tử đầu
            "true, 1.1.1.1, '9.9.9.9, 10.0.0.1, 10.0.0.2', 9.9.9.9",
            // 6. cắt khoảng trắng
            "true, 1.1.1.1, '  9.9.9.9  , 10.0.0.1', 9.9.9.9",
            // 7. phần tử đầu rỗng -> remoteAddr
            "true, 1.1.1.1, ', 10.0.0.1', 1.1.1.1",
            // 8. remoteAddr null: không bao giờ trả null
            "false, NULL, NULL, unknown",
            "false, NULL, '1.2.3.4', unknown",
            "true, NULL, NULL, unknown",
            "true, NULL, ', 10.0.0.1', unknown",
            // remoteAddr null nhưng header hợp lệ và được tin -> dùng header
            "true, NULL, '9.9.9.9', 9.9.9.9"
    })
    void resolve_table(boolean trusted, String remoteAddr, String forwardedFor, String expected) {
        assertEquals(expected, ClientIpResolver.resolve(remoteAddr, forwardedFor, trusted));
    }

    @Test
    void forwardedFor_longerThan64_isTruncatedToExactly64() {
        String header = "a".repeat(500);

        String result = ClientIpResolver.resolve(REMOTE, header, true);

        assertEquals(64, result.length());
        assertEquals("a".repeat(64), result);
    }

    @Test
    void forwardedFor_ofExactly64_isKeptWhole() {
        String header = "b".repeat(64);

        assertEquals(header, ClientIpResolver.resolve(REMOTE, header, true));
    }

    @Test
    void remoteAddr_longerThan64_isAlsoTruncated() {
        String remote = "c".repeat(100);

        assertEquals("c".repeat(64), ClientIpResolver.resolve(remote, null, false));
    }
}