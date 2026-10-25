package ru.studentlab.students.validation;

import org.springframework.stereotype.Component;
import ru.studentlab.students.dto.StudentDto;
import java.util.Map;

@Component
public class StudentStatusRule implements StudentValidationRule {
    @Override
    public void validate(StudentDto student, Map<String, String> errors) {
        if (student.expDate == null) {
            errors.putIfAbsent("expDate", "Обязательное поле");
        }
        if (student.foreigner == null) {
            errors.putIfAbsent("foreigner", "Укажите статус студента");
        }
    }
}
