package ru.mephi.finance.repository;

import ru.mephi.finance.model.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByLogin(String login);

    void save(User user);

    boolean existsByLogin(String login);

    void clear();
}
