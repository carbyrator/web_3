package ru.studentlab.students.exception;

/** Ошибка контракта токенов, не зависящая от библиотеки JWT. */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(Throwable cause) {
        super("Токен недействителен или истёк", cause);
    }
}
