package ru.mephi.finance.repository;

import ru.mephi.finance.model.Wallet;

import java.util.Optional;

public interface WalletRepository {
    Optional<Wallet> findByUserLogin(String login);

    void save(Wallet wallet);

    boolean deleteByUserLogin(String login);

    void clear();
}
