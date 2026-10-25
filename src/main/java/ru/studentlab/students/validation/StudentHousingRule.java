package ru.studentlab.students.validation;

import org.springframework.stereotype.Component;
import ru.studentlab.students.dto.StudentDto;
import java.util.Map;

@Component
public class StudentHousingRule implements StudentValidationRule {
    @Override
    public void validate(StudentDto student, Map<String, String> errors) {
        if (student.dorm == null || student.dorm < 1 || student.dorm > 100) {
            errors.putIfAbsent("dorm", "Номер общежития: от 1 до 100");
        }
        if (student.room == null || !student.room.matches("[0-9]{2,4}[A-Za-zА-Яа-яЁё]?")) {
            errors.putIfAbsent("room", "Комната: 2-4 цифры и необязательная буква");
        }
    }
}
