package ru.mephi.shortener.repository;

import ru.mephi.shortener.model.ShortUrl;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryShortUrlRepository implements ShortUrlRepository {
    private final Map<String, ShortUrl> storage = new ConcurrentHashMap<>();

    @Override
    public void save(ShortUrl shortUrl) {
        Objects.requireNonNull(shortUrl, "shortUrl cannot be null");
        storage.put(shortUrl.getShortCode(), shortUrl);
    }

    @Override
    public Optional<ShortUrl> findByCode(String shortCode) {
        if (shortCode == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(shortCode));
    }

    @Override
    public List<ShortUrl> findByOwnerId(UUID ownerId) {
        if (ownerId == null) {
            return Collections.emptyList();
        }
        return storage.values().stream()
                .filter(url -> ownerId.equals(url.getOwnerId()))
                .sorted(Comparator.comparing(ShortUrl::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteByCode(String shortCode) {
        if (shortCode == null) {
            return false;
        }
        return storage.remove(shortCode) != null;
    }

    @Override
    public List<ShortUrl> deleteExpired(Instant now) {
        List<ShortUrl> expiredUrls = new ArrayList<>();
        for (Map.Entry<String, ShortUrl> entry : storage.entrySet()) {
            ShortUrl url = entry.getValue();
            if (url.isExpired(now)) {
                if (storage.remove(entry.getKey(), url)) {
                    expiredUrls.add(url);
                }
            }
        }
        return expiredUrls;
    }

    @Override
    public int count() {
        return storage.size();
    }

    @Override
    public void clear() {
        storage.clear();
    }
}
