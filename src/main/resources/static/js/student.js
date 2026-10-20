// Берём ИСУ из адреса страницы
const returnButton = document.getElementById('return');
const parameters = new URLSearchParams(window.location.search);
const studentIsu = parameters.get('isu');
// Загружаем одного студента и заполняем текст на странице
async function showStudent() {
    try {
        const student = await getStudentByIsu(studentIsu);
        document.getElementById('name').textContent = student.name;
        document.getElementById('isu').textContent = student.isu;
        document.getElementById('group').textContent = student.group;
        document.getElementById('dorm').textContent = student.dorm;
        document.getElementById('room').textContent = student.room;
        document.getElementById('exp_date').textContent = student.expDate;
        document.getElementById('foreigner').textContent = 'Нет';
        if (student.foreigner) {
            document.getElementById('foreigner').textContent = 'Да';
        }
        document.getElementById('notes').textContent = student.notes || 'Нет заметок';
    } catch (error) {
        document.querySelector('h1').textContent = 'Не удалось загрузить студента';
        if (error.message === 'Студент не найден') {
            document.querySelector('h1').textContent = error.message;
        }
    }
}

showStudent();

returnButton.addEventListener('click', function () {
    window.location.href = 'index.html';
});
