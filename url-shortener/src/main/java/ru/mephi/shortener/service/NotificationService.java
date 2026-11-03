package ru.mephi.shortener.service;

import ru.mephi.shortener.model.ShortUrl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


public class NotificationService {
    private final List<Consumer<String>> listeners = new ArrayList<>();

    public NotificationService() {
        listeners.add(msg -> System.out.println("[Инфо] " + msg));
    }

    public void addListener(Consumer<String> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void notify(String message) {
        for (Consumer<String> listener : listeners) {
            try {
                listener.accept(message);
            } catch (Exception ignored) {
            }
        }
    }

    public void onLimitExhausted(ShortUrl url) {
        notify(String.format("Лимит переходов для ссылки '%s' (%s) исчерпан. Доступ заблокирован.",
                url.getShortCode(), url.getOriginalUrl()));
    }

    public void onUrlExpired(ShortUrl url) {
        notify(String.format("Срок действия ссылки '%s' (%s) истек. Ссылка удалена.",
                url.getShortCode(), url.getOriginalUrl()));
    }

    public void onLimitUpdated(ShortUrl url, int oldLimit, int newLimit) {
        notify(String.format("Лимит для ссылки '%s' обновлен с %d на %d.",
                url.getShortCode(), oldLimit, newLimit));
    }
}
