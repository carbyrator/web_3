package ru.studentlab.students.service.port;

import ru.studentlab.students.dto.*;

public interface Authentication {
    TokenResponse login(LoginRequest input);
    TokenResponse refresh(RefreshRequest input);
}
