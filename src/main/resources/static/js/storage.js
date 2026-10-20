const API_URL = '/api/requests';

// Токены хранятся только в текущей вкладке браузера
function saveTokens(tokens) {
    sessionStorage.setItem('accessToken', tokens.accessToken);
    sessionStorage.setItem('refreshToken', tokens.refreshToken);
}

// Эта проверка управляет кнопками, реальные права проверяет сервер
function isAdmin() {
    return sessionStorage.getItem('role') === 'ADMIN';
}

// При выходе удаляем данные входа из текущей вкладки
function logout() {
    sessionStorage.removeItem('accessToken');
    sessionStorage.removeItem('refreshToken');
    sessionStorage.removeItem('username');
    sessionStorage.removeItem('role');
    window.location.href = 'login.html';
}

// Получаем новую пару токенов по сохранённому токену обновления
async function refreshAccess() {
    const refreshToken = sessionStorage.getItem('refreshToken');
    if (!refreshToken) {
        return false;
    }
    const response = await fetch('/api/auth/refresh', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({refreshToken: refreshToken})
    });
    if (!response.ok) {
        return false;
    }
    saveTokens(await response.json());
    return true;
}

// Добавляем токен к запросу и одинаково обрабатываем ошибки сервера
async function apiRequest(url, options = {}) {
    if (!options.headers) {
        options.headers = {};
    }
    const token = sessionStorage.getItem('accessToken');
    if (token) {
        options.headers.Authorization = 'Bearer ' + token;
    }
    let response;
    try {
        response = await fetch(url, options);
        // При истечении access-токена обновляем его и повторяем запрос один раз
        if (response.status === 401 && await refreshAccess()) {
            options.headers.Authorization = 'Bearer ' + sessionStorage.getItem('accessToken');
            response = await fetch(url, options);
        }
    } catch (error) {
        throw new Error('Не удалось связаться с сервером');
    }
    if (response.status === 401) {
        logout();
        throw new Error('Войдите в систему');
    }
    const result = await response.json().catch(function () { return null; });
    if (!response.ok) {
        const error = new Error('Ошибка запроса к серверу');
        if (result && result.error) {
            error.message = result.error.message;
            error.errors = result.error.details;
        }
        error.status = response.status;
        throw error;
    }
    return result;
}

// Страницы студентов доступны после входа
if (!window.location.pathname.endsWith('login.html') && !sessionStorage.getItem('accessToken')) {
    window.location.replace('login.html');
}

// При большом числе фильтров используем прежний метод QUERY с телом JSON
function getStudents(filters = {}) {
    if (Object.keys(filters).length > 3) {
        return apiRequest(API_URL, {
            method: 'QUERY',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(filters)
        });
    }
    const query = new URLSearchParams(filters).toString();
    if (query !== '') {
        return apiRequest(API_URL + '?' + query);
    }
    return apiRequest(API_URL);
}

// ИСУ в адресе указывает, какого студента получить
function getStudentByIsu(isu) {
    return apiRequest(API_URL + '/' + encodeURIComponent(isu));
}

// Передаём все поля нового студента в теле запроса
function addStudent(student) {
    return apiRequest(API_URL, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(student)
    });
}

// Исходный ИСУ нужен, даже если пользователь меняет его в форме
function updateStudent(oldIsu, student) {
    return apiRequest(API_URL + '/' + encodeURIComponent(oldIsu), {
        method: 'PATCH',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(student)
    });
}

// Удаляем запись по ИСУ
function deleteStudent(isu) {
    return apiRequest(API_URL + '/' + encodeURIComponent(isu), {method: 'DELETE'});
}
