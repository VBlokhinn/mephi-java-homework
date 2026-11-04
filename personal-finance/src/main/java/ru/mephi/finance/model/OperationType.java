package ru.mephi.finance.model;


public enum OperationType {
    INCOME("Доход"),
    EXPENSE("Расход");

    private final String title;

    OperationType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
