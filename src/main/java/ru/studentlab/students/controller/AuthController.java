package ru.studentlab.students.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.studentlab.students.dto.*;
import ru.studentlab.students.model.User;
import ru.studentlab.students.repository.UserRepository;
import ru.studentlab.students.service.AuthService;
import java.security.Principal;
import java.util.Map;

// Принимаем запросы регистрации, входа и обновления токенов
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Авторизация")
public class AuthController {
    private final AuthService auth;
    private final UserRepository users;

    public AuthController(AuthService auth, UserRepository users) {
        this.auth = auth;
        this.users = users;
    }

    // Регистрация всегда создаёт обычного пользователя
    @PostMapping("/register")
    @Operation(summary = "Регистрация пользователя с ролью USER")
    public ResponseEntity<Void> register(@RequestBody RegisterRequest input) {
        auth.register(input);
        return ResponseEntity.status(201).build();
    }

    // При верном пароле возвращаем пару токенов
    @PostMapping("/login")
    @Operation(summary = "Вход: access- и refresh-токены")
    public TokenResponse login(@RequestBody LoginRequest input) {
        return auth.login(input);
    }

    // Продлеваем вход по токену обновления
    @PostMapping("/refresh")
    @Operation(summary = "Получить новые токены по refresh-токену")
    public TokenResponse refresh(@RequestBody RefreshRequest input) {
        return auth.refresh(input);
    }

    // Берём имя из проверенного токена, а роль читаем из базы
    @GetMapping("/me")
    @Operation(summary = "Имя и роль текущего пользователя")
    @SecurityRequirement(name = "bearerAuth")
    public Map<String, String> me(Principal principal) {
        User user = users.findByUsername(principal.getName());
        return Map.of("username", user.getUsername(), "role", user.getRole().name());
    }
}
