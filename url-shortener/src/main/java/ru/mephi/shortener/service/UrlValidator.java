package ru.mephi.shortener.service;

import java.net.URI;
import java.net.URISyntaxException;

public class UrlValidator {

    public static boolean isValid(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String trimmed = url.trim();
        try {
            URI uri = new URI(trimmed);
            String scheme = uri.getScheme();
            if (scheme == null) {
                return false;
            }
            scheme = scheme.toLowerCase();
            if (!scheme.equals("http") && !scheme.equals("https")) {
                return false;
            }
            if (uri.getHost() == null || uri.getHost().trim().isEmpty()) {
                return false;
            }
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
