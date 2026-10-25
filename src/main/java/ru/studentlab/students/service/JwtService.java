package ru.studentlab.students.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.studentlab.students.dto.TokenResponse;
import ru.studentlab.students.exception.InvalidTokenException;
import ru.studentlab.students.service.port.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// Создаём и проверяем подписанные токены
@Service
public class JwtService implements TokenIssuer, AccessTokenReader, RefreshTokenReader {
    private final long accessExpiration;
    private final long refreshExpiration;
    private final SecretKey key;

    // Один раз создаём ключ и проверяем сроки токенов при запуске
    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-token-expiration}") long accessExpiration,
                      @Value("${app.jwt.refresh-token-expiration}") long refreshExpiration) {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        if (accessExpiration <= 0 || refreshExpiration <= accessExpiration) {
            throw new IllegalArgumentException("Refresh должен жить дольше access; сроки должны быть положительными");
        }
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    // В настройках срок указан в миллисекундах, а в ответе нужны секунды
    private long getAccessExpirationSeconds() {
        return accessExpiration / 1000;
    }

    // Создаём токен для обращения к защищённым адресам
    private String generateAccessToken(String username) {
        return buildToken(username, accessExpiration, "access");
    }

    // Создаём токен для продления входа без повторного ввода пароля
    private String generateRefreshToken(String username) {
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
    @Override
    public TokenResponse issue(String username) {
        return new TokenResponse(generateAccessToken(username), generateRefreshToken(username),
                getAccessExpirationSeconds());
    }

    @Override
    public String readAccessUsername(String token) {
        return readUsername(token, "access");
    }

    @Override
    public String readRefreshUsername(String token) {
        return readUsername(token, "refresh");
    }

    private String readUsername(String token, String type) {
        try {
            return parseUsername(token, type);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException(e);
        }
    }

    private String parseUsername(String token, String type) {
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
