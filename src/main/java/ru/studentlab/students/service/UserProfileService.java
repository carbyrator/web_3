package ru.studentlab.students.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studentlab.students.exception.ApiException;
import ru.studentlab.students.model.User;
import ru.studentlab.students.repository.UserReader;
import ru.studentlab.students.service.port.UserProfile;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class UserProfileService implements UserProfile {
    private final UserReader users;

    public UserProfileService(UserReader users) {
        this.users = users;
    }

    @Override
    public Map<String, String> me(String username) {
        User user = users.findByUsername(username);
        if (user == null) {
            throw new ApiException(401, "Пользователь не найден");
        }
        return Map.of("username", user.getUsername(), "role", user.getRole().name());
    }
}
