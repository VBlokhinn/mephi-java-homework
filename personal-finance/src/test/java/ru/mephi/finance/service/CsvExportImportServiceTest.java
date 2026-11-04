package ru.mephi.finance.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mephi.finance.model.Operation;
import ru.mephi.finance.model.OperationType;
import ru.mephi.finance.model.Wallet;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvExportImportServiceTest {

    @Test
    @DisplayName("Экспорт и последующий импорт операций через CSV")
    void testExportAndImportOperations(@TempDir Path tempDir) throws IOException {
        CsvExportImportService service = new CsvExportImportService();
        Path csvFile = tempDir.resolve("export.csv");

        Wallet sourceWallet = new Wallet("user1");
        sourceWallet.addOperation(new Operation(OperationType.INCOME, 5000.0, "Зарплата", LocalDate.of(2026, 5, 1), "Аванс"));
        sourceWallet.addOperation(new Operation(OperationType.EXPENSE, 1200.0, "Еда", LocalDate.of(2026, 5, 2), "Обед в кафе"));

        // Экспорт
        service.exportOperations(sourceWallet, csvFile);
        assertTrue(csvFile.toFile().exists());

        // Импорт
        List<Operation> imported = service.importOperations(csvFile);
        assertEquals(2, imported.size());

        Operation op1 = imported.get(0);
        assertEquals(OperationType.INCOME, op1.getType());
        assertEquals(5000.0, op1.getAmount(), 0.001);
        assertEquals("Зарплата", op1.getCategory());
        assertEquals("Аванс", op1.getDescription());

        Operation op2 = imported.get(1);
        assertEquals(OperationType.EXPENSE, op2.getType());
        assertEquals(1200.0, op2.getAmount(), 0.001);
        assertEquals("Еда", op2.getCategory());
    }

    @Test
    @DisplayName("Импорт из несуществующего файла выбрасывает исключение")
    void testImportNonExistentFileThrowsException(@TempDir Path tempDir) {
        CsvExportImportService service = new CsvExportImportService();
        Path missing = tempDir.resolve("missing.csv");

        assertThrows(IllegalArgumentException.class, () -> service.importOperations(missing));
    }

    @Test
    @DisplayName("Корректный экспорт пустого кошелька")
    void testExportEmptyWallet(@TempDir Path tempDir) throws IOException {
        CsvExportImportService service = new CsvExportImportService();
        Path csvFile = tempDir.resolve("empty.csv");

        Wallet emptyWallet = new Wallet("emptyUser");
        service.exportOperations(emptyWallet, csvFile);

        List<Operation> imported = service.importOperations(csvFile);
        assertTrue(imported.isEmpty());
    }
}
