package ru.studentlab.students.validation;

import org.springframework.stereotype.Component;
import ru.studentlab.students.dto.RegisterRequest;
import ru.studentlab.students.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RegistrationValidator {
    public void validate(RegisterRequest input) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (input.username == null || input.username.trim().length() < 3
                || input.username.trim().length() > 40) {
            errors.put("username", "Имя пользователя: от 3 до 40 символов");
        }
        if (input.email == null || input.email.length() > 254
                || !input.email.trim().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            errors.put("email", "Некорректный email");
        }
        if (!validPassword(input.password)) {
            errors.put("password", "Пароль: от 6 до 40 символов и не более 72 байт UTF-8");
        }
        if (!errors.isEmpty()) {
            throw new ApiException(422, "Некорректные данные пользователя", errors);
        }
    }

    public boolean validPassword(String password) {
        return password != null && !password.isBlank() && password.length() >= 6
                && password.length() <= 40 && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
