package ru.mephi.finance.service;

import ru.mephi.finance.model.Operation;
import ru.mephi.finance.model.OperationType;
import ru.mephi.finance.model.Wallet;
import ru.mephi.finance.repository.WalletRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class FinanceServiceImpl implements FinanceService {
    private final WalletRepository walletRepository;
    private final NotificationService notificationService;
    private final CsvExportImportService csvService;

    public FinanceServiceImpl(WalletRepository walletRepository,
                              NotificationService notificationService,
                              CsvExportImportService csvService) {
        this.walletRepository = Objects.requireNonNull(walletRepository, "walletRepository cannot be null");
        this.notificationService = Objects.requireNonNull(notificationService, "notificationService cannot be null");
        this.csvService = Objects.requireNonNull(csvService, "csvService cannot be null");
    }

    @Override
    public synchronized Wallet getWallet(String login) {
        if (login == null || login.trim().isEmpty()) {
            throw new IllegalArgumentException("Логин не может быть пустым");
        }
        String cleanLogin = login.trim().toLowerCase();
        return walletRepository.findByUserLogin(cleanLogin)
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet(cleanLogin);
                    walletRepository.save(newWallet);
                    return newWallet;
                });
    }

    @Override
    public synchronized void saveWallet(String login) {
        Wallet wallet = getWallet(login);
        walletRepository.save(wallet);
    }

    @Override
    public synchronized Operation addIncome(String login, double amount, String category, LocalDate date, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Сумма дохода должна быть больше нуля");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Категория дохода не может быть пустой");
        }

        Wallet wallet = getWallet(login);
        Operation operation = new Operation(OperationType.INCOME, amount, category.trim(), date, description);
        wallet.addOperation(operation);
        walletRepository.save(wallet);
        return operation;
    }

    @Override
    public synchronized Operation addExpense(String login, double amount, String category, LocalDate date, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Сумма расхода должна быть больше нуля");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Категория расхода не может быть пустой");
        }

        String cleanCategory = category.trim();
        Wallet wallet = getWallet(login);

        double expenseBefore = wallet.getCategoryExpense(cleanCategory);
        Double budget = wallet.getCategoryBudget(cleanCategory);

        Operation operation = new Operation(OperationType.EXPENSE, amount, cleanCategory, date, description);
        wallet.addOperation(operation);

        double expenseAfter = expenseBefore + amount;

        if (budget != null && budget > 0) {
            if (expenseAfter > budget) {
                notificationService.onBudgetExceeded(cleanCategory, expenseAfter, budget);
            } else if (expenseAfter >= 0.8 * budget && expenseBefore < 0.8 * budget) {
                notificationService.onBudgetWarning(cleanCategory, expenseAfter, budget);
            }
        }

        double newBalance = wallet.getBalance();
        if (newBalance <= 0) {
            notificationService.onLowBalance(newBalance);
        }

        walletRepository.save(wallet);
        return operation;
    }

    @Override
    public synchronized void setCategoryBudget(String login, String category, double limit) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Категория не может быть пустой");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Лимит бюджета должен быть больше нуля");
        }

        Wallet wallet = getWallet(login);
        wallet.setBudget(category.trim(), limit);
        walletRepository.save(wallet);
    }

    @Override
    public synchronized boolean removeCategoryBudget(String login, String category) {
        if (category == null) {
            return false;
        }
        Wallet wallet = getWallet(login);
        boolean removed = wallet.removeBudget(category.trim());
        if (removed) {
            walletRepository.save(wallet);
        }
        return removed;
    }

    @Override
    public synchronized List<Operation> getFilteredOperations(String login, LocalDate from, LocalDate to, List<String> categories) {
        Wallet wallet = getWallet(login);
        Set<String> catSet = (categories != null && !categories.isEmpty())
                ? categories.stream().map(c -> c.trim().toLowerCase()).collect(Collectors.toSet())
                : null;

        return wallet.getOperations().stream()
                .filter(op -> from == null || !op.getDate().isBefore(from))
                .filter(op -> to == null || !op.getDate().isAfter(to))
                .filter(op -> catSet == null || catSet.contains(op.getCategory().toLowerCase()))
                .sorted(Comparator.comparing(Operation::getDate).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public synchronized void exportToCsv(String login, Path targetPath) throws IOException {
        Wallet wallet = getWallet(login);
        csvService.exportOperations(wallet, targetPath);
    }

    @Override
    public synchronized int importFromCsv(String login, Path sourcePath) throws IOException {
        Wallet wallet = getWallet(login);
        List<Operation> imported = csvService.importOperations(sourcePath);
        for (Operation op : imported) {
            wallet.addOperation(op);
        }
        walletRepository.save(wallet);
        return imported.size();
    }
}
