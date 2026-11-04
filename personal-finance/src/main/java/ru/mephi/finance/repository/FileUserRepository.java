package ru.mephi.finance.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.mephi.finance.model.User;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileUserRepository implements UserRepository {
    private final Path storagePath;
    private final ObjectMapper objectMapper;
    private final Map<String, User> userCache = new ConcurrentHashMap<>();

    public FileUserRepository(Path storagePath) {
        this.storagePath = Objects.requireNonNull(storagePath, "Storage path cannot be null");
        this.objectMapper = new ObjectMapper();
        load();
    }

    private synchronized void load() {
        if (!Files.exists(storagePath)) {
            return;
        }
        try {
            List<User> list = objectMapper.readValue(storagePath.toFile(), new TypeReference<List<User>>() {});
            userCache.clear();
            for (User u : list) {
                userCache.put(u.getLogin().toLowerCase(), u);
            }
        } catch (IOException e) {
            System.err.printf("Не удалось прочитать файл пользователей: %s%n", e.getMessage());
        }
    }

    private synchronized void persist() {
        try {
            if (storagePath.getParent() != null && !Files.exists(storagePath.getParent())) {
                Files.createDirectories(storagePath.getParent());
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storagePath.toFile(), new ArrayList<>(userCache.values()));
        } catch (IOException e) {
            System.err.printf("Не удалось сохранить файл пользователей: %s%n", e.getMessage());
        }
    }

    @Override
    public Optional<User> findByLogin(String login) {
        if (login == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(userCache.get(login.trim().toLowerCase()));
    }

    @Override
    public synchronized void save(User user) {
        Objects.requireNonNull(user, "User cannot be null");
        userCache.put(user.getLogin().toLowerCase(), user);
        persist();
    }

    @Override
    public boolean existsByLogin(String login) {
        if (login == null) {
            return false;
        }
        return userCache.containsKey(login.trim().toLowerCase());
    }

    @Override
    public synchronized void clear() {
        userCache.clear();
        try {
            Files.deleteIfExists(storagePath);
        } catch (IOException ignored) {
        }
    }
}
