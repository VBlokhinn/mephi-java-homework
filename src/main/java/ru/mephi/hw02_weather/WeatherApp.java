package ru.mephi.hw02_weather;

import java.net.URI;
import java.net.http.*;
import java.util.Locale;

public class WeatherApp {

    private static final String API_KEY = System.getenv("YANDEX_WEATHER_KEY");


    public static String fetchForecastJson(double lat, double lon, int limit) {
        try {
            String uri = String.format(Locale.US,
                    "https://api.weather.yandex.ru/v2/forecast?lat=%f&lon=%f&limit=%d", lat, lon, limit);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .header("X-Yandex-Weather-Key", API_KEY)
                    .GET()
                    .build();

            return HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString())
                    .body();
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
            return "";
        }
    }

    public static int extractCurrentTemperature(String json) {
        int factIndex = json.indexOf("\"fact\"");

        int tempIndex = json.indexOf("\"temp\":", factIndex);
        int startIndex = tempIndex + "\"temp\":".length();

        int endIndex = json.indexOf(",", startIndex);
        String tempString = json.substring(startIndex, endIndex).trim();
        return Integer.parseInt(tempString);
    }

    public static double calculateAverageDayTemperature(String json, int limit) {
        int searchIndex = 0;
        double sum = 0;
        int count = 0;
        for (int i = 0; i < limit; i++) {
            int dayIndex = json.indexOf("\"day\":", searchIndex);
            if (dayIndex == -1) {
                break;
            }
            int tempAvgIndex = json.indexOf("\"temp_avg\":", dayIndex);
            if (tempAvgIndex == -1) {
                break;
            }
            int startIndex = tempAvgIndex + "\"temp_avg\":".length();
            int endIndex = json.indexOf(",", startIndex);
            String tempStr = json.substring(startIndex, endIndex).trim();
            sum += Double.parseDouble(tempStr);
            count++;

            searchIndex = endIndex;
        }
        return count > 0 ? sum / count : 0.0;
    }

    public static void main(String[] args) {
        double lat = 55.649823;
        double lon = 37.664364;
        int limit = 7;
        String json = fetchForecastJson(lat, lon, limit);
        System.out.println("Полный JSON ответ");
        System.out.println(json);
        System.out.println("Текущая температура: " + extractCurrentTemperature(json) + " °C");
        System.out.printf(Locale.US, "Средняя температура за %d суток: %.2f °C%n",
                limit, calculateAverageDayTemperature(json, limit));
    }
}