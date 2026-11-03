package ru.mephi.shortener.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

    @Test
    @DisplayName("Загрузка дефолтных настроек")
    void testLoadDefaults() {
        AppConfig config = ConfigLoader.loadFromResource("non_existent.properties");
        assertNotNull(config);
        assertEquals(86400L, config.getDefaultTtlSeconds());
        assertEquals(5, config.getDefaultClickLimit());
        assertEquals("clck.ru/", config.getShortUrlDomain());
    }

    @Test
    @DisplayName("Загрузка параметров из файла/потока")
    void testLoadCustom() throws IOException {
        String props = "default.ttl.seconds=600\ndefault.click.limit=10\nshort.url.domain=clck.ru/\n";
        try (ByteArrayInputStream in = new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8))) {
            AppConfig config = ConfigLoader.loadFromInputStream(in);
            assertEquals(600L, config.getDefaultTtlSeconds());
            assertEquals(10, config.getDefaultClickLimit());
        }
    }
}
