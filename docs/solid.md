# SOLID в лабораторной №3

В проекте принципы применяются к границам HTTP, прикладных операций, проверки данных, хранения и авторизации. Интерфейс определяет поведение, реализация выполняет работу, а Spring связывает их через конструкторы. Интерфейс сам по себе не доказывает соблюдение SOLID: важны обязанности, зависимости и поведение реализаций.

## S — Single Responsibility: одна причина для изменения

| Компонент | Обязанность |
|---|---|
| `StudentController`, `AuthController` | Получить HTTP-запрос и оформить ответ |
| `StudentService` | Выполнить сценарий работы со студентами в транзакции |
| `StudentValidator` | Прочитать JSON и запустить правила проверки |
| `StudentIdentityRule`, `StudentHousingRule`, `StudentStatusRule` | Проверить соответствующую группу полей |
| `StudentMapper` | Перенести данные между сущностью и DTO |
| `StudentSearchParser` | Разобрать параметры поиска |
| `StudentSearch` | Хранить проверенные неизменяемые критерии |
| `RegistrationService` | Создать учётную запись с назначенной сервером ролью |
| `RegistrationValidator` | Проверить поля регистрации |
| `AuthService` | Проверить учётные данные или refresh и запросить токены |
| `UserProfileService` | Получить профиль текущего пользователя |
| `AdminAccountService` | Обеспечить существование администратора с паролем из настроек |
| `AdminInitializer` | Передать настройки в сценарий инициализации при старте |
| `JwtService` | Создать и проверить JWT средствами JJWT |
| `JpaStudentRepository`, `JpaUserStore` | Выполнить операции хранения через JPA |

В `AuthController` больше нет репозитория. `AuthService` не создаёт пользователей и не конструирует BCrypt. В `StudentDto` нет зависимости от Hibernate-сущности. Контроллеры не знают конкретных прикладных сервисов.

## O — Open/Closed: расширение без изменения стабильного кода

`StudentValidator` получает `List<StudentValidationRule>` через конструктор. Spring собирает список из компонентов, реализующих этот контракт. Для нового бизнес-ограничения достаточно добавить класс:

```java
@Component
public class StudentNotesRule implements StudentValidationRule {
    @Override
    public void validate(StudentDto student, Map<String, String> errors) {
        if (student.notes != null && student.notes.length() > 1000) {
            errors.putIfAbsent("notes", "Не более 1000 символов");
        }
    }
}
```

Это пример расширения; такого ограничения сейчас нет. `StudentService` и `StudentValidator` при добавлении правила менять не нужно. `StudentValidationTest` проверяет подключение дополнительного правила.

Хранилище и реализацию токенов тоже можно заменить адаптером существующих интерфейсов. В конфигурации Spring следует выбрать одну реализацию каждого контракта, например через профиль или `@Primary`. Изменение самой структуры студента или HTTP-контракта по-прежнему требует изменения соответствующего кода.

## L — Liskov Substitution: сохранение контракта при замене

Принцип относится и к реализациям интерфейсов. Наследование классов специально ради демонстрации здесь не требуется.

Контракты явно задают ожидания потребителей:

- `StudentReader` возвращает `null` для отсутствующего ИСУ и непустую ссылку на список для поиска; чтение не меняет хранилище.
- `StudentWriter.saveAndFlush` возвращает сохранённую сущность с идентификатором. Сервис использует возвращённый объект, поэтому адаптер вправе вернуть другой экземпляр после merge.
- `StudentInputReader` принимает `null` как основу полной записи, копию данных как основу PATCH, не меняет переданную основу и сообщает об ошибках через `ApiException`.
- Любое `StudentValidationRule` принимает незаполненные поля, добавляет ошибки, сохраняет чужие ошибки и не меняет данные студента.
- `AccessTokenReader` и `RefreshTokenReader` возвращают имя только для правильного типа действующего токена. Неверный, просроченный или повреждённый токен приводит к `InvalidTokenException`.

`TokenContractTest` запускает один набор ожиданий для реального `JwtService` и тестовой реализации с непрозрачными токенами. В обоих случаях неизменённый `AuthService` выполняет вход и refresh. Отдельно проверяются просроченный JWT и другой ключ подписи. `StudentValidationTest` проверяет общий контракт встроенных правил и отсутствие изменения основы при PATCH, в том числе при ошибке.

Тестовая реализация токенов находится только в `src/test` и не включается в приложение.

## I — Interface Segregation: интерфейсы под нужды потребителя

| Потребитель | Нужные контракты |
|---|---|
| `StudentController` | `StudentQueries`, `StudentCommands` |
| `StudentService` | `StudentReader`, `StudentWriter`, `StudentInputReader` |
| `AuthController` | `Registration`, `Authentication`, `UserProfile` |
| `AuthService` | `UserReader`, `PasswordEncoder`, `TokenIssuer`, `RefreshTokenReader` |
| `RegistrationService` | `UserReader`, `UserWriter`, `PasswordEncoder` |
| `AdminAccountService` | `UserReader`, `UserWriter`, `UserProvisioning`, `PasswordEncoder` |
| `AdminInitializer` | `AdministratorAccounts` |
| `JwtFilter` | `AccessTokenReader`, `UserReader` |
| `UserProfileService` | `UserReader` |

Фильтр не может выдавать токены или сохранять пользователя через свои зависимости. Сервис профиля не зависит от методов записи. Публичная регистрация получает только `Registration`, а назначение роли доступно внутреннему `UserProvisioning`. Сервисы не получают весь интерфейс Spring Data `JpaRepository`.

## D — Dependency Inversion: сценарии зависят от контрактов

Сервисы получают инфраструктуру через интерфейсы, а конкретные адаптеры реализуют эти интерфейсы:

```text
StudentController → StudentQueries / StudentCommands ← StudentService
StudentService → StudentReader / StudentWriter ← JpaStudentRepository → EntityManager
StudentService → StudentInputReader ← StudentValidator → StudentValidationRule ← правила

AuthController → Registration ← RegistrationService
AuthController → Authentication ← AuthService
AuthController → UserProfile ← UserProfileService

Сервисы пользователей → UserReader / UserWriter ← JpaUserStore → UserRepository → JPA
AuthService → PasswordEncoder ← BCryptPasswordEncoder (bean в SecurityConfig)
AuthService → TokenIssuer / RefreshTokenReader ← JwtService
JwtFilter → AccessTokenReader ← JwtService
AdminInitializer → AdministratorAccounts ← AdminAccountService
```

`AuthService` и `JwtFilter` не импортируют классы JJWT. Адаптер `JwtService` переводит исключения библиотеки в общее `InvalidTokenException`. Переход на другой механизм токенов не требует менять обработку ошибок в потребителях.

Конструкторное внедрение обеспечивает передачу зависимостей, а зависимость от интерфейса обеспечивает инверсию. Эти понятия связаны, но не равнозначны. Простые DTO, неизменяемые критерии и вспомогательные преобразователи не требуют отдельного интерфейса для каждого класса.

## Проверка и совместимость

Команда из корня проекта:

```sh
mvn clean verify
```

В IntelliJ IDEA: Maven → Lifecycle → verify. Используется JDK 17–25. Тесты не требуют Mockito или подключения JVM-агента.

`ApiIntegrationTest` поднимает Spring с H2 в памяти и проверяет через MockMvc реальные фильтры, контроллеры, сервисы и JPA. Свойства заданы в тесте, чтение `.env` отключено. Рабочая файловая БД не используется. Отдельные тесты проверяют расширение правил, контракты токенов, неизменяемость критериев и некорректные фильтры.

Сохранены адреса API, поля JSON, статусы, заголовки страниц, различия PUT/PATCH, QUERY, старый фильтр `dormitory`, роли USER/ADMIN и обновление пароля администратора при старте. Модель БД, настройки `.env` и frontend не требуют миграции.

Для защиты: «HTTP, прикладные сценарии и инфраструктура разделены; проверки расширяются новыми правилами; реализации соблюдают явные контракты; интерфейсы разделены по потребителям; сервисы зависят от абстракций хранения, паролей и токенов».
