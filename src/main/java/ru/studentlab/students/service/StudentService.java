package ru.studentlab.students.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studentlab.students.dto.StudentDto;
import ru.studentlab.students.exception.ApiException;
import ru.studentlab.students.model.Student;
import ru.studentlab.students.repository.StudentReader;
import ru.studentlab.students.repository.StudentWriter;
import ru.studentlab.students.service.port.*;
import ru.studentlab.students.query.StudentSearch;
import ru.studentlab.students.dto.StudentSearchResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Здесь задаём порядок проверок и действий над студентами
@Service
@Transactional(readOnly = true)
public class StudentService implements StudentQueries, StudentCommands {
    private static final Logger log = LoggerFactory.getLogger(StudentService.class);
    private final StudentReader reader;
    private final StudentWriter writer;
    private final StudentInputReader validator;
    private final StudentMapper mapper;

    public StudentService(StudentReader reader, StudentWriter writer,
                          StudentInputReader validator, StudentMapper mapper) {
        this.reader = reader;
        this.writer = writer;
        this.validator = validator;
        this.mapper = mapper;
    }

    // Получаем сущности из базы и превращаем их в данные ответа
    public StudentSearchResult findAll(StudentSearch search) {
        List<Student> students = reader.findAll(search);
        List<StudentDto> result = new ArrayList<>();
        for (Student student : students) {
            result.add(mapper.toDto(student));
        }
        long total = result.size();
        if (search.page() != null) {
            // При страницах нужно общее число совпадений, а не длина страницы
            total = reader.count(search);
        }
        return new StudentSearchResult(result, total, search.page(), search.size());
    }

    // Внешний API работает с ИСУ, а не с внутренним ключом базы
    public StudentDto findByIsu(Integer isu) {
        Student student = findEntity(isu);
        return mapper.toDto(student);
    }

    // При нашей ошибке транзакция отменит изменения в базе
    @Transactional
    public StudentDto create(JsonNode input) {
        StudentDto data = validator.read(input, null);
        if (reader.findByIsu(data.isu) != null) {
            duplicateIsu();
        }
        Student student = new Student();
        mapper.copyToEntity(data, student);
        student = writer.saveAndFlush(student);
        log.info("Создан студент с ИСУ {}", student.getIsu());
        return mapper.toDto(student);
    }

    @Transactional
    // Флаг partial отличает PATCH от полной замены PUT
    public StudentDto update(Integer isu, JsonNode input, boolean partial) {
        Student student = findEntity(isu);
        StudentDto oldData = null;
        // Для PATCH сначала копируем старые значения в отдельный объект
        if (partial) {
            oldData = mapper.toDto(student);
        }
        StudentDto data = validator.read(input, oldData);
        // Новый ИСУ может совпадать со старым, но не с ИСУ другого студента
        Student other = reader.findByIsu(data.isu);
        if (other != null && !other.getId().equals(student.getId())) {
            duplicateIsu();
        }
        mapper.copyToEntity(data, student);
        student = writer.saveAndFlush(student);
        log.info("Обновлён студент: ИСУ {} -> {}", isu, student.getIsu());
        return mapper.toDto(student);
    }

    @Transactional
    // Сначала ищем запись, чтобы вернуть 404 для отсутствующего студента
    public void delete(Integer isu) {
        Student student = findEntity(isu);
        writer.delete(student);
        log.info("Удалён студент с ИСУ {}", isu);
    }

    // Общий поиск для чтения, изменения и удаления
    private Student findEntity(Integer isu) {
        Student student = reader.findByIsu(isu);
        if (student == null) {
            throw new ApiException(404, "Студент не найден");
        }
        return student;
    }

    // При занятом ИСУ возвращаем конфликт 409 и пояснение поля
    private void duplicateIsu() {
        throw new ApiException(409, "Студент с таким ИСУ уже существует",
                Map.of("isu", "Студент с таким ИСУ уже существует"));
    }

}
