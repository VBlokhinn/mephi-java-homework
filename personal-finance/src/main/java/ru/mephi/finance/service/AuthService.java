package ru.mephi.finance.service;

import ru.mephi.finance.model.User;
import ru.mephi.finance.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Optional;

public class AuthService {
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository cannot be null");
    }

    public User register(String login, String password) {
        if (login == null || login.trim().isEmpty()) {
            throw new IllegalArgumentException("Логин не может быть пустым");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Пароль не может быть пустым");
        }

        String cleanLogin = login.trim().toLowerCase();
        if (userRepository.existsByLogin(cleanLogin)) {
            throw new IllegalArgumentException("Пользователь с логином '" + cleanLogin + "' уже существует");
        }

        String hash = hashPassword(password);
        User user = new User(cleanLogin, hash);
        userRepository.save(user);
        return user;
    }

    public Optional<User> login(String login, String password) {
        if (login == null || password == null) {
            return Optional.empty();
        }

        String cleanLogin = login.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByLogin(cleanLogin);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        String hash = hashPassword(password);
        if (user.getPasswordHash().equals(hash)) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : encoded) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Ошибка алгоритма SHA-256", e);
        }
    }
}
