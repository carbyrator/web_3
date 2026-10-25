package ru.studentlab.students.service.port;

import java.util.Map;

public interface UserProfile {
    Map<String, String> me(String username);
}
