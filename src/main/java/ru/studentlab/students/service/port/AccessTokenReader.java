package ru.studentlab.students.service.port;

/** Возвращает непустое имя из действующего access-токена, иначе InvalidTokenException. */
public interface AccessTokenReader {
    String readAccessUsername(String token);
}
