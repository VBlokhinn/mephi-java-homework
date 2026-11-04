package ru.mephi.finance.cli;

import ru.mephi.finance.model.Operation;
import ru.mephi.finance.model.OperationType;
import ru.mephi.finance.model.User;
import ru.mephi.finance.model.Wallet;
import ru.mephi.finance.service.AuthService;
import ru.mephi.finance.service.FinanceService;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public class FinanceCliController {
    private final AuthService authService;
    private final FinanceService financeService;
    private final Scanner scanner;
    private final PrintStream out;
    private User currentUser;

    public FinanceCliController(AuthService authService,
                                FinanceService financeService,
                                InputStream in,
                                PrintStream out) {
        this.authService = authService;
        this.financeService = financeService;
        this.scanner = new Scanner(in);
        this.out = out;
    }

    public void run() {
        out.println("Система управления личными финансами");
        out.println("Войдите в аккаунт командой 'login <логин> <пароль>' или зарегистрируйтесь 'register <логин> <пароль>'.");
        out.println("Введите 'help' для списка команд или 'exit' для выхода.");

        boolean running = true;
        while (running && scanner.hasNextLine()) {
            String prompt = (currentUser != null) ? "[" + currentUser.getLogin() + "] > " : "[гость] > ";
            out.print(prompt);
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }

            try {
                running = handleCommandLine(line);
            } catch (Exception e) {
                out.printf("Ошибка: %s%n", e.getMessage());
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
            case "register":
                handleRegister(parts);
                break;
            case "login":
                handleLogin(parts);
                break;
            case "logout":
                handleLogout();
                break;
            case "income":
                handleIncome(parts);
                break;
            case "expense":
                handleExpense(parts);
                break;
            case "budget":
                handleBudget(parts);
                break;
            case "stats":
            case "summary":
                handleStats();
                break;
            case "history":
                handleHistory(parts);
                break;
            case "export":
                handleExport(parts);
                break;
            case "import":
                handleImport(parts);
                break;
            case "exit":
            case "quit":
                if (currentUser != null) {
                    financeService.saveWallet(currentUser.getLogin());
                }
                out.println("Завершение работы.");
                return false;
            default:
                out.printf("Неизвестная команда '%s'. Введите 'help' для справки.%n", command);
        }
        return true;
    }

    private void handleRegister(String[] parts) {
        if (parts.length < 3) {
            out.println("Использование: register <логин> <пароль>");
            return;
        }
        try {
            User user = authService.register(parts[1], parts[2]);
            this.currentUser = user;
            out.printf("Пользователь '%s' успешно зарегистрирован и авторизован.%n", user.getLogin());
        } catch (IllegalArgumentException e) {
            out.printf("Ошибка регистрации: %s%n", e.getMessage());
        }
    }

    private void handleLogin(String[] parts) {
        if (parts.length < 3) {
            out.println("Использование: login <логин> <пароль>");
            return;
        }
        Optional<User> userOpt = authService.login(parts[1], parts[2]);
        if (userOpt.isPresent()) {
            this.currentUser = userOpt.get();
            out.printf("Успешный вход в систему. Добро пожаловать, %s!%n", currentUser.getLogin());
        } else {
            out.println("Неверный логин или пароль.");
        }
    }

    private void handleLogout() {
        if (currentUser == null) {
            out.println("Вы не авторизованы.");
            return;
        }
        financeService.saveWallet(currentUser.getLogin());
        out.printf("Сессия пользователя '%s' завершена.%n", currentUser.getLogin());
        this.currentUser = null;
    }

    private void handleIncome(String[] parts) {
        if (!checkAuth()) return;
        if (parts.length < 3) {
            out.println("Использование: income <сумма> <категория> [описание]");
            out.println("Пример: income 50000 Зарплата Аванс");
            return;
        }

        double amount = parseAmount(parts[1]);
        if (amount <= 0) return;

        String category = parts[2];
        String description = extractDescription(parts, 3);

        Operation op = financeService.addIncome(currentUser.getLogin(), amount, category, LocalDate.now(), description);
        out.printf("Доход добавлен: +%.2f руб. (категория: %s). Текущий баланс: %.2f руб.%n",
                op.getAmount(), op.getCategory(), financeService.getWallet(currentUser.getLogin()).getBalance());
    }

    private void handleExpense(String[] parts) {
        if (!checkAuth()) return;
        if (parts.length < 3) {
            out.println("Использование: expense <сумма> <категория> [описание]");
            out.println("Пример: expense 1500 Еда Обед в кафе");
            return;
        }

        double amount = parseAmount(parts[1]);
        if (amount <= 0) return;

        String category = parts[2];
        String description = extractDescription(parts, 3);

        Operation op = financeService.addExpense(currentUser.getLogin(), amount, category, LocalDate.now(), description);
        out.printf("Расход добавлен: -%.2f руб. (категория: %s). Текущий баланс: %.2f руб.%n",
                op.getAmount(), op.getCategory(), financeService.getWallet(currentUser.getLogin()).getBalance());
    }

    private void handleBudget(String[] parts) {
        if (!checkAuth()) return;
        if (parts.length < 2) {
            out.println("Использование: budget set <категория> <лимит>  или  budget remove <категория>");
            return;
        }

        String action = parts[1].toLowerCase();
        if ("set".equals(action)) {
            if (parts.length < 4) {
                out.println("Использование: budget set <категория> <лимит>");
                out.println("Пример: budget set Еда 10000");
                return;
            }
            double limit = parseAmount(parts[3]);
            if (limit <= 0) return;

            String category = parts[2];
            financeService.setCategoryBudget(currentUser.getLogin(), category, limit);
            out.printf("Бюджет для категории '%s' установлен: %.2f руб.%n", category, limit);

        } else if ("remove".equals(action)) {
            if (parts.length < 3) {
                out.println("Использование: budget remove <категория>");
                return;
            }
            String category = parts[2];
            boolean removed = financeService.removeCategoryBudget(currentUser.getLogin(), category);
            if (removed) {
                out.printf("Бюджет для категории '%s' удален.%n", category);
            } else {
                out.printf("Бюджет для категории '%s' не был задан.%n", category);
            }
        } else {
            out.println("Неизвестное действие для бюджета. Доступны: set, remove.");
        }
    }

    private void handleStats() {
        if (!checkAuth()) return;

        Wallet wallet = financeService.getWallet(currentUser.getLogin());
        out.println("Финансовая сводка кошелька");
        out.printf("Общий доход   : %.2f руб.%n", wallet.getTotalIncome());
        out.printf("Общий расход  : %.2f руб.%n", wallet.getTotalExpense());
        out.printf("Текущий баланс: %.2f руб.%n", wallet.getBalance());

        Set<String> categories = wallet.getAllCategories();
        if (categories.isEmpty()) {
            out.println("Операций и бюджетов пока нет.");
            return;
        }

        out.println("\nСтатистика по категориям:");
        out.printf("%-18s %-12s %-12s %-14s %s%n", "Категория", "Доход", "Расход", "Бюджет", "Остаток бюджета");

        for (String cat : categories) {
            double income = wallet.getCategoryIncome(cat);
            double expense = wallet.getCategoryExpense(cat);
            Double budget = wallet.getCategoryBudget(cat);

            String budgetStr = (budget != null) ? String.format("%.2f", budget) : "не задан";
            String remainingStr;
            if (budget == null) {
                remainingStr = "-";
            } else {
                double remaining = budget - expense;
                if (remaining < 0) {
                    remainingStr = String.format("перерасход %.2f", Math.abs(remaining));
                } else {
                    remainingStr = String.format("%.2f", remaining);
                }
            }

            out.printf("%-18s %-12.2f %-12.2f %-14s %s%n",
                    cat, income, expense, budgetStr, remainingStr);
        }
    }

    private void handleHistory(String[] parts) {
        if (!checkAuth()) return;

        LocalDate from = null;
        LocalDate to = null;
        List<String> categories = null;

        if (parts.length >= 2) {
            try {
                from = LocalDate.parse(parts[1]);
            } catch (DateTimeParseException e) {
                categories = List.of(parts[1]);
            }
        }
        if (parts.length >= 3 && from != null) {
            try {
                to = LocalDate.parse(parts[2]);
            } catch (DateTimeParseException e) {
                categories = List.of(parts[2]);
            }
        }
        if (parts.length >= 4) {
            categories = List.of(parts[3]);
        }

        List<Operation> list = financeService.getFilteredOperations(currentUser.getLogin(), from, to, categories);
        if (list.isEmpty()) {
            out.println("По заданным критериям операций не найдено.");
            return;
        }

        out.printf("История операций (всего: %d):%n", list.size());
        out.printf("%-10s %-8s %-12s %-16s %s%n", "Дата", "Тип", "Сумма", "Категория", "Описание");
        for (Operation op : list) {
            String sign = op.getType() == OperationType.INCOME ? "+" : "-";
            out.printf("%-10s %-8s %-12s %-16s %s%n",
                    op.getDate(),
                    op.getType().getTitle(),
                    sign + String.format("%.2f", op.getAmount()),
                    op.getCategory(),
                    op.getDescription());
        }
    }

    private void handleExport(String[] parts) {
        if (!checkAuth()) return;
        if (parts.length < 2) {
            out.println("Использование: export <имя_файла.csv>");
            out.println("Пример: export report.csv");
            return;
        }

        Path path = Paths.get(parts[1]);
        try {
            financeService.exportToCsv(currentUser.getLogin(), path);
            out.printf("Отчет успешно экспортирован в файл: %s%n", path.toAbsolutePath());
        } catch (Exception e) {
            out.printf("Ошибка экспорта: %s%n", e.getMessage());
        }
    }

    private void handleImport(String[] parts) {
        if (!checkAuth()) return;
        if (parts.length < 2) {
            out.println("Использование: import <имя_файла.csv>");
            out.println("Пример: import report.csv");
            return;
        }

        Path path = Paths.get(parts[1]);
        try {
            int count = financeService.importFromCsv(currentUser.getLogin(), path);
            out.printf("Импортировано операций: %d. Файл: %s%n", count, path.toAbsolutePath());
        } catch (Exception e) {
            out.printf("Ошибка импорта: %s%n", e.getMessage());
        }
    }

    private void printHelp() {
        out.println("Доступные команды:");
        if (currentUser == null) {
            out.println("  register <логин> <пароль>        - Регистрация нового пользователя");
            out.println("  login <логин> <пароль>           - Вход в систему");
            out.println("  help                             - Показать справку");
            out.println("  exit                             - Выход из программы");
        } else {
            out.println("  income <сумма> <кат> [описание]  - Добавить доход (пример: income 50000 Зарплата Аванс)");
            out.println("  expense <сумма> <кат> [описание] - Добавить расход (пример: expense 1200 Еда Обед)");
            out.println("  budget set <категория> <лимит>   - Установить бюджет (пример: budget set Еда 15000)");
            out.println("  budget remove <категория>        - Удалить бюджет категории");
            out.println("  stats                            - Сводка по доходам, расходам, балансу и категориям");
            out.println("  history [от_даты] [до_даты] [кат]- История операций с фильтром по датам и категории");
            out.println("  export <файл.csv>                - Экспорт истории операций в CSV");
            out.println("  import <файл.csv>                - Импорт операций из CSV");
            out.println("  logout                           - Выход из текущего аккаунта");
            out.println("  help                             - Показать справку");
            out.println("  exit                             - Сохранить данные и выйти");
        }
    }

    private boolean checkAuth() {
        if (currentUser == null) {
            out.println("Для выполнения операции необходимо войти в систему: 'login <логин> <пароль>'.");
            return false;
        }
        return true;
    }

    private double parseAmount(String str) {
        try {
            double val = Double.parseDouble(str.replace(',', '.'));
            if (val <= 0) {
                out.println("Ошибка: сумма должна быть строго больше нуля.");
                return -1;
            }
            return val;
        } catch (NumberFormatException e) {
            out.println("Ошибка: некорректный числовой формат суммы.");
            return -1;
        }
    }

    private String extractDescription(String[] parts, int startIndex) {
        if (parts.length <= startIndex) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < parts.length; i++) {
            if (i > startIndex) sb.append(" ");
            sb.append(parts[i]);
        }
        return sb.toString();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }
}
