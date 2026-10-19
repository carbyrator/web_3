package ru.studentlab.students.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.studentlab.students.model.Student;
import ru.studentlab.students.service.StudentSearch;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// EntityManager — объект JPA, через который Hibernate читает и меняет записи БД.
@Repository
@Transactional(readOnly = true)
public class StudentRepository {
    private final EntityManager entityManager;

    public StudentRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Student findByIsu(Integer isu) {
        TypedQuery<Student> request = entityManager.createQuery(
                "select s from Student s where s.isu = :isu", Student.class);
        request.setParameter("isu", isu);
        List<Student> students = request.getResultList();
        if (students.isEmpty()) {
            return null;
        }
        return students.get(0);
    }

    public List<Student> findAll(StudentSearch search) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        String where = makeWhere(search, parameters);
        String order = " order by s." + search.sortField + " " + search.sortDirection;
        if (!search.sortField.equals("id")) {
            order += ", s.id asc";
        }
        TypedQuery<Student> request = entityManager.createQuery(
                "select s from Student s" + where + order, Student.class);
        for (String name : parameters.keySet()) {
            request.setParameter(name, parameters.get(name));
        }
        if (search.page != null) {
            request.setFirstResult(search.page * search.size);
            request.setMaxResults(search.size);
        }
        return request.getResultList();
    }

    public long count(StudentSearch search) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        String where = makeWhere(search, parameters);
        TypedQuery<Long> request = entityManager.createQuery(
                "select count(s) from Student s" + where, Long.class);
        for (String name : parameters.keySet()) {
            request.setParameter(name, parameters.get(name));
        }
        return request.getSingleResult();
    }

    @Transactional
    public Student saveAndFlush(Student student) {
        if (student.getId() == null) {
            entityManager.persist(student);  // INSERT: новая запись.
        } else {
            student = entityManager.merge(student);  // UPDATE: существующая запись.
        }
        entityManager.flush();
        return student;
    }

    @Transactional
    public void delete(Student student) {
        entityManager.remove(entityManager.merge(student));
        entityManager.flush();
    }

    private String makeWhere(StudentSearch search, Map<String, Object> parameters) {
        String where = " where 1 = 1";
        for (String field : search.filters.keySet()) {
            String value = search.filters.get(field);
            if (field.equals("foreigner")) {
                where += " and s.foreigner = :foreigner";
                parameters.put("foreigner", Boolean.parseBoolean(value));
            } else {
                where += " and " + contains(field, field);
                parameters.put(field, pattern(value));
            }
        }
        // В запрос попадают только проверенные имена полей из StudentSearch.
        // Значения пользователя передаются отдельно через setParameter.
        return where;
    }

    private String contains(String field, String parameter) {
        // Приведение к строке нужно для поиска по части ИСУ, номера общежития и даты.
        return "lower(cast(s." + field + " as string)) like :" + parameter + " escape '\\'";
    }

    private String pattern(String value) {
        value = value.toLowerCase(Locale.ROOT);
        value = value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + value + "%";
    }
}
