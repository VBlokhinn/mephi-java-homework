package ru.mephi.finance.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mephi.finance.model.Operation;
import ru.mephi.finance.model.User;
import ru.mephi.finance.model.Wallet;
import ru.mephi.finance.repository.FileUserRepository;
import ru.mephi.finance.repository.FileWalletRepository;
import ru.mephi.finance.service.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FinanceIntegrationTest {
    private AuthService authService;
    private FinanceService financeService;
    private CsvExportImportService csvService;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        FileUserRepository userRepo = new FileUserRepository(tempDir.resolve("users.json"));
        FileWalletRepository walletRepo = new FileWalletRepository(tempDir.resolve("wallets"));
        NotificationService notificationService = new NotificationService();
        csvService = new CsvExportImportService();

        authService = new AuthService(userRepo);
        financeService = new FinanceServiceImpl(walletRepo, notificationService, csvService);
    }

    @Test
    @DisplayName("Сквозной сценарий пользователя: регистрация -> доход -> бюджет -> расход -> экспорт")
    void testFullUserScenario(@TempDir Path tempDir) throws IOException {
        // 1. Регистрация и вход
        User user = authService.register("vladislav", "pass2026");
        Optional<User> loggedIn = authService.login("vladislav", "pass2026");
        assertTrue(loggedIn.isPresent());

        // 2. Добавление дохода
        financeService.addIncome("vladislav", 100000.0, "Зарплата", LocalDate.now(), "Месячный оклад");

        // 3. Установка бюджета
        financeService.setCategoryBudget("vladislav", "Еда", 25000.0);

        // 4. Добавление расходов
        financeService.addExpense("vladislav", 5000.0, "Еда", LocalDate.now(), "Супермаркет");
        financeService.addExpense("vladislav", 15000.0, "Еда", LocalDate.now(), "Ресторан");

        // 5. Проверка баланса и расходов
        Wallet wallet = financeService.getWallet("vladislav");
        assertEquals(80000.0, wallet.getBalance(), 0.001);
        assertEquals(20000.0, wallet.getCategoryExpense("Еда"), 0.001);
        assertEquals(25000.0, wallet.getCategoryBudget("Еда"));

        // 6. Экспорт
        Path csvExport = tempDir.resolve("vladislav_report.csv");
        financeService.exportToCsv("vladislav", csvExport);
        assertTrue(csvExport.toFile().exists());

        // 7. Импорт обратно для проверки
        List<Operation> imported = csvService.importOperations(csvExport);
        assertEquals(3, imported.size());
    }

    @Test
    @DisplayName("Изоляция кошельков и данных между разными пользователями")
    void testMultiUserIsolationScenario() {
        authService.register("user_a", "passA");
        authService.register("user_b", "passB");

        financeService.addIncome("user_a", 50000.0, "Зарплата", LocalDate.now(), null);
        financeService.setCategoryBudget("user_a", "Аренда", 30000.0);

        financeService.addIncome("user_b", 20000.0, "Стипендия", LocalDate.now(), null);
        financeService.setCategoryBudget("user_b", "Книги", 5000.0);

        Wallet walletA = financeService.getWallet("user_a");
        Wallet walletB = financeService.getWallet("user_b");

        assertEquals(50000.0, walletA.getBalance(), 0.001);
        assertEquals(20000.0, walletB.getBalance(), 0.001);

        assertEquals(30000.0, walletA.getCategoryBudget("Аренда"));
        assertNull(walletB.getCategoryBudget("Аренда"));

        assertEquals(5000.0, walletB.getCategoryBudget("Книги"));
        assertNull(walletA.getCategoryBudget("Книги"));
    }
}
