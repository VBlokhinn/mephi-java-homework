package ru.mephi.shortener.model;

public class RedirectResult {
    public enum Status {
        SUCCESS,
        LIMIT_EXHAUSTED,
        EXPIRED,
        NOT_FOUND
    }

    private final Status status;
    private final String originalUrl;
    private final String message;
    private final ShortUrl shortUrl;

    public RedirectResult(Status status, String originalUrl, String message, ShortUrl shortUrl) {
        this.status = status;
        this.originalUrl = originalUrl;
        this.message = message;
        this.shortUrl = shortUrl;
    }

    public static RedirectResult success(String originalUrl, ShortUrl shortUrl) {
        return new RedirectResult(Status.SUCCESS, originalUrl, "Успешный переход", shortUrl);
    }

    public static RedirectResult limitExhausted(ShortUrl shortUrl) {
        int limit = shortUrl != null ? shortUrl.getClickLimit() : 0;
        return new RedirectResult(
                Status.LIMIT_EXHAUSTED,
                null,
                "Лимит переходов исчерпан (" + limit + "). Ссылка заблокирована.",
                shortUrl
        );
    }

    public static RedirectResult expired(ShortUrl shortUrl) {
        return new RedirectResult(
                Status.EXPIRED,
                null,
                "Срок действия ссылки истек. Ссылка удалена.",
                shortUrl
        );
    }

    public static RedirectResult notFound(String code) {
        return new RedirectResult(
                Status.NOT_FOUND,
                null,
                "Ссылка '" + code + "' не найдена.",
                null
        );
    }

    public Status getStatus() {
        return status;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getMessage() {
        return message;
    }

    public ShortUrl getShortUrl() {
        return shortUrl;
    }

    public boolean isSuccessful() {
        return status == Status.SUCCESS;
    }
}
