// Показываем сообщения входа и регистрации на странице
const message = document.getElementById('authMessage');

// Для входа и регистрации access-токен ещё не нужен
async function sendAuth(path, data) {
    const response = await fetch('/api/auth/' + path, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(data)
    });
    const result = await response.json().catch(function () { return null; });
    if (!response.ok) {
        let text = 'Ошибка запроса';
        if (result && result.error) {
            text = result.error.message;
            const details = Object.values(result.error.details);
            if (details.length > 0) {
                text += ': ' + details.join('; ');
            }
        }
        throw new Error(text);
    }
    return result;
}

// После входа сохраняем токены, имя и роль для текущей вкладки
document.getElementById('loginForm').addEventListener('submit', async function (event) {
    event.preventDefault();
    message.textContent = '';
    try {
        const tokens = await sendAuth('login', {
            username: document.getElementById('loginUsername').value.trim(),
            password: document.getElementById('loginPassword').value
        });
        saveTokens(tokens);
        const user = await apiRequest('/api/auth/me');
        sessionStorage.setItem('username', user.username);
        sessionStorage.setItem('role', user.role);
        window.location.href = 'index.html';
    } catch (error) {
        message.textContent = error.message;
    }
});

// После регистрации предлагаем войти под созданным именем
document.getElementById('registerForm').addEventListener('submit', async function (event) {
    event.preventDefault();
    message.textContent = '';
    try {
        await sendAuth('register', {
            username: document.getElementById('registerUsername').value.trim(),
            email: document.getElementById('registerEmail').value.trim(),
            password: document.getElementById('registerPassword').value
        });
        document.getElementById('registerPassword').value = '';
        message.textContent = 'Регистрация успешна. Теперь войдите под своим именем.';
    } catch (error) {
        message.textContent = error.message;
    }
});
