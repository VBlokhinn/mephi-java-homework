package ru.mephi.shortener;

import ru.mephi.shortener.cli.CliController;
import ru.mephi.shortener.config.AppConfig;
import ru.mephi.shortener.config.ConfigLoader;
import ru.mephi.shortener.repository.InMemoryShortUrlRepository;
import ru.mephi.shortener.repository.ShortUrlRepository;
import ru.mephi.shortener.service.BrowserRedirectService;
import ru.mephi.shortener.service.NotificationService;
import ru.mephi.shortener.service.TtlCleanupScheduler;
import ru.mephi.shortener.service.UrlShortenerService;
import ru.mephi.shortener.service.UrlShortenerServiceImpl;


public class Main {
    public static void main(String[] args) {
        AppConfig config = ConfigLoader.load();

        ShortUrlRepository repository = new InMemoryShortUrlRepository();

        NotificationService notificationService = new NotificationService();
        BrowserRedirectService redirectService = new BrowserRedirectService();
        UrlShortenerService shortenerService = new UrlShortenerServiceImpl(repository, config, notificationService);

        TtlCleanupScheduler cleanupScheduler = new TtlCleanupScheduler(repository, notificationService, config);
        cleanupScheduler.start();

        Runtime.getRuntime().addShutdownHook(new Thread(cleanupScheduler::stop));

        CliController controller = new CliController(shortenerService, redirectService, System.in, System.out);
        try {
            controller.run();
        } finally {
            cleanupScheduler.stop();
        }
    }
}
