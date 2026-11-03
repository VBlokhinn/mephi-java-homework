package ru.mephi.shortener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mephi.shortener.config.AppConfig;
import ru.mephi.shortener.model.RedirectResult;
import ru.mephi.shortener.model.ShortUrl;
import ru.mephi.shortener.repository.InMemoryShortUrlRepository;
import ru.mephi.shortener.repository.ShortUrlRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UrlShortenerServiceTest {
    private ShortUrlRepository repository;
    private AppConfig config;
    private NotificationService notificationService;
    private TestClock clock;
    private UrlShortenerService service;
    private final UUID user1 = UUID.randomUUID();
    private final UUID user2 = UUID.randomUUID();

    static class TestClock extends Clock {
        private Instant now;

        public TestClock(Instant initial) {
            this.now = initial;
        }

        public void addSeconds(long sec) {
            now = now.plusSeconds(sec);
        }

        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @BeforeEach
    void setUp() {
        repository = new InMemoryShortUrlRepository();
        config = new AppConfig(3600L, 2, "clck.ru/", 5L);
        notificationService = new NotificationService();
        clock = new TestClock(Instant.parse("2026-01-01T10:00:00Z"));
        service = new UrlShortenerServiceImpl(repository, config, notificationService, clock);
    }

    @Test
    @DisplayName("Уникальность коротких ссылок для одного пользователя")
    void testUniqueCodesPerUser() {
        ShortUrl url1 = service.shortenUrl(user1, "https://example.com", null, null);
        ShortUrl url2 = service.shortenUrl(user1, "https://example.com", null, null);

        assertNotEquals(url1.getShortCode(), url2.getShortCode());
    }

    @Test
    @DisplayName("Разные пользователи получают разные ссылки для одного URL")
    void testDistinctLinksForDifferentUsers() {
        ShortUrl url1 = service.shortenUrl(user1, "https://ya.ru", null, null);
        ShortUrl url2 = service.shortenUrl(user2, "https://ya.ru", null, null);

        assertNotEquals(url1.getShortCode(), url2.getShortCode());
    }

    @Test
    @DisplayName("Учет кликов и блокировка при исчерпании лимита")
    void testClicksCountingAndLimitExhaustion() {
        ShortUrl url = service.shortenUrl(user1, "https://example.com", 2, null);
        String code = url.getShortCode();

        // 1-й клик
        RedirectResult r1 = service.resolveAndClick(code);
        assertEquals(RedirectResult.Status.SUCCESS, r1.getStatus());

        // 2-й клик (достигнут лимит)
        RedirectResult r2 = service.resolveAndClick(code);
        assertEquals(RedirectResult.Status.SUCCESS, r2.getStatus());

        // 3-й клик (заблокирован)
        RedirectResult r3 = service.resolveAndClick(code);
        assertEquals(RedirectResult.Status.LIMIT_EXHAUSTED, r3.getStatus());
    }

    @Test
    @DisplayName("Истечение TTL и удаление просроченной ссылки")
    void testTtlExpiration() {
        ShortUrl url = service.shortenUrl(user1, "https://example.com", 5, 60L);
        String code = url.getShortCode();

        // Прошло 70 секунд
        clock.addSeconds(70);

        RedirectResult result = service.resolveAndClick(code);
        assertEquals(RedirectResult.Status.EXPIRED, result.getStatus());
        assertTrue(repository.findByCode(code).isEmpty());
    }

    @Test
    @DisplayName("Владелец может изменить лимит переходов")
    void testOwnerCanUpdateLimit() {
        ShortUrl url = service.shortenUrl(user1, "https://example.com", 2, null);
        boolean updated = service.updateLimit(user1, url.getShortCode(), 10);

        assertTrue(updated);
        assertEquals(10, url.getClickLimit());
    }

    @Test
    @DisplayName("Чужой пользователь не может изменить лимит")
    void testNonOwnerCannotUpdateLimit() {
        ShortUrl url = service.shortenUrl(user1, "https://example.com", 2, null);
        assertThrows(SecurityException.class, () -> service.updateLimit(user2, url.getShortCode(), 10));
    }

    @Test
    @DisplayName("Владелец может удалить ссылку")
    void testOwnerCanDelete() {
        ShortUrl url = service.shortenUrl(user1, "https://example.com", 2, null);
        assertTrue(service.deleteUrl(user1, url.getShortCode()));
        assertTrue(repository.findByCode(url.getShortCode()).isEmpty());
    }

    @Test
    @DisplayName("Чужой пользователь не может удалить чужую ссылку")
    void testNonOwnerCannotDelete() {
        ShortUrl url = service.shortenUrl(user1, "https://example.com", 2, null);
        assertThrows(SecurityException.class, () -> service.deleteUrl(user2, url.getShortCode()));
    }
}
