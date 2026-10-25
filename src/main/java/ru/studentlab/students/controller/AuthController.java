package ru.studentlab.students.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.studentlab.students.dto.*;
import ru.studentlab.students.service.port.Authentication;
import ru.studentlab.students.service.port.Registration;
import ru.studentlab.students.service.port.UserProfile;
import java.security.Principal;
import java.util.Map;

// Принимаем запросы регистрации, входа и обновления токенов
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Авторизация")
public class AuthController {
    private final Authentication auth;
    private final Registration registration;
    private final UserProfile profile;

    public AuthController(Authentication auth, Registration registration, UserProfile profile) {
        this.auth = auth;
        this.registration = registration;
        this.profile = profile;
    }

    // Регистрация всегда создаёт обычного пользователя
    @PostMapping("/register")
    @Operation(summary = "Регистрация пользователя с ролью USER")
    public ResponseEntity<Void> register(@RequestBody RegisterRequest input) {
        registration.register(input);
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
        return profile.me(principal.getName());
    }
}
