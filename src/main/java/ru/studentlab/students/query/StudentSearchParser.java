package ru.studentlab.students.query;

import org.springframework.stereotype.Component;
import ru.studentlab.students.validation.StudentFields;

import com.fasterxml.jackson.databind.JsonNode;
import ru.studentlab.students.exception.ApiException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

// Проверяем фильтры, сортировку и параметры страниц
@Component
public class StudentSearchParser {
    public StudentSearch parse(JsonNode input) {
        return new Parsing(input).result();
    }

    private static class Parsing {
        private final Map<String, String> filters = new LinkedHashMap<>();
        private String sortField = "id";
        private String sortDirection = "asc";
        private Integer page;
        private int size = 20;

        // Разбираем параметры один раз перед обращением к базе
        private Parsing(JsonNode input) {
            if (input == null || !input.isObject()) {
                throw new ApiException(400, "Ожидался JSON-объект с фильтрами");
            }
            Iterator<String> fields = input.fieldNames();
            while (fields.hasNext()) {
                String original = fields.next();
                String key = original;
                // Старое название фильтра приводим к названию поля модели
                if (key.equals("dormitory")) {
                    key = "dorm";
                }
                boolean option = key.equals("sort")
                        || key.equals("page") || key.equals("size");
                if (!StudentFields.ALL.contains(key) && !option) {
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

                // Служебные параметры не добавляем в условия поиска
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
            // Переданный размер без номера страницы включает первую страницу
            if (page == null && input.has("size")) {
                page = 0;
            }
            // Умножаем в long, чтобы заранее проверить переполнение смещения
            if (page != null && (long) page * size > Integer.MAX_VALUE) {
                throw error("page", "Слишком большой номер страницы");
            }
        }

        // Разрешаем только поля студента и направления asc или desc
        private void readSort(String value) {
            String[] parts = value.split(",", -1);
            if (parts.length > 2 || !StudentFields.ALL.contains(parts[0])) {
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

        // Сначала проверяем запись числа, затем преобразование и диапазон
        private int readNumber(String key, String value, int min, int max) {
            if (!value.matches("[0-9]+")) {
                throw error(key, "Ожидалось целое число от " + min + " до " + max);
            }
            int result;
            try {
                result = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw error(key, "Ожидалось целое число от " + min + " до " + max);
            }
            if (result < min || result > max) {
                throw error(key, "Ожидалось целое число от " + min + " до " + max);
            }
            return result;
        }

        private ApiException error(String field, String message) {
            return new ApiException(422, "Некорректные фильтры", Map.of(field, message));
        }
        private StudentSearch result() {
            return new StudentSearch(filters, sortField, sortDirection, page, size);
        }
    }
}
