// Эта проверка нужна для интерфейса. Настоящие права проверяет сервер
if (!isAdmin()) {
    window.location.replace('index.html');
}

// Один файл обслуживает создание и редактирование студента
const form = document.getElementById("studentForm");
const returnButton = document.getElementById("return");
const parameters = new URLSearchParams(window.location.search);
const studentIsu = parameters.get("isu");

const expDateInput = document.getElementById('exp_date');
// Для новой записи по умолчанию ставим сегодняшнюю дату
const today = new Date();
const year = today.getFullYear();
const month = String(today.getMonth() + 1).padStart(2, '0');
const day = String(today.getDate()).padStart(2, '0');
const todayString = year + '-' + month + '-' + day;

expDateInput.min = '0001-01-01';
expDateInput.value = todayString;

// При редактировании сначала загружаем существующие данные
async function loadStudent() {
    if (studentIsu === null) {
        return;
    }
    document.getElementById('submit').disabled = true;
    try {
        const existingStudent = await getStudentByIsu(studentIsu);
        document.querySelector('h1').textContent = 'Редактирование студента';
        document.getElementById('submit').textContent = 'Сохранить изменения';
        document.getElementById('name').value = existingStudent.name;
        document.getElementById('group').value = existingStudent.group;
        document.getElementById('isu').value = existingStudent.isu;
        document.getElementById('dorm').value = existingStudent.dorm;
        document.getElementById('room').value = existingStudent.room;
        document.getElementById('exp_date').value = existingStudent.expDate;
        document.getElementById('foreigner').checked = existingStudent.foreigner;
        document.getElementById('notes').value = existingStudent.notes || '';
        document.getElementById('submit').disabled = false;
    } catch (error) {
        document.querySelector('h1').textContent = 'Не удалось загрузить студента';
        if (error.message === 'Студент не найден') {
            document.querySelector('h1').textContent = error.message;
        }
    }
}

loadStudent();

form.addEventListener("submit", async function (event) {
    // Отменяем стандартную отправку формы и перезагрузку страницы
    event.preventDefault();
    // Очищаем ранее установленные пользовательские ошибки валидации
    document.getElementById('name').setCustomValidity('');
    document.getElementById('group').setCustomValidity('');
    document.getElementById('isu').setCustomValidity('');
    document.getElementById('dorm').setCustomValidity('');
    document.getElementById('room').setCustomValidity('');
    expDateInput.setCustomValidity('');

    // Собираем значения полей в объект для отправки серверу
    const student = {
        name: document.getElementById('name').value.trim(),
        group: document.getElementById('group').value.trim(),
        isu: Number(document.getElementById('isu').value),
        dorm: Number(document.getElementById('dorm').value),
        room: document.getElementById('room').value.trim(),
        expDate: document.getElementById('exp_date').value,
        foreigner: document.getElementById('foreigner').checked,
        notes: document.getElementById('notes').value.trim()
    };
    // ФИО должно содержать хотя бы два слова из букв
    if (student.name !== '' && !/^\p{L}+(?:\s+\p{L}+)+$/u.test(student.name)) {
        document.getElementById('name').setCustomValidity(
            'ФИО должно состоять из двух или более слов, содержащих только буквы');
    }

    // Группа состоит из латинской буквы и четырёх цифр
    if (student.group !== '' && !/^[A-Za-z][1-9][0-9]{3}$/.test(student.group)) {
        document.getElementById('group').setCustomValidity(
            'Группа должна состоять из одной латинской буквы и четырёх цифр, например P1234');
    }

    if (student.isu < 100000 || student.isu > 999999) {
        document.getElementById('isu').setCustomValidity('ИСУ должен быть числом от 100000 до 999999');
    }

    // Номер общежития должен попадать в диапазон от 1 до 100
    if (student.dorm < 1 || student.dorm > 100) {
        document.getElementById('dorm').setCustomValidity(
            'Номер общежития должен быть числом от 1 до 100');
    }

    // После 2–4 цифр у номера комнаты может быть одна необязательная буква
    if (student.room !== '' && !/^[0-9]{2,4}[A-Za-zА-Яа-яЁё]?$/.test(student.room)) {
        document.getElementById('room').setCustomValidity(
            'Номер комнаты: от 2 до 4 цифр и необязательная буква, например 25А');
    }

    if (!form.reportValidity()) {   // Если форма невалидна, прекратить выполнение обработчика
        return;
    }
    // Если в URL нет параметра isu, значит создаём нового студента
    // Иначе обновляем данные студента, найденного по его старому ИСУ
    try {
        document.getElementById('submit').disabled = true;
        if (studentIsu === null) {
            await addStudent(student);
        } else {
            await updateStudent(studentIsu, student);
        }
        window.location.href = 'index.html';
    } catch (error) {
        // Ошибку конкретного поля показываем рядом с этим полем
        document.getElementById('submit').disabled = false;
        if (error.errors && typeof error.errors === 'object') {
            const field = Object.keys(error.errors)[0];
            let inputId = field;
            if (field === 'expDate') {
                inputId = 'exp_date';
            }
            const input = document.getElementById(inputId);
            if (input) {
                input.setCustomValidity(error.errors[field]);
                form.reportValidity();
                return;
            }
        }
        alert(error.message);
    }
});

returnButton.addEventListener('click', function () {
    window.location.href = 'index.html';
});
