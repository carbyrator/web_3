package ru.studentlab.students.service.port;

import ru.studentlab.students.dto.RegisterRequest;

public interface Registration {
    void register(RegisterRequest input);
}
