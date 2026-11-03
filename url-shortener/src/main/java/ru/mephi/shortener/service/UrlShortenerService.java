package ru.mephi.shortener.service;

import ru.mephi.shortener.model.RedirectResult;
import ru.mephi.shortener.model.ShortUrl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UrlShortenerService {

    ShortUrl shortenUrl(UUID userId, String originalUrl, Integer customLimit, Long customTtlSeconds);

    RedirectResult resolveAndClick(String shortCodeOrUrl);

    Optional<ShortUrl> findByCode(String shortCodeOrUrl);

    boolean updateLimit(UUID userId, String shortCodeOrUrl, int newLimit);

    boolean deleteUrl(UUID userId, String shortCodeOrUrl);

    List<ShortUrl> getUserUrls(UUID userId);

    String toFullShortUrl(String shortCode);

    String extractShortCode(String shortCodeOrUrl);
}
