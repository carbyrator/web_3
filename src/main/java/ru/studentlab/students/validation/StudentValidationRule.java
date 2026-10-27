package ru.studentlab.students.validation;

import ru.studentlab.students.dto.StudentDto;
import java.util.Map;

// Правило принимает в том числе незаполненные поля, добавляет ошибки в errors,
// не меняет student и не стирает ошибки других правил. Новые правила — Spring-компоненты.
@FunctionalInterface
public interface StudentValidationRule {
    void validate(StudentDto student, Map<String, String> errors);
}
