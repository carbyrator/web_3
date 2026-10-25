package ru.studentlab.students.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studentlab.students.dto.RegisterRequest;
import ru.studentlab.students.exception.ApiException;
import ru.studentlab.students.model.Role;
import ru.studentlab.students.model.User;
import ru.studentlab.students.repository.UserReader;
import ru.studentlab.students.repository.UserWriter;
import ru.studentlab.students.service.port.Registration;
import ru.studentlab.students.service.port.UserProvisioning;
import ru.studentlab.students.validation.RegistrationValidator;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional
public class RegistrationService implements Registration, UserProvisioning {
    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);
    private final UserReader users;
    private final UserWriter writer;
    private final PasswordEncoder passwords;
    private final RegistrationValidator validator;

    public RegistrationService(UserReader users, UserWriter writer, PasswordEncoder passwords,
                               RegistrationValidator validator) {
        this.users = users;
        this.writer = writer;
        this.passwords = passwords;
        this.validator = validator;
    }

    @Override
    public void register(RegisterRequest input) {
        create(input, Role.USER);
    }

    @Override
    public void create(RegisterRequest input, Role role) {
        validator.validate(input);
        String username = input.username.trim();
        String email = input.email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByUsername(username)) {
            throw new ApiException(409, "Имя пользователя уже занято", Map.of("username", "Уже используется"));
        }
        if (users.existsByEmail(email)) {
            throw new ApiException(409, "Email уже занят", Map.of("email", "Уже используется"));
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwords.encode(input.password));
        user.setRole(role);
        writer.saveAndFlush(user);
        log.info("Создан пользователь с ролью {}", role);
    }
}
