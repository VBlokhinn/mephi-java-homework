package ru.mephi.shortener.cli;

import ru.mephi.shortener.model.RedirectResult;
import ru.mephi.shortener.model.ShortUrl;
import ru.mephi.shortener.model.User;
import ru.mephi.shortener.service.BrowserRedirectService;
import ru.mephi.shortener.service.UrlShortenerService;

import java.io.InputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

public class CliController {
    private final UrlShortenerService shortenerService;
    private final BrowserRedirectService redirectService;
    private final Scanner scanner;
    private final PrintStream out;
    private User currentUser;

    public CliController(UrlShortenerService shortenerService,
                         BrowserRedirectService redirectService,
                         InputStream in,
                         PrintStream out) {
        this.shortenerService = shortenerService;
        this.redirectService = redirectService;
        this.scanner = new Scanner(in);
        this.out = out;
        this.currentUser = User.createNew();
    }

    public void run() {
        out.println("Сервис коротких ссылок");
        out.printf("Текущий пользователь (UUID): %s%n", currentUser.getId());
        out.println("Введите 'help' для справки или 'exit' для выхода.");

        boolean running = true;
        while (running && scanner.hasNextLine()) {
            out.printf("[%s] > ", currentUser.getId().toString().substring(0, 8));
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }

            try {
                running = handleCommandLine(line);
            } catch (Exception e) {
                out.printf("Ошибка при выполнении команды: %s%n", e.getMessage());
            }
        }
    }

    public boolean handleCommandLine(String line) {
        String[] parts = line.split("\\s+");
        String command = parts[0].toLowerCase();

        switch (command) {
            case "help":
                printHelp();
                break;
            case "shorten":
                handleShorten(parts);
                break;
            case "open":
                handleOpen(parts);
                break;
            case "list":
                handleList();
                break;
            case "edit":
                handleEdit(parts);
                break;
            case "delete":
                handleDelete(parts);
                break;
            case "info":
                handleInfo(parts);
                break;
            case "user":
                handleUser(parts);
                break;
            case "exit":
            case "quit":
                out.println("Завершение работы.");
                return false;
            default:
                out.printf("Неизвестная команда '%s'. Введите 'help' для справки.%n", command);
        }
        return true;
    }

    private void handleShorten(String[] parts) {
        if (parts.length < 2) {
            out.println("Использование: shorten <URL> [лимит_кликов] [время_жизни_сек]");
            out.println("Пример: shorten https://ya.ru");
            out.println("Пример: shorten https://ya.ru 10 3600");
            return;
        }

        String originalUrl = parts[1];
        Integer customLimit = null;
        Long customTtl = null;

        if (parts.length >= 3) {
            try {
                customLimit = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                out.println("Ошибка: лимит переходов должен быть целым числом больше нуля.");
                return;
            }
        }

        if (parts.length >= 4) {
            try {
                customTtl = Long.parseLong(parts[3]);
            } catch (NumberFormatException e) {
                out.println("Ошибка: время жизни (TTL) должно быть целым числом секунд больше нуля.");
                return;
            }
        }

        try {
            ShortUrl shortUrl = shortenerService.shortenUrl(currentUser.getId(), originalUrl, customLimit, customTtl);
            out.println("Ссылка создана:");
            out.printf("  Короткая ссылка : %s%n", shortenerService.toFullShortUrl(shortUrl.getShortCode()));
            out.printf("  Код ссылки      : %s%n", shortUrl.getShortCode());
            out.printf("  Лимит переходов : %d%n", shortUrl.getClickLimit());
            out.printf("  Осталось секунд : %d%n", shortUrl.getRemainingSeconds(Instant.now()));
        } catch (IllegalArgumentException e) {
            out.printf("Ошибка валидации: %s%n", e.getMessage());
        }
    }

    private void handleOpen(String[] parts) {
        if (parts.length < 2) {
            out.println("Использование: open <код_или_ссылка>");
            out.println("Пример: open clck.ru/aB3dE1 или open aB3dE1");
            return;
        }

        String target = parts[1];
        RedirectResult result = shortenerService.resolveAndClick(target);

        switch (result.getStatus()) {
            case SUCCESS:
                out.printf("Переход: %s -> %s%n", target, result.getOriginalUrl());
                ShortUrl url = result.getShortUrl();
                if (url != null) {
                    out.printf("Осталось переходов: %d из %d%n",
                            url.getRemainingClicks(), url.getClickLimit());
                }
                redirectService.openInBrowser(result.getOriginalUrl());
                break;
            case LIMIT_EXHAUSTED:
            case EXPIRED:
            case NOT_FOUND:
                out.println(result.getMessage());
                break;
        }
    }

    private void handleList() {
        List<ShortUrl> urls = shortenerService.getUserUrls(currentUser.getId());
        if (urls.isEmpty()) {
            out.println("Список ссылок пуст. Создайте ссылку командой 'shorten <URL>'.");
            return;
        }

        out.printf("Ссылки пользователя [%s] (всего: %d):%n", currentUser.getId(), urls.size());
        out.printf("%-18s %-12s %-14s %-10s %s%n", "Короткая ссылка", "Переходы", "Осталось (с)", "Статус", "Исходный URL");

        Instant now = Instant.now();
        for (ShortUrl u : urls) {
            String status;
            if (u.isExpired(now)) {
                status = "ИСТЕКЛА";
            } else if (u.isLimitReached()) {
                status = "ЛИМИТ";
            } else {
                status = "АКТИВНА";
            }

            String clicks = String.format("%d / %d", u.getCurrentClicks(), u.getClickLimit());
            long remainingSec = u.getRemainingSeconds(now);

            out.printf("%-18s %-12s %-14d %-10s %s%n",
                    shortenerService.toFullShortUrl(u.getShortCode()),
                    clicks,
                    remainingSec,
                    status,
                    u.getOriginalUrl());
        }
    }

    private void handleEdit(String[] parts) {
        if (parts.length < 3) {
            out.println("Использование: edit <код_или_ссылка> <новый_лимит>");
            out.println("Пример: edit aB3dE1 20");
            return;
        }

        String target = parts[1];
        int newLimit;
        try {
            newLimit = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            out.println("Ошибка: новый лимит должен быть целым числом больше нуля.");
            return;
        }

        try {
            boolean updated = shortenerService.updateLimit(currentUser.getId(), target, newLimit);
            if (updated) {
                out.println("Лимит переходов успешно обновлен.");
            } else {
                out.println("Ссылка не найдена.");
            }
        } catch (SecurityException e) {
            out.printf("Ошибка доступа: %s%n", e.getMessage());
        } catch (IllegalArgumentException e) {
            out.printf("Ошибка: %s%n", e.getMessage());
        }
    }

    private void handleDelete(String[] parts) {
        if (parts.length < 2) {
            out.println("Использование: delete <код_или_ссылка>");
            out.println("Пример: delete aB3dE1");
            return;
        }

        String target = parts[1];
        try {
            boolean deleted = shortenerService.deleteUrl(currentUser.getId(), target);
            if (deleted) {
                out.printf("Ссылка '%s' удалена.%n", target);
            } else {
                out.println("Ссылка не найдена или уже удалена.");
            }
        } catch (SecurityException e) {
            out.printf("Ошибка доступа: %s%n", e.getMessage());
        }
    }

    private void handleInfo(String[] parts) {
        if (parts.length < 2) {
            out.println("Использование: info <код_или_ссылка>");
            return;
        }

        String target = parts[1];
        Optional<ShortUrl> opt = shortenerService.findByCode(target);
        if (opt.isEmpty()) {
            out.println("Ссылка не найдена.");
            return;
        }

        ShortUrl url = opt.get();
        Instant now = Instant.now();
        boolean isOwner = currentUser.getId().equals(url.getOwnerId());

        out.printf("Информация о ссылке [%s]:%n", shortenerService.toFullShortUrl(url.getShortCode()));
        out.printf("  Код          : %s%n", url.getShortCode());
        out.printf("  Исходный URL : %s%n", url.getOriginalUrl());
        out.printf("  Владелец UUID: %s %s%n", url.getOwnerId(), isOwner ? "(вы)" : "(другой пользователь)");
        out.printf("  Кликов       : %d из %d (осталось: %d)%n",
                url.getCurrentClicks(), url.getClickLimit(), url.getRemainingClicks());
        out.printf("  Осталось     : %d сек.%n", url.getRemainingSeconds(now));
        out.printf("  Статус       : %s%n",
                url.isExpired(now) ? "ИСТЕКЛА" : (url.isLimitReached() ? "ЛИМИТ ИСЧЕРПАН" : "АКТИВНА"));
    }

    private void handleUser(String[] parts) {
        if (parts.length == 1 || (parts.length >= 2 && parts[1].equalsIgnoreCase("current"))) {
            out.printf("Текущий пользователь (UUID): %s%n", currentUser.getId());
            return;
        }

        String sub = parts[1].toLowerCase();
        if ("new".equals(sub)) {
            currentUser = User.createNew();
            out.printf("Создан новый пользователь: %s%n", currentUser.getId());
        } else if ("switch".equals(sub)) {
            if (parts.length < 3) {
                out.println("Использование: user switch <UUID>");
                return;
            }
            try {
                UUID uuid = UUID.fromString(parts[2]);
                currentUser = new User(uuid);
                out.printf("Переключено на пользователя: %s%n", currentUser.getId());
            } catch (IllegalArgumentException e) {
                out.println("Некорректный формат UUID. Пример: 123e4567-e89b-12d3-a456-426614174000");
            }
        } else {
            out.println("Неизвестная подкоманда. Используйте: user current, user new, user switch <UUID>");
        }
    }

    private void printHelp() {
        out.println("Справка по командам:");
        out.println("  shorten <URL> [лимит] [TTL_сек] - Создать короткую ссылку (пример: shorten https://ya.ru 5 120)");
        out.println("  open <код_или_ссылка>           - Открыть ссылку в браузере (пример: open clck.ru/xYz123)");
        out.println("  list                            - Список ссылок текущего пользователя");
        out.println("  info <код_или_ссылка>           - Информация о ссылке");
        out.println("  edit <код_или_ссылка> <лимит>   - Изменить лимит кликов (только для владельца)");
        out.println("  delete <код_или_ссылка>         - Удалить ссылку (только для владельца)");
        out.println("  user current                    - Показать свой UUID");
        out.println("  user new                        - Создать нового пользователя");
        out.println("  user switch <UUID>              - Войти под другим UUID");
        out.println("  help                            - Показать эту справку");
        out.println("  exit                            - Выйти из приложения");
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }
}
