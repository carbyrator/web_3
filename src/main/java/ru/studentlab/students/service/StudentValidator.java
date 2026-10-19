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
import java.util.Set;

// Spring сам создаст объект этого класса и передаст его в сервис.
@Component
public class StudentValidator {
    public static final Set<String> FIELDS = Set.of(
            "name", "group", "isu", "dorm", "room", "expDate", "foreigner", "notes");

    public StudentDto read(JsonNode input, StudentDto student) {
        if (input == null || !input.isObject() || input.isEmpty()) {
            throw new ApiException(400, "Ожидался непустой JSON-объект");
        }
        if (student == null) {
            student = new StudentDto();
        }

        Map<String, String> errors = new LinkedHashMap<>();
        // JsonNode хранит JSON запроса. При PATCH здесь перебираются только присланные поля.
        Iterator<String> fields = input.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            JsonNode value = input.get(field);
            if (!FIELDS.contains(field)) {
                errors.put(field, "Неизвестное свойство студента");
            } else if (value.isNull()) {
                errors.put(field, "Поле не может быть null");
            } else if (field.equals("name")) {
                student.name = readText(value, field, errors);
            } else if (field.equals("group")) {
                student.group = readText(value, field, errors);
            } else if (field.equals("isu")) {
                student.isu = readNumber(value, field, errors);
            } else if (field.equals("dorm")) {
                student.dorm = readNumber(value, field, errors);
            } else if (field.equals("room")) {
                student.room = readText(value, field, errors);
            } else if (field.equals("notes")) {
                student.notes = readText(value, field, errors);
            } else if (field.equals("foreigner")) {
                if (value.isBoolean()) {
                    student.foreigner = value.booleanValue();
                } else {
                    errors.put(field, "Ожидалось true или false");
                }
            } else if (field.equals("expDate")) {
                String date = readText(value, field, errors);
                if (date != null) {
                    if (!date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
                        errors.put(field, "Укажите дату в формате ГГГГ-ММ-ДД");
                    } else {
                        try {
                            student.expDate = LocalDate.parse(date);
                        } catch (DateTimeParseException e) {
                            errors.put(field, "Такой даты не существует");
                        }
                    }
                }
            }
        }
        // Сначала проверяем типы полей, затем сами значения.
        reject(errors);
        checkValues(student, errors);
        reject(errors);
        return student;
    }

    private void checkValues(StudentDto student, Map<String, String> errors) {
        if (student.name == null || student.name.trim().isEmpty()) {
            errors.put("name", "Обязательное поле");
        } else if (!student.name.matches("[a-zA-Zа-яА-ЯёЁ\\s]+")) {
            errors.put("name", "ФИО должно содержать только буквы");
        } else if (student.name.trim().split("\\s+").length < 2) {
            errors.put("name", "ФИО должно содержать минимум 2 слова");
        }
        if (student.group == null || !student.group.matches("[A-Za-z][1-9][0-9]{3}")) {
            errors.put("group", "Группа: 1 буква и 4 цифры (например, P1234)");
        }
        if (student.isu == null || student.isu < 100000 || student.isu > 999999) {
            errors.put("isu", "ИСУ должен быть от 100000 до 999999");
        }
        if (student.dorm == null || student.dorm < 1 || student.dorm > 100) {
            errors.put("dorm", "Номер общежития: от 1 до 100");
        }
        if (student.room == null || !student.room.matches("[0-9]{2,4}[A-Za-zА-Яа-яЁё]?")) {
            errors.put("room", "Комната: 2-4 цифры и необязательная буква");
        }
        if (student.expDate == null) {
            errors.put("expDate", "Обязательное поле");
        }
        if (student.foreigner == null) {
            errors.put("foreigner", "Укажите статус студента");
        }
    }

    private String readText(JsonNode value, String field, Map<String, String> errors) {
        if (!value.isTextual()) {
            errors.put(field, "Ожидалась строка");
            return null;
        }
        return value.textValue();
    }

    private Integer readNumber(JsonNode value, String field, Map<String, String> errors) {
        if (value.isIntegralNumber() && value.canConvertToInt()) {
            return value.intValue();
        }
        // В старой лабораторной числа могли приходить строками: "505301".
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

    private void reject(Map<String, String> errors) {
        if (!errors.isEmpty()) {
            throw new ApiException(422, "Некорректные данные студента", errors);
        }
    }
}
