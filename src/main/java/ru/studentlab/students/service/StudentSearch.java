package ru.studentlab.students.service;

import com.fasterxml.jackson.databind.JsonNode;
import ru.studentlab.students.exception.ApiException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

// Параметры списка. Сам запрос к БД находится в StudentRepository.
public class StudentSearch {
    public Map<String, String> filters = new LinkedHashMap<>();
    public String sortField = "id";
    public String sortDirection = "asc";
    public Integer page;
    public int size = 20;

    public StudentSearch(JsonNode input) {
        if (input == null || !input.isObject()) {
            throw new ApiException(400, "Ожидался JSON-объект с фильтрами");
        }
        Iterator<String> fields = input.fieldNames();
        while (fields.hasNext()) {
            String original = fields.next();
            String key = original;
            if (key.equals("dormitory")) {
                key = "dorm";
            }
            boolean option = key.equals("sort")
                    || key.equals("page") || key.equals("size");
            if (!StudentValidator.FIELDS.contains(key) && !option) {
                throw error(original, "Неизвестное свойство студента");
            }

            JsonNode node = input.get(original);
            if (node.isNull() || node.isContainerNode()) {
                throw error(original, "Некорректное значение параметра");
            }
            String value = node.asText();
            if (key.equals("isu") || key.equals("dorm")) {
                if (!value.matches("[0-9]+")) {
                    throw error(original, "Ожидалось целое число");
                }
            } else if (key.equals("foreigner")) {
                value = value.toLowerCase(Locale.ROOT);
                if (!value.equals("true") && !value.equals("false")) {
                    throw error(original, "Ожидалось true или false");
                }
            } else {
                if (!node.isTextual() && !key.equals("page") && !key.equals("size")) {
                    throw error(original, "Ожидалась непустая строка");
                }
                value = value.trim();
                if (value.isEmpty()) {
                    throw error(original, "Ожидалась непустая строка");
                }
            }

            if (key.equals("sort")) {
                readSort(value);
            } else if (key.equals("page")) {
                page = readNumber(key, value, 0, Integer.MAX_VALUE);
            } else if (key.equals("size")) {
                size = readNumber(key, value, 1, 100);
            } else {
                if (filters.containsKey(key) && !filters.get(key).equals(value)) {
                    throw error(original, "Противоречивые значения одного фильтра");
                }
                filters.put(key, value);
            }
        }
        if (page == null && input.has("size")) {
            page = 0;
        }
        if (page != null && (long) page * size > Integer.MAX_VALUE) {
            throw error("page", "Слишком большой номер страницы");
        }
    }

    private void readSort(String value) {
        String[] parts = value.split(",", -1);
        if (parts.length > 2 || !StudentValidator.FIELDS.contains(parts[0])) {
            throw error("sort", "Укажите поле студента, например name,asc");
        }
        sortField = parts[0];
        if (parts.length == 2) {
            sortDirection = parts[1].toLowerCase(Locale.ROOT);
        }
        if (!sortDirection.equals("asc") && !sortDirection.equals("desc")) {
            throw error("sort", "Направление сортировки: asc или desc");
        }
    }

    private int readNumber(String key, String value, int min, int max) {
        try {
            if (!value.matches("[0-9]+")) {
                throw new NumberFormatException();
            }
            int result = Integer.parseInt(value);
            if (result < min || result > max) {
                throw new NumberFormatException();
            }
            return result;
        } catch (NumberFormatException e) {
            throw error(key, "Ожидалось целое число от " + min + " до " + max);
        }
    }

    private ApiException error(String field, String message) {
        return new ApiException(422, "Некорректные фильтры", Map.of(field, message));
    }
}
