package ru.mephi.shortener.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ConfigLoader {
    public static final String DEFAULT_CONFIG_RESOURCE = "config.properties";

    public static AppConfig load() {
        return loadFromResource(DEFAULT_CONFIG_RESOURCE);
    }

    public static AppConfig loadFromResource(String resourceName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ConfigLoader.class.getClassLoader();
        }

        try (InputStream inputStream = classLoader.getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                return AppConfig.createDefault();
            }
            return loadFromInputStream(inputStream);
        } catch (IOException e) {
            return AppConfig.createDefault();
        }
    }

    public static AppConfig loadFromFile(Path path) {
        if (path == null || !Files.exists(path)) {
            return AppConfig.createDefault();
        }

        try (InputStream inputStream = Files.newInputStream(path)) {
            return loadFromInputStream(inputStream);
        } catch (IOException e) {
            return AppConfig.createDefault();
        }
    }

    public static AppConfig loadFromInputStream(InputStream inputStream) throws IOException {
        Properties properties = new Properties();
        properties.load(inputStream);

        AppConfig defaults = AppConfig.createDefault();

        long ttl = parseLongOrDefault(properties.getProperty("default.ttl.seconds"), defaults.getDefaultTtlSeconds());
        int limit = parseIntOrDefault(properties.getProperty("default.click.limit"), defaults.getDefaultClickLimit());
        String domain = properties.getProperty("short.url.domain", defaults.getShortUrlDomain());
        long cleanup = parseLongOrDefault(properties.getProperty("cleanup.interval.seconds"), defaults.getCleanupIntervalSeconds());

        return new AppConfig(ttl, limit, domain, cleanup);
    }

    private static long parseLongOrDefault(String value, long defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
