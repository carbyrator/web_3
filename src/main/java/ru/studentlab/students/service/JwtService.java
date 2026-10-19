package ru.studentlab.students.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.PostConstruct;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-token-expiration}")
    private long accessExpiration;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshExpiration;

    @PostConstruct
    public void checkConfig() {
        getSigningKey(); // При слишком коротком ключе приложение сразу сообщит об ошибке.
        if (accessExpiration <= 0 || refreshExpiration <= accessExpiration) {
            throw new IllegalArgumentException("Refresh должен жить дольше access; сроки должны быть положительными");
        }
    }

    public long getAccessExpirationSeconds() {
        return accessExpiration / 1000;
    }

    // Создаем ключ из строки секрета
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // 2. Генерация Access токена
    public String generateAccessToken(String username) {
        return buildToken(username, accessExpiration, "access");
    }

    // 3. Генерация Refresh токена
    public String generateRefreshToken(String username) {
        return buildToken(username, refreshExpiration, "refresh");
    }

    // 4. Общий метод сборки токена (чтобы не дублировать код)
    private String buildToken(String username, long expirationMs, String type) {
        return Jwts.builder()
                .subject(username)
                .claim("type", type)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    // 5. Получение имени пользователя из токена
    public String getUsername(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // 6. Проверка валидности (если парсинг не упал с ошибкой — токен валиден)
    public boolean isValid(String token) {
        try {
            getUsername(token); // Пытаемся прочитать. Если ок — значит валиден
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false; // Истёкший или подделанный токен не принимаем
        }
    }

    public boolean isAccessToken(String token) {
        String type = Jwts.parser().verifyWith(getSigningKey()).build()
                .parseSignedClaims(token).getPayload().get("type", String.class);
        return "access".equals(type);
    }

    // 7. Проверка, что это именно refresh-токен
    public boolean isRefreshToken(String token) {
        String type = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("type", String.class);
        return "refresh".equals(type);
    }
}