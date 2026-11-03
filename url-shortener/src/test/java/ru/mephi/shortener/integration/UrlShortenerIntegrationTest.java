package ru.mephi.shortener.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mephi.shortener.config.AppConfig;
import ru.mephi.shortener.model.RedirectResult;
import ru.mephi.shortener.model.ShortUrl;
import ru.mephi.shortener.repository.InMemoryShortUrlRepository;
import ru.mephi.shortener.repository.ShortUrlRepository;
import ru.mephi.shortener.service.NotificationService;
import ru.mephi.shortener.service.UrlShortenerService;
import ru.mephi.shortener.service.UrlShortenerServiceImpl;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UrlShortenerIntegrationTest {
    private ShortUrlRepository repository;
    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryShortUrlRepository();
        AppConfig config = new AppConfig(86400L, 5, "clck.ru/", 5L);
        service = new UrlShortenerServiceImpl(repository, config, new NotificationService());
    }

    @Test
    @DisplayName("Жизненный цикл ссылки: создание -> клики -> лимит -> увеличение лимита -> продолжение работы")
    void testFullLifecycleScenario() {
        UUID userId = UUID.randomUUID();
        ShortUrl url = service.shortenUrl(userId, "https://github.com", 2, 3600L);
        String code = url.getShortCode();

        // Первые 2 клика успешны
        assertTrue(service.resolveAndClick(code).isSuccessful());
        assertTrue(service.resolveAndClick(code).isSuccessful());

        // 3-й клик заблокирован (лимит исчерпан)
        RedirectResult r3 = service.resolveAndClick(code);
        assertEquals(RedirectResult.Status.LIMIT_EXHAUSTED, r3.getStatus());

        // Владелец увеличивает лимит
        assertTrue(service.updateLimit(userId, code, 5));

        // Ссылка снова доступна
        assertTrue(service.resolveAndClick(code).isSuccessful());
    }

    @Test
    @DisplayName("Изоляция пользователей: раздельные списки ссылок и защита от чужих действий")
    void testMultiUserIsolationScenario() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        ShortUrl u1 = service.shortenUrl(user1, "https://user1.com", 5, 3600L);
        ShortUrl u2 = service.shortenUrl(user2, "https://user2.com", 5, 3600L);

        List<ShortUrl> user1Urls = service.getUserUrls(user1);
        assertEquals(1, user1Urls.size());
        assertEquals(u1.getShortCode(), user1Urls.get(0).getShortCode());

        // user2 не может удалить ссылку user1
        assertThrows(SecurityException.class, () -> service.deleteUrl(user2, u1.getShortCode()));

        // user1 может удалить свою ссылку
        assertTrue(service.deleteUrl(user1, u1.getShortCode()));
    }
}
