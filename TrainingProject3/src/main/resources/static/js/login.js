 document.getElementById('loginForm').addEventListener('submit', async function(event) {
    event.preventDefault();

    const phoneNumber = document.getElementById('phoneNumber').value;
    const password = document.getElementById('password').value;

    const messageDiv = document.getElementById('message');

    messageDiv.style.display = 'none';
    messageDiv.className = 'message';
    document.querySelectorAll('.input-error-message').forEach(span => span.textContent = '');
    document.querySelectorAll('input').forEach(input => input.classList.remove('error-input'));

    try {
    const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: {
    'Content-Type': 'application/json'
},
    body: JSON.stringify({ phoneNumber, password })
});

    const responseData = await response.json();

    if (response.ok) {
    messageDiv.textContent = responseData.message || 'Đăng nhập thành công!';
    messageDiv.classList.add('success-message');
    messageDiv.style.display = 'block';

    if (responseData.data && responseData.data.jwt) {
    const token = responseData.data.jwt;
    localStorage.setItem('jwtToken', token);

    try {
    const userInfoRes = await fetch('/api/user/info', {
    method: 'GET',
    headers: {
    'Authorization': 'Bearer ' + token
}
});

    const userInfoData = await userInfoRes.json();
    if (userInfoRes.ok && userInfoData.success && userInfoData.data) {
    sessionStorage.setItem('userInfo', JSON.stringify(userInfoData.data));
} else {
    console.warn('Không thể lấy thông tin người dùng sau đăng nhập:', userInfoData.message || userInfoData);
}
} catch (userFetchError) {
    console.error('Lỗi khi lấy thông tin người dùng:', userFetchError);
}
}

    document.getElementById('loginForm').reset();

    setTimeout(() => {
    window.location.href = '/';
}, 1000);

} else {
    let displayMessage = 'Đăng nhập thất bại. Vui lòng thử lại.';
    messageDiv.classList.add('error-message');

    if (responseData && responseData.error) {
    if (responseData.error.message) {
    displayMessage = responseData.error.message;
} else if (responseData.message) {
    displayMessage = responseData.message;
}
} else if (responseData && responseData.message) {
    displayMessage = responseData.message;
}

    messageDiv.textContent = displayMessage;
    messageDiv.style.display = 'block';

    console.error('Đăng nhập thất bại (chi tiết từ server):', responseData);
}
} catch (error) {
    console.error('Lỗi khi gửi yêu cầu đăng nhập hoặc xử lý phản hồi:', error);
    messageDiv.textContent = 'Có lỗi xảy ra trong quá trình kết nối hoặc xử lý dữ liệu. Vui lòng thử lại sau.';
    messageDiv.classList.add('error-message');
    messageDiv.style.display = 'block';
}
});