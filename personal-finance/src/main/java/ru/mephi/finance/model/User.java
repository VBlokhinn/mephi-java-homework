package ru.mephi.finance.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class User {
    private final String login;
    private final String passwordHash;

    @JsonCreator
    public User(@JsonProperty("login") String login,
                @JsonProperty("passwordHash") String passwordHash) {
        this.login = Objects.requireNonNull(login, "Login cannot be null").trim().toLowerCase();
        this.passwordHash = Objects.requireNonNull(passwordHash, "Password hash cannot be null");
    }

    public String getLogin() {
        return login;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(login, user.login);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(login);
    }
}
