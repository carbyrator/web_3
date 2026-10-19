package ru.studentlab.students.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.studentlab.students.dto.ApiError;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException error, HttpServletRequest request) {
        log.warn("{} {}: {} ({})", request.getMethod(), request.getRequestURI(),
                error.getMessage(), error.getStatus());
        return ResponseEntity.status(error.getStatus())
                .body(new ApiError(error.getMessage(), error.getDetails()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleJson(HttpMessageNotReadableException error) {
        return ResponseEntity.badRequest().body(new ApiError("Ожидался корректный JSON-объект", Map.of()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleType(MethodArgumentTypeMismatchException error) {
        return ResponseEntity.badRequest().body(new ApiError("Некорректный параметр адреса",
                Map.of(error.getName(), "Ожидалось целое число")));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleConflict(DataIntegrityViolationException error) {
        // UNIQUE в БД защищает от одновременного создания одинаковых ИСУ, имён и email.
        log.warn("Конфликт ограничения базы данных");
        return ResponseEntity.status(409).body(new ApiError("Конфликт данных",
                Map.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception error, HttpServletRequest request) {
        // Сюда также попадают стандартные ошибки Spring: 404, 405 и 415.
        if (error instanceof ErrorResponse) {
            ErrorResponse response = (ErrorResponse) error;
            return ResponseEntity.status(response.getStatusCode()).headers(response.getHeaders())
                    .body(new ApiError(response.getBody().getTitle(), Map.of()));
        }
        log.error("Ошибка при {} {}", request.getMethod(), request.getRequestURI(), error);
        return ResponseEntity.internalServerError()
                .body(new ApiError("Внутренняя ошибка сервера", Map.of()));
    }
}
