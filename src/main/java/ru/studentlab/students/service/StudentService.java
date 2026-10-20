package ru.studentlab.students.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studentlab.students.dto.StudentDto;
import ru.studentlab.students.exception.ApiException;
import ru.studentlab.students.model.Student;
import ru.studentlab.students.repository.StudentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Здесь задаём порядок проверок и действий над студентами
@Service
@Transactional(readOnly = true)
public class StudentService {
    private static final Logger log = LoggerFactory.getLogger(StudentService.class);
    private final StudentRepository repository;
    private final StudentValidator validator;

    // Spring передаёт готовые repository и validator в конструктор
    public StudentService(StudentRepository repository, StudentValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    // Получаем сущности из базы и превращаем их в данные ответа
    public SearchResult findAll(StudentSearch search) {
        List<Student> students = repository.findAll(search);
        List<StudentDto> result = new ArrayList<>();
        for (Student student : students) {
            result.add(new StudentDto(student));
        }
        long total = result.size();
        if (search.page != null) {
            // При страницах нужно общее число совпадений, а не длина страницы
            total = repository.count(search);
        }
        return new SearchResult(result, total, search.page, search.size);
    }

    // Внешний API работает с ИСУ, а не с внутренним ключом базы
    public StudentDto findByIsu(Integer isu) {
        Student student = findEntity(isu);
        return new StudentDto(student);
    }

    // При нашей ошибке транзакция отменит изменения в базе
    @Transactional
    public StudentDto create(JsonNode input) {
        StudentDto data = validator.read(input, null);
        if (repository.findByIsu(data.isu) != null) {
            duplicateIsu();
        }
        Student student = new Student();
        copyFields(data, student);
        repository.saveAndFlush(student);
        log.info("Создан студент с ИСУ {}", student.getIsu());
        return new StudentDto(student);
    }

    @Transactional
    // Флаг partial отличает PATCH от полной замены PUT
    public StudentDto update(Integer isu, JsonNode input, boolean partial) {
        Student student = findEntity(isu);
        StudentDto oldData = null;
        // Для PATCH сначала копируем старые значения в отдельный объект
        if (partial) {
            oldData = new StudentDto(student);
        }
        StudentDto data = validator.read(input, oldData);
        // Новый ИСУ может совпадать со старым, но не с ИСУ другого студента
        Student other = repository.findByIsu(data.isu);
        if (other != null && !other.getId().equals(student.getId())) {
            duplicateIsu();
        }
        copyFields(data, student);
        repository.saveAndFlush(student);
        log.info("Обновлён студент: ИСУ {} -> {}", isu, student.getIsu());
        return new StudentDto(student);
    }

    @Transactional
    // Сначала ищем запись, чтобы вернуть 404 для отсутствующего студента
    public void delete(Integer isu) {
        Student student = findEntity(isu);
        repository.delete(student);
        log.info("Удалён студент с ИСУ {}", isu);
    }

    // Общий поиск для чтения, изменения и удаления
    private Student findEntity(Integer isu) {
        Student student = repository.findByIsu(isu);
        if (student == null) {
            throw new ApiException(404, "Студент не найден");
        }
        return student;
    }

    // В сущность записываем только уже проверенные значения
    private void copyFields(StudentDto data, Student student) {
        student.setName(data.name);
        student.setGroup(data.group);
        student.setIsu(data.isu);
        student.setDorm(data.dorm);
        student.setRoom(data.room);
        student.setExpDate(data.expDate);
        student.setForeigner(data.foreigner);
        student.setNotes(data.notes);
    }

    // При занятом ИСУ возвращаем конфликт 409 и пояснение поля
    private void duplicateIsu() {
        throw new ApiException(409, "Студент с таким ИСУ уже существует",
                Map.of("isu", "Студент с таким ИСУ уже существует"));
    }

    // Список и его количество нужны контроллеру для тела ответа и заголовков
    public static class SearchResult {
        public List<StudentDto> students;
        public long total;
        public Integer page;
        public int size;

        public SearchResult(List<StudentDto> students, long total, Integer page, int size) {
            this.students = students;
            this.total = total;
            this.page = page;
            this.size = size;
        }
    }
}
