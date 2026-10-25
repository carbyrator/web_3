package ru.studentlab.students.service.port;

/** Возвращает непустое имя из действующего refresh-токена, иначе InvalidTokenException. */
public interface RefreshTokenReader {
    String readRefreshUsername(String token);
}
