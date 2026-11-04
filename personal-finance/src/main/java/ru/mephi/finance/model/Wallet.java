package ru.mephi.finance.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Wallet {
    private final String userLogin;
    private final Map<String, Double> categoryBudgets;
    private final List<Operation> operations;

    public Wallet(String userLogin) {
        this(userLogin, new HashMap<>(), new ArrayList<>());
    }

    @JsonCreator
    public Wallet(@JsonProperty("userLogin") String userLogin,
                  @JsonProperty("categoryBudgets") Map<String, Double> categoryBudgets,
                  @JsonProperty("operations") List<Operation> operations) {
        this.userLogin = Objects.requireNonNull(userLogin, "User login cannot be null").trim().toLowerCase();
        this.categoryBudgets = categoryBudgets != null ? new HashMap<>(categoryBudgets) : new HashMap<>();
        this.operations = operations != null ? new ArrayList<>(operations) : new ArrayList<>();
    }

    public String getUserLogin() {
        return userLogin;
    }

    public synchronized Map<String, Double> getCategoryBudgets() {
        return Collections.unmodifiableMap(categoryBudgets);
    }

    public synchronized List<Operation> getOperations() {
        return Collections.unmodifiableList(operations);
    }

    public synchronized void addOperation(Operation operation) {
        Objects.requireNonNull(operation, "Operation cannot be null");
        operations.add(operation);
    }

    public synchronized void setBudget(String category, double limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Бюджет должен быть больше нуля");
        }
        categoryBudgets.put(category.trim(), limit);
    }

    public synchronized boolean removeBudget(String category) {
        return categoryBudgets.remove(category.trim()) != null;
    }

    public synchronized Double getCategoryBudget(String category) {
        return categoryBudgets.get(category.trim());
    }

    @JsonIgnore
    public synchronized double getBalance() {
        return getTotalIncome() - getTotalExpense();
    }

    @JsonIgnore
    public synchronized double getTotalIncome() {
        return operations.stream()
                .filter(op -> op.getType() == OperationType.INCOME)
                .mapToDouble(Operation::getAmount)
                .sum();
    }

    @JsonIgnore
    public synchronized double getTotalExpense() {
        return operations.stream()
                .filter(op -> op.getType() == OperationType.EXPENSE)
                .mapToDouble(Operation::getAmount)
                .sum();
    }

    public synchronized double getCategoryExpense(String category) {
        return operations.stream()
                .filter(op -> op.getType() == OperationType.EXPENSE && op.getCategory().equalsIgnoreCase(category.trim()))
                .mapToDouble(Operation::getAmount)
                .sum();
    }

    public synchronized double getCategoryIncome(String category) {
        return operations.stream()
                .filter(op -> op.getType() == OperationType.INCOME && op.getCategory().equalsIgnoreCase(category.trim()))
                .mapToDouble(Operation::getAmount)
                .sum();
    }

    @JsonIgnore
    public synchronized Set<String> getAllCategories() {
        Set<String> categories = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        categories.addAll(categoryBudgets.keySet());
        for (Operation op : operations) {
            categories.add(op.getCategory());
        }
        return categories;
    }
}
