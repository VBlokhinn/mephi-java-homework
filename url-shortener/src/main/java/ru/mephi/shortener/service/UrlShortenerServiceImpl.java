package ru.mephi.shortener.service;

import ru.mephi.shortener.config.AppConfig;
import ru.mephi.shortener.model.RedirectResult;
import ru.mephi.shortener.model.ShortUrl;
import ru.mephi.shortener.repository.ShortUrlRepository;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

public class UrlShortenerServiceImpl implements UrlShortenerService {
    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;

    private final ShortUrlRepository repository;
    private final AppConfig config;
    private final NotificationService notificationService;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public UrlShortenerServiceImpl(ShortUrlRepository repository,
                                   AppConfig config,
                                   NotificationService notificationService) {
        this(repository, config, notificationService, Clock.systemUTC());
    }

    public UrlShortenerServiceImpl(ShortUrlRepository repository,
                                   AppConfig config,
                                   NotificationService notificationService,
                                   Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.notificationService = Objects.requireNonNull(notificationService, "notificationService cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Override
    public ShortUrl shortenUrl(UUID userId, String originalUrl, Integer customLimit, Long customTtlSeconds) {
        Objects.requireNonNull(userId, "userId cannot be null");
        if (!UrlValidator.isValid(originalUrl)) {
            throw new IllegalArgumentException("Некорректный URL: '" + originalUrl + "'. Ссылка должна начинаться с http:// или https://");
        }

        int limit = (customLimit != null) ? customLimit : config.getDefaultClickLimit();
        if (limit <= 0) {
            throw new IllegalArgumentException("Лимит переходов должен быть больше 0");
        }

        long ttlSeconds = (customTtlSeconds != null) ? customTtlSeconds : config.getDefaultTtlSeconds();
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("Время жизни (TTL) должно быть больше 0");
        }

        Instant now = clock.instant();
        Instant expiresAt = now.plusSeconds(ttlSeconds);

        String shortCode = generateUniqueCode();

        ShortUrl shortUrl = new ShortUrl(
                shortCode,
                originalUrl.trim(),
                userId,
                limit,
                now,
                expiresAt
        );

        repository.save(shortUrl);
        return shortUrl;
    }

    @Override
    public RedirectResult resolveAndClick(String shortCodeOrUrl) {
        String code = extractShortCode(shortCodeOrUrl);
        Optional<ShortUrl> opt = repository.findByCode(code);
        if (opt.isEmpty()) {
            return RedirectResult.notFound(code);
        }

        ShortUrl shortUrl = opt.get();
        Instant now = clock.instant();

        if (shortUrl.isExpired(now)) {
            repository.deleteByCode(code);
            notificationService.onUrlExpired(shortUrl);
            return RedirectResult.expired(shortUrl);
        }

        if (!shortUrl.tryClick(now)) {
            if (shortUrl.isExpired(now)) {
                repository.deleteByCode(code);
                notificationService.onUrlExpired(shortUrl);
                return RedirectResult.expired(shortUrl);
            }
            notificationService.onLimitExhausted(shortUrl);
            return RedirectResult.limitExhausted(shortUrl);
        }

        if (shortUrl.isLimitReached()) {
            notificationService.onLimitExhausted(shortUrl);
        }

        return RedirectResult.success(shortUrl.getOriginalUrl(), shortUrl);
    }

    @Override
    public Optional<ShortUrl> findByCode(String shortCodeOrUrl) {
        String code = extractShortCode(shortCodeOrUrl);
        return repository.findByCode(code);
    }

    @Override
    public boolean updateLimit(UUID userId, String shortCodeOrUrl, int newLimit) {
        Objects.requireNonNull(userId, "userId cannot be null");
        if (newLimit <= 0) {
            throw new IllegalArgumentException("Новый лимит переходов должен быть больше 0");
        }

        String code = extractShortCode(shortCodeOrUrl);
        Optional<ShortUrl> opt = repository.findByCode(code);
        if (opt.isEmpty()) {
            return false;
        }

        ShortUrl shortUrl = opt.get();
        if (!userId.equals(shortUrl.getOwnerId())) {
            throw new SecurityException("Доступ запрещен. Только создатель ссылки может изменять её параметры.");
        }

        int oldLimit = shortUrl.getClickLimit();
        shortUrl.setClickLimit(newLimit);
        notificationService.onLimitUpdated(shortUrl, oldLimit, newLimit);
        return true;
    }

    @Override
    public boolean deleteUrl(UUID userId, String shortCodeOrUrl) {
        Objects.requireNonNull(userId, "userId cannot be null");
        String code = extractShortCode(shortCodeOrUrl);
        Optional<ShortUrl> opt = repository.findByCode(code);
        if (opt.isEmpty()) {
            return false;
        }

        ShortUrl shortUrl = opt.get();
        if (!userId.equals(shortUrl.getOwnerId())) {
            throw new SecurityException("Доступ запрещен. Только создатель ссылки может удалить её.");
        }

        return repository.deleteByCode(code);
    }

    @Override
    public List<ShortUrl> getUserUrls(UUID userId) {
        Objects.requireNonNull(userId, "userId cannot be null");
        return repository.findByOwnerId(userId);
    }

    @Override
    public String toFullShortUrl(String shortCode) {
        String domain = config.getShortUrlDomain();
        if (!domain.endsWith("/")) {
            domain += "/";
        }
        return domain + extractShortCode(shortCode);
    }

    @Override
    public String extractShortCode(String shortCodeOrUrl) {
        if (shortCodeOrUrl == null) {
            return "";
        }
        String s = shortCodeOrUrl.trim();

        if (s.startsWith("http://")) {
            s = s.substring(7);
        } else if (s.startsWith("https://")) {
            s = s.substring(8);
        }


        String domain = config.getShortUrlDomain().trim();
        if (domain.startsWith("http://")) {
            domain = domain.substring(7);
        } else if (domain.startsWith("https://")) {
            domain = domain.substring(8);
        }
        if (domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }

        if (s.startsWith(domain + "/")) {
            s = s.substring(domain.length() + 1);
        } else if (s.startsWith(domain)) {
            s = s.substring(domain.length());
        }

        if (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }

        int lastSlash = s.lastIndexOf('/');
        if (lastSlash >= 0) {
            s = s.substring(lastSlash + 1);
        }

        return s;
    }

    private String generateUniqueCode() {
        while (true) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                int index = random.nextInt(BASE62.length());
                sb.append(BASE62.charAt(index));
            }
            String code = sb.toString();
            if (repository.findByCode(code).isEmpty()) {
                return code;
            }
        }
    }
}
