package ru.studentlab.students.service.port;

import ru.studentlab.students.dto.RegisterRequest;
import ru.studentlab.students.model.Role;

/** Внутренний контракт создания учётных записей с заданной сервером ролью. */
public interface UserProvisioning {
    void create(RegisterRequest input, Role role);
}
