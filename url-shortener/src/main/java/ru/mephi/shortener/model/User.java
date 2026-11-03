package ru.mephi.shortener.model;

import java.util.Objects;
import java.util.UUID;

public record User(UUID id) {
    public User {
        Objects.requireNonNull(id, "User ID cannot be null");
    }

    public static User createNew() {
        return new User(UUID.randomUUID());
    }

    public UUID getId() {
        return id;
    }
}
