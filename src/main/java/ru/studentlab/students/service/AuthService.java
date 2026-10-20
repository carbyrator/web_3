package ru.studentlab.students.service;

import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studentlab.students.dto.*;
import ru.studentlab.students.exception.ApiException;
import ru.studentlab.students.model.Role;
import ru.studentlab.students.model.User;
import ru.studentlab.students.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

// Регистрируем пользователей, проверяем пароли и выдаём токены
@Service
@Transactional(readOnly = true)
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository users;
    private final JwtService jwt;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();

    public AuthService(UserRepository users, JwtService jwt) {
        this.users = users;
        this.jwt = jwt;
    }

    @Transactional
    public void register(RegisterRequest input) {
        // Публичная регистрация всегда создаёт USER
        createUser(input, Role.USER);
    }

    @Transactional
    public void createAdmin(RegisterRequest input) {
        createUser(input, Role.ADMIN);
    }

    // Проверяем данные до записи пользователя в базу
    private void createUser(RegisterRequest input, Role role) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (input.username == null || input.username.trim().length() < 3
                || input.username.trim().length() > 40) {
            errors.put("username", "Имя пользователя: от 3 до 40 символов");
        }
        if (input.email == null || input.email.length() > 254
                || !input.email.trim().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            errors.put("email", "Некорректный email");
        }
        if (input.password == null || input.password.isBlank() || input.password.length() < 6
                || input.password.length() > 40
                || input.password.getBytes(StandardCharsets.UTF_8).length > 72) {
            errors.put("password", "Пароль: от 6 до 40 символов и не более 72 байт UTF-8");
        }
        if (!errors.isEmpty()) {
            throw new ApiException(422, "Некорректные данные пользователя", errors);
        }
        // Убираем крайние пробелы и приводим почту к нижнему регистру
        String username = input.username.trim();
        String email = input.email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByUsername(username)) {
            throw new ApiException(409, "Имя пользователя уже занято", Map.of("username", "Уже используется"));
        }
        if (users.existsByEmail(email)) {
            throw new ApiException(409, "Email уже занят", Map.of("email", "Уже используется"));
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        // BCrypt сохраняет хеш с солью вместо исходного пароля
        user.setPassword(passwords.encode(input.password));
        user.setRole(role);
        users.saveAndFlush(user);
        log.info("Создан пользователь с ролью {}", role);
    }

    // Ищем пользователя по имени и сравниваем пароль с хешем
    public TokenResponse login(LoginRequest input) {
        if (input.username == null || input.username.isBlank()
                || input.password == null || input.password.isBlank()) {
            throw new ApiException(422, "Укажите имя пользователя и пароль");
        }
        User user = users.findByUsername(input.username.trim());
        if (user == null || !passwords.matches(input.password, user.getPassword())) {
            log.warn("Неудачная попытка входа");
            throw new ApiException(401, "Неверное имя пользователя или пароль");
        }
        log.info("Выполнен вход пользователя");
        return tokens(user.getUsername());
    }

    // Для обновления принимаем только токен типа refresh
    public TokenResponse refresh(RefreshRequest input) {
        String token = input.refreshToken;
        if (token == null || token.isBlank()) {
            throw new ApiException(401, "Укажите refresh-токен");
        }
        try {
            String username = jwt.readUsername(token, "refresh");
            if (users.findByUsername(username) == null) {
                throw new ApiException(401, "Пользователь не найден");
            }
            return tokens(username);
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(401, "Refresh-токен недействителен или истёк");
        }
    }

    // Выдаём оба токена для одного пользователя
    private TokenResponse tokens(String username) {
        return new TokenResponse(jwt.generateAccessToken(username),
                jwt.generateRefreshToken(username), jwt.getAccessExpirationSeconds());
    }
}
