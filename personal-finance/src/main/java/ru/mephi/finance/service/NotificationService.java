package ru.mephi.finance.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NotificationService {
    private final List<Consumer<String>> listeners = new ArrayList<>();

    public NotificationService() {
        listeners.add(msg -> System.out.println("[Оповещение] " + msg));
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

    public void onBudgetWarning(String category, double currentExpense, double limit) {
        double percent = (currentExpense / limit) * 100.0;
        notify(String.format("Внимание: расходы по категории '%s' достигли %.1f%% от лимита (%.2f из %.2f).", category, percent, currentExpense, limit));
    }

    public void onBudgetExceeded(String category, double currentExpense, double limit) {
        double overspend = currentExpense - limit;
        notify(String.format("Превышен бюджет по категории '%s'! Лимит: %.2f, израсходовано: %.2f (перерасход: %.2f).", category, limit, currentExpense, overspend));
    }

    public void onLowBalance(double balance) {
        if (balance < 0) {
            notify(String.format("Отрицательный баланс кошелька: %.2f. Расходы превышают доходы.", balance));
        } else if (balance == 0) {
            notify("Баланс кошелька равен 0.00. Средства исчерпаны.");
        }
    }
}
