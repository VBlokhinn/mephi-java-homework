package ru.mephi.shortener.repository;

import ru.mephi.shortener.model.ShortUrl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShortUrlRepository {
    void save(ShortUrl shortUrl);

    Optional<ShortUrl> findByCode(String shortCode);

    List<ShortUrl> findByOwnerId(UUID ownerId);

    boolean deleteByCode(String shortCode);

    List<ShortUrl> deleteExpired(Instant now);

    int count();

    void clear();
}
