package ru.studentlab.students.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import ru.studentlab.students.dto.StudentDto;
import ru.studentlab.students.exception.ApiException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import ru.studentlab.students.service.port.StudentInputReader;
import ru.studentlab.students.validation.StudentFields;
import ru.studentlab.students.validation.StudentValidationRule;

// Читаем поля JSON и проверяем их типы, форматы и диапазоны
@Component
public class StudentValidator implements StudentInputReader {
    private final List<StudentValidationRule> rules;

    public StudentValidator(List<StudentValidationRule> rules) {
        this.rules = List.copyOf(rules);
    }

    // Для создания и PUT получаем пустой объект, для PATCH копию старых данных
    public StudentDto read(JsonNode input, StudentDto student) {
        if (input == null || !input.isObject() || input.isEmpty()) {
            throw new ApiException(400, "Ожидался непустой JSON-объект");
        }
        student = student == null ? new StudentDto() : new StudentDto(student);

        Map<String, String> errors = new LinkedHashMap<>();
        // JsonNode хранит JSON запроса. При PATCH здесь перебираются только присланные поля
        Iterator<String> fields = input.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            JsonNode value = input.get(field);
            if (!StudentFields.ALL.contains(field)) {
                errors.put(field, "Неизвестное свойство студента");
            } else if (value.isNull()) {
                errors.put(field, "Поле не может быть null");
            } else {
                switch (field) {
                    case "name" -> student.name = readText(value, field, errors);
                    case "group" -> student.group = readText(value, field, errors);
                    case "isu" -> student.isu = readNumber(value, field, errors);
                    case "dorm" -> student.dorm = readNumber(value, field, errors);
                    case "room" -> student.room = readText(value, field, errors);
                    case "notes" -> student.notes = readText(value, field, errors);
                    case "expDate" -> student.expDate = readDate(value, field, errors);
                    case "foreigner" -> {
                        if (value.isBoolean()) {
                            student.foreigner = value.booleanValue();
                        } else {
                            errors.put(field, "Ожидалось true или false");
                        }
                    }
                }
            }
        }
        // Сначала проверяем типы полей, затем сами значения
        reject(errors);
        // После проверки типов проверяем значения всего студента
        for (StudentValidationRule rule : rules) {
            rule.validate(student, errors);
        }
        reject(errors);
        return student;
    }

    // Число или логическое значение не заменяют строку
    private String readText(JsonNode value, String field, Map<String, String> errors) {
        if (!value.isTextual()) {
            errors.put(field, "Ожидалась строка");
            return null;
        }
        return value.textValue();
    }

    private LocalDate readDate(JsonNode value, String field, Map<String, String> errors) {
        String date = readText(value, field, errors);
        if (date == null) {
            return null;
        }
        if (!date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            errors.put(field, "Укажите дату в формате ГГГГ-ММ-ДД");
        } else {
            try {
                return LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                errors.put(field, "Такой даты не существует");
            }
        }
        return null;
    }

    // Старый клиент может передавать целое число строкой из цифр
    private Integer readNumber(JsonNode value, String field, Map<String, String> errors) {
        if (value.isIntegralNumber() && value.canConvertToInt()) {
            return value.intValue();
        }
        // В старой лабораторной числа могли приходить строками: "505301"
        if (value.isTextual() && value.textValue().matches("[0-9]+")) {
            try {
                return Integer.parseInt(value.textValue());
            } catch (NumberFormatException e) {
                errors.put(field, "Слишком большое число");
                return null;
            }
        }
        errors.put(field, "Ожидалось целое число");
        return null;
    }

    // Если есть ошибки, прерываем действие и возвращаем их клиенту
    private void reject(Map<String, String> errors) {
        if (!errors.isEmpty()) {
            throw new ApiException(422, "Некорректные данные студента", errors);
        }
    }
}
