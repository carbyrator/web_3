package ru.studentlab.students.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.PostConstruct;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// Создаём и проверяем подписанные токены
@Service
public class JwtService {
    // Настройки берутся из .env через application.properties
    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-token-expiration}")
    private long accessExpiration;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshExpiration;

    private SecretKey key;

    // Один раз создаём ключ и проверяем сроки токенов при запуске
    @PostConstruct
    public void checkConfig() {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        if (accessExpiration <= 0 || refreshExpiration <= accessExpiration) {
            throw new IllegalArgumentException("Refresh должен жить дольше access; сроки должны быть положительными");
        }
    }

    // В настройках срок указан в миллисекундах, а в ответе нужны секунды
    public long getAccessExpirationSeconds() {
        return accessExpiration / 1000;
    }

    // Создаём токен для обращения к защищённым адресам
    public String generateAccessToken(String username) {
        return buildToken(username, accessExpiration, "access");
    }

    // Создаём токен для продления входа без повторного ввода пароля
    public String generateRefreshToken(String username) {
        return buildToken(username, refreshExpiration, "refresh");
    }

    // В обоих токенах сохраняем имя пользователя, тип и срок действия
    private String buildToken(String username, long expirationMs, String type) {
        return Jwts.builder()
                .subject(username)
                .claim("type", type)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    // Проверяем подпись, срок и нужный тип токена, затем читаем имя
    public String readUsername(String token, String type) {
        Claims data = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!type.equals(data.get("type", String.class))) {
            throw new JwtException("Неверный тип токена");
        }
        String username = data.getSubject();
        if (username == null || username.isBlank()) {
            throw new JwtException("В токене нет имени пользователя");
        }
        return username;
    }
}
