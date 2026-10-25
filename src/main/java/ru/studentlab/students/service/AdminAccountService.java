package ru.studentlab.students.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studentlab.students.dto.RegisterRequest;
import ru.studentlab.students.model.Role;
import ru.studentlab.students.model.User;
import ru.studentlab.students.repository.UserReader;
import ru.studentlab.students.repository.UserWriter;
import ru.studentlab.students.service.port.AdministratorAccounts;
import ru.studentlab.students.service.port.UserProvisioning;
import ru.studentlab.students.validation.RegistrationValidator;

@Service
@Transactional
public class AdminAccountService implements AdministratorAccounts {
    private final UserReader users;
    private final UserWriter writer;
    private final UserProvisioning provisioning;
    private final PasswordEncoder passwords;
    private final RegistrationValidator validator;

    public AdminAccountService(UserReader users, UserWriter writer, UserProvisioning provisioning,
                               PasswordEncoder passwords, RegistrationValidator validator) {
        this.users = users;
        this.writer = writer;
        this.provisioning = provisioning;
        this.passwords = passwords;
        this.validator = validator;
    }

    @Override
    public void initialize(String username, String email, String password) {
        if (username.isBlank() && email.isBlank() && password.isBlank()) {
            return;
        }
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("Для администратора нужны ADMIN_USERNAME, ADMIN_EMAIL и ADMIN_PASSWORD");
        }
        if (!validator.validPassword(password)) {
            throw new IllegalArgumentException("Пароль администратора: от 6 до 40 символов и не более 72 байт UTF-8");
        }
        User existing = users.findByUsername(username.trim());
        if (existing != null) {
            if (existing.getRole() != Role.ADMIN) {
                throw new IllegalArgumentException("Имя администратора занято обычным пользователем");
            }
            if (!passwords.matches(password, existing.getPassword())) {
                existing.setPassword(passwords.encode(password));
                writer.saveAndFlush(existing);
            }
            return;
        }
        RegisterRequest input = new RegisterRequest();
        input.username = username;
        input.email = email;
        input.password = password;
        provisioning.create(input, Role.ADMIN);
    }
}
