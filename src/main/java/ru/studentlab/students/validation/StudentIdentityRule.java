package ru.studentlab.students.validation;

import org.springframework.stereotype.Component;
import ru.studentlab.students.dto.StudentDto;
import java.util.Map;

@Component
public class StudentIdentityRule implements StudentValidationRule {
    @Override
    public void validate(StudentDto student, Map<String, String> errors) {
        if (student.name == null || student.name.trim().isEmpty()) {
            errors.putIfAbsent("name", "Обязательное поле");
        } else if (!student.name.matches("[a-zA-Zа-яА-ЯёЁ\\s]+")) {
            errors.putIfAbsent("name", "ФИО должно содержать только буквы");
        } else if (student.name.trim().split("\\s+").length < 2) {
            errors.putIfAbsent("name", "ФИО должно содержать минимум 2 слова");
        }
        if (student.group == null || !student.group.matches("[A-Za-z][1-9][0-9]{3}")) {
            errors.putIfAbsent("group", "Группа: 1 буква и 4 цифры (например, P1234)");
        }
        if (student.isu == null || student.isu < 100000 || student.isu > 999999) {
            errors.putIfAbsent("isu", "ИСУ должен быть от 100000 до 999999");
        }
    }
}
