package ru.studentlab.students.service;

import ru.studentlab.students.exception.InvalidTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studentlab.students.dto.*;
import ru.studentlab.students.exception.ApiException;
import ru.studentlab.students.model.User;
import ru.studentlab.students.repository.UserReader;
import ru.studentlab.students.service.port.*;

// Проверяем учtтные данные и обновляем вход через абстракции токенов
@Service
@Transactional(readOnly = true)
public class AuthService implements Authentication {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserReader users;
    private final TokenIssuer tokens;
    private final RefreshTokenReader refreshTokens;
    private final PasswordEncoder passwords;

    public AuthService(UserReader users, TokenIssuer tokens,
                       RefreshTokenReader refreshTokens, PasswordEncoder passwords) {
        this.users = users;
        this.tokens = tokens;
        this.refreshTokens = refreshTokens;
        this.passwords = passwords;
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
        return tokens.issue(user.getUsername());
    }

    // Для обновления принимаем только токен типа refresh
    public TokenResponse refresh(RefreshRequest input) {
        String token = input.refreshToken;
        if (token == null || token.isBlank()) {
            throw new ApiException(401, "Укажите refresh-токен");
        }
        try {
            String username = refreshTokens.readRefreshUsername(token);
            if (users.findByUsername(username) == null) {
                throw new ApiException(401, "Пользователь не найден");
            }
            return tokens.issue(username);
        } catch (InvalidTokenException e) {
            throw new ApiException(401, "Refresh-токен недействителен или истёк");
        }
    }

}
