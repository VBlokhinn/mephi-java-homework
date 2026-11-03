package ru.mephi.shortener.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mephi.shortener.model.ShortUrl;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryShortUrlRepositoryTest {
    private ShortUrlRepository repository;
    private final UUID user1 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = new InMemoryShortUrlRepository();
    }

    @Test
    @DisplayName("Сохранение и поиск по короткому коду")
    void testSaveAndFind() {
        Instant now = Instant.now();
        ShortUrl url = new ShortUrl("code1", "https://example.com", user1, 5, now, now.plusSeconds(3600));

        repository.save(url);
        assertTrue(repository.findByCode("code1").isPresent());
        assertEquals("https://example.com", repository.findByCode("code1").get().getOriginalUrl());
    }

    @Test
    @DisplayName("Поиск всех ссылок одного пользователя")
    void testFindByOwner() {
        Instant now = Instant.now();
        repository.save(new ShortUrl("c1", "https://site1.org", user1, 5, now, now.plusSeconds(100)));
        repository.save(new ShortUrl("c2", "https://site2.org", user1, 5, now, now.plusSeconds(100)));

        List<ShortUrl> urls = repository.findByOwnerId(user1);
        assertEquals(2, urls.size());
    }

    @Test
    @DisplayName("Удаление просроченных ссылок")
    void testDeleteExpired() {
        Instant now = Instant.now();
        ShortUrl expired = new ShortUrl("exp", "https://old.com", user1, 5, now.minusSeconds(100), now.minusSeconds(10));
        ShortUrl active = new ShortUrl("act", "https://new.com", user1, 5, now, now.plusSeconds(100));

        repository.save(expired);
        repository.save(active);

        List<ShortUrl> deleted = repository.deleteExpired(now);
        assertEquals(1, deleted.size());
        assertTrue(repository.findByCode("exp").isEmpty());
        assertTrue(repository.findByCode("act").isPresent());
    }
}
