package ru.studentlab.students.exception;

import java.util.Map;

// Наша ошибка: сообщение, HTTP-статус и пояснения по полям.
public class ApiException extends RuntimeException {
    private final int status;
    private final Map<String, String> details;

    public ApiException(int status, String message) {
        this(status, message, Map.of());
    }

    public ApiException(int status, String message, Map<String, String> details) {
        super(message);
        this.status = status;
        this.details = details;
    }

    public int getStatus() { return status; }
    public Map<String, String> getDetails() { return details; }
}
