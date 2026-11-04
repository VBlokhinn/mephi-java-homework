package ru.mephi.finance;

import ru.mephi.finance.cli.FinanceCliController;
import ru.mephi.finance.repository.FileUserRepository;
import ru.mephi.finance.repository.FileWalletRepository;
import ru.mephi.finance.repository.UserRepository;
import ru.mephi.finance.repository.WalletRepository;
import ru.mephi.finance.service.*;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        Path dataDir = Paths.get("data");
        UserRepository userRepository = new FileUserRepository(dataDir.resolve("users.json"));
        WalletRepository walletRepository = new FileWalletRepository(dataDir.resolve("wallets"));

        AuthService authService = new AuthService(userRepository);
        NotificationService notificationService = new NotificationService();
        CsvExportImportService csvService = new CsvExportImportService();
        FinanceService financeService = new FinanceServiceImpl(walletRepository, notificationService, csvService);

        FinanceCliController controller = new FinanceCliController(authService, financeService, System.in, System.out);

        controller.run();
    }
}
