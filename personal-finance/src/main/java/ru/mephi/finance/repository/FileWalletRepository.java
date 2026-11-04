package ru.mephi.finance.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ru.mephi.finance.model.Wallet;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileWalletRepository implements WalletRepository {
    private final Path baseDirectory;
    private final ObjectMapper objectMapper;
    private final Map<String, Wallet> walletCache = new ConcurrentHashMap<>();

    public FileWalletRepository(Path baseDirectory) {
        this.baseDirectory = Objects.requireNonNull(baseDirectory, "Base directory cannot be null");
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        init();
    }

    private void init() {
        try {
            if (!Files.exists(baseDirectory)) {
                Files.createDirectories(baseDirectory);
            }
        } catch (IOException e) {
            System.err.printf("Не удалось создать директорию для кошельков: %s%n", e.getMessage());
        }
    }

    private Path resolveWalletFile(String login) {
        String safeLogin = login.trim().toLowerCase().replaceAll("[^a-zA-Z0-9_.-]", "_");
        return baseDirectory.resolve("wallet_" + safeLogin + ".json");
    }

    @Override
    public synchronized Optional<Wallet> findByUserLogin(String login) {
        if (login == null) {
            return Optional.empty();
        }
        String key = login.trim().toLowerCase();
        if (walletCache.containsKey(key)) {
            return Optional.of(walletCache.get(key));
        }

        Path path = resolveWalletFile(key);
        if (!Files.exists(path)) {
            return Optional.empty();
        }

        try {
            Wallet wallet = objectMapper.readValue(path.toFile(), Wallet.class);
            walletCache.put(key, wallet);
            return Optional.of(wallet);
        } catch (IOException e) {
            System.err.printf("Ошибка чтения кошелька для %s: %s%n", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public synchronized void save(Wallet wallet) {
        Objects.requireNonNull(wallet, "Wallet cannot be null");
        String key = wallet.getUserLogin();
        walletCache.put(key, wallet);

        Path path = resolveWalletFile(key);
        try {
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), wallet);
        } catch (IOException e) {
            System.err.printf("Ошибка сохранения кошелька %s: %s%n", key, e.getMessage());
        }
    }

    @Override
    public synchronized boolean deleteByUserLogin(String login) {
        if (login == null) {
            return false;
        }
        String key = login.trim().toLowerCase();
        walletCache.remove(key);
        Path path = resolveWalletFile(key);
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public synchronized void clear() {
        walletCache.clear();
        if (!Files.exists(baseDirectory)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDirectory, "wallet_*.json")) {
            for (Path p : stream) {
                Files.deleteIfExists(p);
            }
        } catch (IOException ignored) {
        }
    }
}
