package ru.studentlab.students;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import ru.studentlab.students.dto.*;
import ru.studentlab.students.exception.ApiException;
import ru.studentlab.students.exception.InvalidTokenException;
import ru.studentlab.students.model.User;
import ru.studentlab.students.repository.UserReader;
import ru.studentlab.students.service.AuthService;
import ru.studentlab.students.service.JwtService;
import ru.studentlab.students.service.port.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/** Один набор ожиданий для JWT и непрозрачных токенов: проверяем подстановку реализаций. */
class TokenContractTest {
    private static final String SECRET = "solid-test-secret-longer-than-thirty-two-bytes";

    record Backend(TokenIssuer issuer, AccessTokenReader access, RefreshTokenReader refresh) { }

    static Stream<Backend> backends() {
        JwtService jwt = new JwtService(SECRET, 900000, 604800000);
        OpaqueTokens opaque = new OpaqueTokens();
        return Stream.of(new Backend(jwt, jwt, jwt), new Backend(opaque, opaque, opaque));
    }

    @ParameterizedTest
    @MethodSource("backends")
    void tokenReadersHonorTypeAndReportInvalidTokensThroughOneException(Backend backend) {
        TokenResponse issued = backend.issuer.issue("alice");
        assertEquals("alice", backend.access.readAccessUsername(issued.accessToken));
        assertEquals("alice", backend.refresh.readRefreshUsername(issued.refreshToken));
        assertThrows(InvalidTokenException.class,
                () -> backend.access.readAccessUsername(issued.refreshToken));
        assertThrows(InvalidTokenException.class,
                () -> backend.refresh.readRefreshUsername(issued.accessToken));
        for (String invalid : new String[]{null, "", "invalid", issued.accessToken + "changed"}) {
            assertThrows(InvalidTokenException.class, () -> backend.access.readAccessUsername(invalid));
        }
    }

    @ParameterizedTest
    @MethodSource("backends")
    void authenticationNeedsNeitherJpaNorJwtToLoginAndRefresh(Backend backend) {
        BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
        User alice = new User();
        alice.setUsername("alice");
        alice.setPassword(passwords.encode("password123"));
        UserReader users = new UserReader() {
            @Override public User findByUsername(String name) { return "alice".equals(name) ? alice : null; }
            @Override public boolean existsByUsername(String name) { return findByUsername(name) != null; }
            @Override public boolean existsByEmail(String email) { return false; }
        };
        AuthService auth = new AuthService(users, backend.issuer, backend.refresh, passwords);
        LoginRequest login = new LoginRequest();
        login.username = " alice ";
        login.password = "password123";
        TokenResponse issued = auth.login(login);
        RefreshRequest refresh = new RefreshRequest();
        refresh.refreshToken = issued.refreshToken;
        assertEquals("alice", backend.access.readAccessUsername(auth.refresh(refresh).accessToken));
        refresh.refreshToken = issued.accessToken;
        ApiException error = assertThrows(ApiException.class, () -> auth.refresh(refresh));
        assertEquals(401, error.getStatus());
        login.password = "incorrect";
        assertEquals(401, assertThrows(ApiException.class, () -> auth.login(login)).getStatus());
    }

    @Test
    void jwtRejectsExpiredTokensAndTokensSignedWithAnotherKey() {
        JwtService jwt = new JwtService(SECRET, 900000, 604800000);
        var key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String expired = io.jsonwebtoken.Jwts.builder().subject("alice").claim("type", "access")
                .expiration(new java.util.Date(System.currentTimeMillis() - 60000)).signWith(key).compact();
        assertThrows(InvalidTokenException.class, () -> jwt.readAccessUsername(expired));
        JwtService other = new JwtService(SECRET + "-other", 900000, 604800000);
        assertThrows(InvalidTokenException.class, () -> jwt.readAccessUsername(other.issue("alice").accessToken));
    }

    /** Тестовая реализация с той же семантикой чтения и ошибок, без JWT-библиотеки. */
    static class OpaqueTokens implements TokenIssuer, AccessTokenReader, RefreshTokenReader {
        private final Map<String, String> access = new HashMap<>();
        private final Map<String, String> refresh = new HashMap<>();

        @Override public TokenResponse issue(String username) {
            String accessToken = UUID.randomUUID().toString();
            String refreshToken = UUID.randomUUID().toString();
            access.put(accessToken, username);
            refresh.put(refreshToken, username);
            return new TokenResponse(accessToken, refreshToken, 900);
        }

        @Override public String readAccessUsername(String token) { return read(access, token); }
        @Override public String readRefreshUsername(String token) { return read(refresh, token); }

        private String read(Map<String, String> tokens, String token) {
            String username = tokens.get(token);
            if (username == null) {
                throw new InvalidTokenException(new IllegalArgumentException("Неизвестный токен"));
            }
            return username;
        }
    }
}
