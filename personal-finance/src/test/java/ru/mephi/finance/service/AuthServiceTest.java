package ru.mephi.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mephi.finance.model.User;
import ru.mephi.finance.repository.FileUserRepository;
import ru.mephi.finance.repository.UserRepository;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {
    private UserRepository userRepository;
    private AuthService authService;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        userRepository = new FileUserRepository(tempDir.resolve("users.json"));
        authService = new AuthService(userRepository);
    }

    @Test
    @DisplayName("Успешная регистрация нового пользователя")
    void testRegisterSuccess() {
        User user = authService.register("alex", "pass123");
        assertNotNull(user);
        assertEquals("alex", user.getLogin());
        assertTrue(userRepository.existsByLogin("alex"));
    }

    @Test
    @DisplayName("Попытка регистрации дубликата логина вызывает исключение")
    void testRegisterDuplicateLoginThrowsException() {
        authService.register("alex", "pass123");
        assertThrows(IllegalArgumentException.class, () -> authService.register("alex", "otherPass"));
    }

    @Test
    @DisplayName("Успешная авторизация с правильным паролем")
    void testLoginSuccessWithCorrectPassword() {
        authService.register("bob", "secret123");
        Optional<User> loggedIn = authService.login("bob", "secret123");
        assertTrue(loggedIn.isPresent());
        assertEquals("bob", loggedIn.get().getLogin());
    }

    @Test
    @DisplayName("Отказ в авторизации при неверном пароле или неизвестном логине")
    void testLoginFailureWithWrongPassword() {
        authService.register("bob", "secret123");
        assertTrue(authService.login("bob", "wrongPass").isEmpty());
        assertTrue(authService.login("unknown", "pass").isEmpty());
    }
}
