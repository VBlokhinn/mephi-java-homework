package ru.mephi.shortener.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ShortUrl {
    private final String shortCode;
    private final String originalUrl;
    private final UUID ownerId;
    private volatile int clickLimit;
    private final AtomicInteger currentClicks;
    private final Instant createdAt;
    private volatile Instant expiresAt;

    public ShortUrl(String shortCode,
                    String originalUrl,
                    UUID ownerId,
                    int clickLimit,
                    Instant createdAt,
                    Instant expiresAt) {
        this.shortCode = Objects.requireNonNull(shortCode, "shortCode cannot be null");
        this.originalUrl = Objects.requireNonNull(originalUrl, "originalUrl cannot be null");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId cannot be null");
        this.clickLimit = clickLimit;
        this.currentClicks = new AtomicInteger(0);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt cannot be null");
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public int getClickLimit() {
        return clickLimit;
    }

    public void setClickLimit(int clickLimit) {
        if (clickLimit <= 0) {
            throw new IllegalArgumentException("Click limit must be greater than 0");
        }
        this.clickLimit = clickLimit;
    }

    public int getCurrentClicks() {
        return currentClicks.get();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt cannot be null");
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt) || now.equals(expiresAt);
    }

    public boolean isLimitReached() {
        return currentClicks.get() >= clickLimit;
    }

    public boolean isActive(Instant now) {
        return !isExpired(now) && !isLimitReached();
    }

    public int getRemainingClicks() {
        int remaining = clickLimit - currentClicks.get();
        return Math.max(remaining, 0);
    }

    public long getRemainingSeconds(Instant now) {
        if (isExpired(now)) {
            return 0L;
        }
        return Math.max(0L, Duration.between(now, expiresAt).getSeconds());
    }

    public boolean tryClick(Instant now) {
        if (isExpired(now)) {
            return false;
        }
        while (true) {
            int current = currentClicks.get();
            if (current >= clickLimit) {
                return false;
            }
            if (currentClicks.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShortUrl shortUrl)) return false;
        return Objects.equals(shortCode, shortUrl.shortCode);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(shortCode);
    }

    @Override
    public String toString() {
        return "ShortUrl{" +
                "shortCode='" + shortCode + '\'' +
                ", originalUrl='" + originalUrl + '\'' +
                ", ownerId=" + ownerId +
                ", clickLimit=" + clickLimit +
                ", currentClicks=" + currentClicks.get() +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
