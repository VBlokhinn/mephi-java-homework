package ru.mephi.shortener.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UrlValidatorTest {

    @Test
    @DisplayName("Корректные ссылки с протоколами http и https")
    void testValidUrls() {
        assertTrue(UrlValidator.isValid("http://example.com"));
        assertTrue(UrlValidator.isValid("https://yandex.ru/search?q=java"));
    }

    @Test
    @DisplayName("Запрет неподдерживаемых протоколов")
    void testInvalidProtocols() {
        assertFalse(UrlValidator.isValid("ftp://example.com/file"));
        assertFalse(UrlValidator.isValid("file:///Users/doc.txt"));
    }

    @Test
    @DisplayName("Отклонение пустых или некорректных строк")
    void testInvalidOrEmpty() {
        assertFalse(UrlValidator.isValid(null));
        assertFalse(UrlValidator.isValid(""));
        assertFalse(UrlValidator.isValid("not-a-url"));
    }
}
