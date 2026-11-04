package ru.mephi.finance.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mephi.finance.model.Operation;
import ru.mephi.finance.model.OperationType;
import ru.mephi.finance.model.Wallet;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FileWalletRepositoryTest {
    private FileWalletRepository repository;
    private Path tempDirectory;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        this.tempDirectory = tempDir;
        repository = new FileWalletRepository(tempDir.resolve("wallets"));
    }

    @Test
    @DisplayName("Сохранение кошелька в JSON и корректное чтение после перезагрузки репозитория")
    void testSaveAndReloadWallet() {
        Wallet wallet = new Wallet("ivan");
        wallet.setBudget("Еда", 15000.0);
        wallet.addOperation(new Operation(OperationType.INCOME, 30000.0, "Зарплата", LocalDate.now(), "Аванс"));
        wallet.addOperation(new Operation(OperationType.EXPENSE, 1500.0, "Еда", LocalDate.now(), "Обед"));

        repository.save(wallet);

        // Создаем новый экземпляр репозитория, читающий ту же директорию
        FileWalletRepository reloadedRepo = new FileWalletRepository(tempDirectory.resolve("wallets"));
        Optional<Wallet> loaded = reloadedRepo.findByUserLogin("ivan");

        assertTrue(loaded.isPresent());
        Wallet loadedWallet = loaded.get();
        assertEquals(28500.0, loadedWallet.getBalance(), 0.001);
        assertEquals(15000.0, loadedWallet.getCategoryBudget("Еда"));
        assertEquals(2, loadedWallet.getOperations().size());
    }

    @Test
    @DisplayName("Удаление кошелька пользователя и очистка файла")
    void testDeleteWallet() {
        Wallet wallet = new Wallet("petr");
        repository.save(wallet);

        assertTrue(repository.findByUserLogin("petr").isPresent());
        assertTrue(repository.deleteByUserLogin("petr"));
        assertTrue(repository.findByUserLogin("petr").isEmpty());
    }
}
