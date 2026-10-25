package ru.studentlab.students.service.port;

import ru.studentlab.students.dto.TokenResponse;

public interface TokenIssuer {
    TokenResponse issue(String username);
}
