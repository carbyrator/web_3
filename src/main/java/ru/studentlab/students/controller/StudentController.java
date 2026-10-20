package ru.studentlab.students.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.studentlab.students.dto.StudentDto;
import ru.studentlab.students.exception.ApiException;
import ru.studentlab.students.service.StudentSearch;
import ru.studentlab.students.service.StudentService;
import java.net.URI;
import java.util.List;
import java.util.Map;

// Получаем запросы по студентам и передаём работу сервису
@RestController
@RequestMapping("/api/requests")
@Tag(name = "Студенты")
@SecurityRequirement(name = "bearerAuth")
public class StudentController {
    private final StudentService service;
    private final ObjectMapper mapper;

    public StudentController(StudentService service, ObjectMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    // Параметры после знака вопроса задают фильтры и страницу
    @GetMapping
    @Operation(summary = "Список студентов", description = "Ответ — массив, как в лабораторной №2. "
            + "Фильтры объединяются через И. Без page и size возвращается весь список.")
    @Parameters({
            @Parameter(name = "name", description = "Часть ФИО"),
            @Parameter(name = "group", description = "Часть группы"),
            @Parameter(name = "isu", description = "Часть ИСУ"),
            @Parameter(name = "dorm", description = "Часть номера общежития"),
            @Parameter(name = "dormitory", description = "Альтернативное название dorm"),
            @Parameter(name = "room", description = "Часть номера комнаты"),
            @Parameter(name = "expDate", description = "Часть даты, например 2027-06"),
            @Parameter(name = "foreigner", description = "true или false"),
            @Parameter(name = "notes", description = "Часть заметки"),
            @Parameter(name = "sort", description = "Поле и направление, например isu,desc"),
            @Parameter(name = "page", description = "Номер страницы, начиная с 0"),
            @Parameter(name = "size", description = "Размер страницы от 1 до 100, по умолчанию 20")
    })
    public ResponseEntity<List<StudentDto>> list(@Parameter(hidden = true) @RequestParam Map<String, String> filters) {
        return searchResult(mapper.valueToTree(filters));
    }

    // ИСУ берём из адреса, например /api/requests/505301
    @GetMapping("/{isu}")
    @Operation(summary = "Получить студента по ИСУ")
    public StudentDto get(@PathVariable Integer isu) {
        return service.findByIsu(isu);
    }

    // После создания возвращаем статус 201 и адрес новой записи
    @PostMapping
    @Operation(summary = "Создать студента")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(schema = @Schema(implementation = StudentDto.class)))
    public ResponseEntity<StudentDto> create(@RequestBody JsonNode input) {
        StudentDto student = service.create(input);
        return ResponseEntity.created(URI.create("/api/requests/" + student.isu)).body(student);
    }

    // Полная замена требует все обязательные поля
    @PutMapping("/{isu}")
    @Operation(summary = "Полностью заменить данные студента", description = "Нужны все обязательные поля. "
            + "Если notes не передано, заметка очищается. ИСУ можно изменить, если новый номер свободен.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(schema = @Schema(implementation = StudentDto.class)))
    public StudentDto replace(@PathVariable Integer isu, @RequestBody JsonNode input) {
        return service.update(isu, input, false);
    }

    // Частичное изменение сохраняет остальные поля студента
    @PatchMapping("/{isu}")
    @Operation(summary = "Изменить отдельные поля", description = "Передаются только изменяемые поля, "
            + "например {\"room\":\"102\"}. Пустой объект и null вместо поля не допускаются.")
    public StudentDto patch(@PathVariable Integer isu, @RequestBody JsonNode input) {
        return service.update(isu, input, true);
    }

    // После удаления возвращаем статус 204 без тела ответа
    @DeleteMapping("/{isu}")
    @Operation(summary = "Удалить студента")
    public ResponseEntity<Void> delete(@PathVariable Integer isu) {
        service.delete(isu);
        return ResponseEntity.noContent().build();
    }

    // Поддерживаем запрос QUERY с фильтрами в JSON из прошлой лабораторной
    @Hidden
    @RequestMapping
    public ResponseEntity<List<StudentDto>> legacyQuery(HttpServletRequest request,
                                                       @RequestBody(required = false) JsonNode filters) {
        if (!request.getMethod().equals("QUERY")) {
            throw new ApiException(405, "Метод не поддерживается");
        }
        if (filters == null) {
            throw new ApiException(400, "Ожидался JSON-объект с фильтрами");
        }
        return searchResult(filters);
    }

    // Данные списка идут в JSON, количество и страница в заголовки
    private ResponseEntity<List<StudentDto>> searchResult(JsonNode filters) {
        StudentService.SearchResult result = service.findAll(new StudentSearch(filters));
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        response.header("X-Total-Count", Long.toString(result.total));
        if (result.page != null) {
            response.header("X-Page", result.page.toString());
            response.header("X-Page-Size", Integer.toString(result.size));
        }
        return response.body(result.students);
    }
}
