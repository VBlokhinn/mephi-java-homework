package ru.mephi.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mephi.finance.model.Operation;
import ru.mephi.finance.model.Wallet;
import ru.mephi.finance.repository.FileWalletRepository;
import ru.mephi.finance.repository.WalletRepository;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FinanceServiceTest {
    private WalletRepository walletRepository;
    private NotificationService notificationService;
    private FinanceService financeService;
    private final List<String> notifications = new ArrayList<>();
    private final String login = "tester";

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        walletRepository = new FileWalletRepository(tempDir.resolve("wallets"));
        notificationService = new NotificationService();
        notifications.clear();
        notificationService.addListener(notifications::add);
        CsvExportImportService csvService = new CsvExportImportService();
        financeService = new FinanceServiceImpl(walletRepository, notificationService, csvService);
    }

    @Test
    @DisplayName("Добавление дохода и корректный пересчет баланса")
    void testAddIncomeAndBalance() {
        financeService.addIncome(login, 10000.0, "Зарплата", LocalDate.now(), "Бонус");
        Wallet wallet = financeService.getWallet(login);

        assertEquals(10000.0, wallet.getBalance(), 0.001);
        assertEquals(10000.0, wallet.getTotalIncome(), 0.001);
        assertEquals(0.0, wallet.getTotalExpense(), 0.001);
    }

    @Test
    @DisplayName("Добавление расхода и уменьшение баланса")
    void testAddExpenseAndBalance() {
        financeService.addIncome(login, 5000.0, "Зарплата", LocalDate.now(), null);
        financeService.addExpense(login, 2000.0, "Еда", LocalDate.now(), "Продукты");

        Wallet wallet = financeService.getWallet(login);
        assertEquals(3000.0, wallet.getBalance(), 0.001);
        assertEquals(2000.0, wallet.getCategoryExpense("Еда"), 0.001);
    }

    @Test
    @DisplayName("Установка и удаление бюджета по категории")
    void testCategoryBudgetTracking() {
        financeService.setCategoryBudget(login, "Транспорт", 3000.0);
        Wallet wallet = financeService.getWallet(login);

        assertEquals(3000.0, wallet.getCategoryBudget("Транспорт"));
        assertTrue(financeService.removeCategoryBudget(login, "Транспорт"));
        assertNull(wallet.getCategoryBudget("Транспорт"));
    }

    @Test
    @DisplayName("Оповещение о превышении 100% лимита бюджета категории")
    void testBudgetExceededNotification() {
        financeService.addIncome(login, 50000.0, "Зарплата", LocalDate.now(), null);
        financeService.setCategoryBudget(login, "Кафе", 2000.0);

        financeService.addExpense(login, 2500.0, "Кафе", LocalDate.now(), "Ужин");

        assertTrue(notifications.stream().anyMatch(n -> n.contains("Превышен бюджет") && n.contains("Кафе")));
    }

    @Test
    @DisplayName("Предупреждающее оповещение при достижении 80% лимита бюджета")
    void testBudget80PercentWarningNotification() {
        financeService.addIncome(login, 50000.0, "Зарплата", LocalDate.now(), null);
        financeService.setCategoryBudget(login, "Одежда", 10000.0);

        // 8500 составляет 85% (>80%) от 10000
        financeService.addExpense(login, 8500.0, "Одежда", LocalDate.now(), "Куртка");

        assertTrue(notifications.stream().anyMatch(n -> n.contains("достигли") && n.contains("85")));
    }

    @Test
    @DisplayName("Оповещение при нулевом или отрицательном балансе кошелька")
    void testLowBalanceNotification() {
        financeService.addIncome(login, 1000.0, "Подработка", LocalDate.now(), null);

        // Расход больше баланса
        financeService.addExpense(login, 1500.0, "Разное", LocalDate.now(), "Покупка");

        assertTrue(notifications.stream().anyMatch(n -> n.contains("Отрицательный баланс")));
    }

    @Test
    @DisplayName("Фильтрация операций по периоду дат и категории")
    void testFilteringByDateAndCategory() {
        LocalDate d1 = LocalDate.of(2026, 1, 10);
        LocalDate d2 = LocalDate.of(2026, 2, 15);
        LocalDate d3 = LocalDate.of(2026, 3, 20);

        financeService.addIncome(login, 1000.0, "Зарплата", d1, "Январь");
        financeService.addExpense(login, 200.0, "Еда", d2, "Февраль");
        financeService.addExpense(login, 500.0, "Транспорт", d3, "Март");

        // Фильтр по диапазону дат
        List<Operation> inRange = financeService.getFilteredOperations(login,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), null);
        assertEquals(1, inRange.size());
        assertEquals("Еда", inRange.get(0).getCategory());

        // Фильтр по категории
        List<Operation> foodOnly = financeService.getFilteredOperations(login, null, null, List.of("Еда"));
        assertEquals(1, foodOnly.size());
        assertEquals(200.0, foodOnly.get(0).getAmount());
    }
}
