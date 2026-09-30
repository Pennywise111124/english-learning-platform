package com.example.englishlearningplatform.dto.topic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TopicSortTest {

    @Test
    @DisplayName("Trả về NEWEST khi tham số đầu vào là null")
    void from_whenNull_shouldReturnNewest() {
        assertEquals(TopicSort.NEWEST, TopicSort.from(null));
    }

    @Test
    @DisplayName("Trả về NEWEST khi tham số đầu vào là chuỗi rỗng hoặc chỉ chứa khoảng trắng")
    void from_whenBlank_shouldReturnNewest() {
        assertEquals(TopicSort.NEWEST, TopicSort.from(""));
        assertEquals(TopicSort.NEWEST, TopicSort.from("   "));
    }

    @Test
    @DisplayName("Trả về Enum tương ứng khi tham số hợp lệ (không phân biệt hoa/thường, tự cắt khoảng trắng)")
    void from_whenValidValueInAnyCase_shouldReturnMatchingEnum() {
        assertEquals(TopicSort.POPULAR, TopicSort.from("popular"));
        assertEquals(TopicSort.POPULAR, TopicSort.from("POPULAR"));
        assertEquals(TopicSort.TITLE, TopicSort.from(" title "));
    }

    @Test
    @DisplayName("Bắn ra IllegalArgumentException khi truyền vào giá trị không hợp lệ")
    void from_whenUnknownValue_shouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> TopicSort.from("xyz"));

        assertNotNull(exception);
    }
}