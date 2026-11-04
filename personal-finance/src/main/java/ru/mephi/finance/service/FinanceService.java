package ru.mephi.finance.service;

import ru.mephi.finance.model.Operation;
import ru.mephi.finance.model.Wallet;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public interface FinanceService {

    Wallet getWallet(String login);

    void saveWallet(String login);

    Operation addIncome(String login, double amount, String category, LocalDate date, String description);

    Operation addExpense(String login, double amount, String category, LocalDate date, String description);

    void setCategoryBudget(String login, String category, double limit);

    boolean removeCategoryBudget(String login, String category);

    List<Operation> getFilteredOperations(String login, LocalDate from, LocalDate to, List<String> categories);

    void exportToCsv(String login, Path targetPath) throws IOException;

    int importFromCsv(String login, Path sourcePath) throws IOException;
}
