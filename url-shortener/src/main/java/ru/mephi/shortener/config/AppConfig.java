package ru.mephi.shortener.config;

public class AppConfig {
    private final long defaultTtlSeconds;
    private final int defaultClickLimit;
    private final String shortUrlDomain;
    private final long cleanupIntervalSeconds;

    public AppConfig(long defaultTtlSeconds, int defaultClickLimit, String shortUrlDomain, long cleanupIntervalSeconds) {
        this.defaultTtlSeconds = defaultTtlSeconds;
        this.defaultClickLimit = defaultClickLimit;
        this.shortUrlDomain = shortUrlDomain != null ? shortUrlDomain : "clck.ru/";
        this.cleanupIntervalSeconds = cleanupIntervalSeconds;
    }

    public long getDefaultTtlSeconds() {
        return defaultTtlSeconds;
    }

    public int getDefaultClickLimit() {
        return defaultClickLimit;
    }

    public String getShortUrlDomain() {
        return shortUrlDomain;
    }

    public long getCleanupIntervalSeconds() {
        return cleanupIntervalSeconds;
    }

    public static AppConfig createDefault() {
        return new AppConfig(86400L, 5, "clck.ru/", 5L);
    }
}
