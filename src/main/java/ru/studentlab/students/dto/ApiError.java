package ru.studentlab.students.dto;

import java.util.Map;

public class ApiError {
    public ErrorBody error;

    public ApiError(String message, Map<String, String> details) {
        error = new ErrorBody(message, details);
    }

    public static class ErrorBody {
        public String message;
        public Map<String, String> details;

        public ErrorBody(String message, Map<String, String> details) {
            this.message = message;
            this.details = details;
        }
    }
}
