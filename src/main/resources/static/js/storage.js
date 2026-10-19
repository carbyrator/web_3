const API_URL = '/api/requests';

function saveTokens(tokens) {
    sessionStorage.setItem('accessToken', tokens.accessToken);
    sessionStorage.setItem('refreshToken', tokens.refreshToken);
}

function isAdmin() {
    return sessionStorage.getItem('role') === 'ADMIN';
}

function logout() {
    sessionStorage.removeItem('accessToken');
    sessionStorage.removeItem('refreshToken');
    sessionStorage.removeItem('username');
    sessionStorage.removeItem('role');
    window.location.href = 'login.html';
}

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
        // При истечении access-токена обновляем его и повторяем запрос один раз.
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

if (!window.location.pathname.endsWith('login.html') && !sessionStorage.getItem('accessToken')) {
    window.location.replace('login.html');
}

function getStudents(filters = {}) {
    const entries = Object.entries(filters);
    if (entries.length > 3) {
        return apiRequest(API_URL, {
            method: 'QUERY',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(filters)
        });
    }
    const query = new URLSearchParams(filters).toString();
    return apiRequest(query ? `${API_URL}?${query}` : API_URL);
}

function getStudentByIsu(isu) {
    return apiRequest(`${API_URL}/${encodeURIComponent(isu)}`);
}

function addStudent(student) {
    return apiRequest(API_URL, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(student)
    });
}

function updateStudent(oldIsu, student) {
    return apiRequest(`${API_URL}/${encodeURIComponent(oldIsu)}`, {
        method: 'PATCH',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(student)
    });
}

function deleteStudent(isu) {
    return apiRequest(`${API_URL}/${encodeURIComponent(isu)}`, {method: 'DELETE'});
}
