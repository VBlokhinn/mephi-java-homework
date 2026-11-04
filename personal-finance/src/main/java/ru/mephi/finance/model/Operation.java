package ru.mephi.finance.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class Operation {
    private final String id;
    private final OperationType type;
    private final double amount;
    private final String category;
    private final LocalDate date;
    private final String description;

    public Operation(OperationType type, double amount, String category, LocalDate date, String description) {
        this(UUID.randomUUID().toString().substring(0, 8), type, amount, category, date, description);
    }

    @JsonCreator
    public Operation(@JsonProperty("id") String id,
                     @JsonProperty("type") OperationType type,
                     @JsonProperty("amount") double amount,
                     @JsonProperty("category") String category,
                     @JsonProperty("date") LocalDate date,
                     @JsonProperty("description") String description) {
        this.id = id != null ? id : UUID.randomUUID().toString().substring(0, 8);
        this.type = Objects.requireNonNull(type, "Operation type cannot be null");
        if (amount <= 0) {
            throw new IllegalArgumentException("Operation amount must be greater than 0");
        }
        this.amount = amount;
        this.category = Objects.requireNonNull(category, "Category cannot be null").trim();
        this.date = date != null ? date : LocalDate.now();
        this.description = description != null ? description.trim() : "";
    }

    public String getId() {
        return id;
    }

    public OperationType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Operation that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Operation{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", amount=" + amount +
                ", category='" + category + '\'' +
                ", date=" + date +
                ", description='" + description + '\'' +
                '}';
    }
}
