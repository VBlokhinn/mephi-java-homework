package ru.mephi.shortener.service;

import ru.mephi.shortener.config.AppConfig;
import ru.mephi.shortener.model.ShortUrl;
import ru.mephi.shortener.repository.ShortUrlRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TtlCleanupScheduler {
    private final ShortUrlRepository repository;
    private final NotificationService notificationService;
    private final AppConfig config;
    private final Clock clock;
    private ScheduledExecutorService scheduler;

    public TtlCleanupScheduler(ShortUrlRepository repository,
                               NotificationService notificationService,
                               AppConfig config) {
        this(repository, notificationService, config, Clock.systemUTC());
    }

    public TtlCleanupScheduler(ShortUrlRepository repository,
                               NotificationService notificationService,
                               AppConfig config,
                               Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.notificationService = Objects.requireNonNull(notificationService, "notificationService cannot be null");
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    public synchronized void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ttl-cleanup-thread");
            t.setDaemon(true);
            return t;
        });

        long interval = Math.max(1L, config.getCleanupIntervalSeconds());
        scheduler.scheduleAtFixedRate(this::triggerCleanup, interval, interval, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    public int triggerCleanup() {
        try {
            Instant now = clock.instant();
            List<ShortUrl> expiredUrls = repository.deleteExpired(now);
            for (ShortUrl url : expiredUrls) {
                notificationService.onUrlExpired(url);
            }
            return expiredUrls.size();
        } catch (Exception e) {
            System.err.printf("[Ошибка очистки TTL]: %s%n", e.getMessage());
            return 0;
        }
    }
}
