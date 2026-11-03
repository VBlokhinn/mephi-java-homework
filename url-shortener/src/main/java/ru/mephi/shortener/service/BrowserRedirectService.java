package ru.mephi.shortener.service;

import java.awt.Desktop;
import java.net.URI;

public class BrowserRedirectService {

    public boolean openInBrowser(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        try {
            URI uri = URI.create(url.trim());
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(uri);
                    return true;
                }
            }

            System.out.printf("Браузер недоступен в текущей среде. Исходный URL: %s%n", uri);
            return true;
        } catch (Exception e) {
            System.err.printf("Не удалось открыть браузер: %s (URL: %s)%n", e.getMessage(), url);
            return false;
        }
    }
}
