document.addEventListener('DOMContentLoaded', function () {
    console.log('--- Script header.js loaded ---');

    const userInfoBtn = document.getElementById('user-info-btn');
    const logoutBtn = document.getElementById('logout-btn');
    const loginLink = document.getElementById('login-link');
    const userDisplayName = document.getElementById('user-display-name');
    // Get the admin dashboard link element
    const adminDashboardLink = document.getElementById('admin-dashboard-link'); // Make sure this ID exists in your header.html

    const USER_INFO_API = '/api/user/info';

    function getLastName(fullName) {
        if (!fullName) return '';
        const parts = fullName.trim().split(/\s+/);
        return parts[parts.length - 1];
    }

    async function updateAuthUI() { // Made async to allow fetching
        const jwtToken = localStorage.getItem('jwtToken');
        let userInfo = null;

        // Try to get userInfo from sessionStorage first
        const userInfoRaw = sessionStorage.getItem('userInfo');
        if (userInfoRaw) {
            try {
                userInfo = JSON.parse(userInfoRaw);
            } catch (e) {
                console.error("Lỗi khi parse userInfo từ sessionStorage:", e);
                sessionStorage.removeItem('userInfo'); // Clear invalid data
            }
        }

        if (jwtToken) {
            if (!userInfo) {
                try {
                    const response = await fetch(USER_INFO_API, {
                        method: 'GET',
                        headers: {
                            'Authorization': 'Bearer ' + jwtToken,
                            'Content-Type': 'application/json'
                        }
                    });

                    if (response.status === 401 || response.status === 403) {
                        localStorage.removeItem('jwtToken');
                        sessionStorage.removeItem('userInfo');
                        console.warn('Phiên đăng nhập đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại.');
                        showLoggedOutState();
                        return;
                    }

                    const data = await response.json();
                    if (data.success && data.data) {
                        userInfo = data.data;
                        sessionStorage.setItem('userInfo', JSON.stringify(userInfo)); // Store fresh data
                    } else {
                        console.warn('Không thể lấy thông tin người dùng từ API:', data.message || 'Lỗi không xác định.');
                        showLoggedOutState();
                        return;
                    }
                } catch (error) {
                    console.error('Lỗi khi fetch thông tin người dùng:', error);
                    showLoggedOutState();
                    return;
                }
            }

            if (userInfo) {
                const displayName = userInfo.fullName ? getLastName(userInfo.fullName) : userInfo.username || 'Người dùng';

                if (userDisplayName) {
                    userDisplayName.textContent =  displayName;
                    userDisplayName.style.display = 'inline-block';
                }
                if (logoutBtn) {
                    logoutBtn.style.display = 'inline-block';
                }
                if (loginLink) {
                    loginLink.style.display = 'none';
                }

                // --- Admin Role Check ---
                if (adminDashboardLink) {
                    if (userInfo.role === 'admin') {
                        adminDashboardLink.style.display = 'list-item';
                    } else {
                        adminDashboardLink.style.display = 'none';
                    }
                }
                // --- End Admin Role Check ---

            } else {
                showLoggedOutState();
            }

        } else {
            // No JWT token found
            showLoggedOutState();
        }
    }

    // Function to set the UI to a logged-out state
    function showLoggedOutState() {
        if (userDisplayName) userDisplayName.style.display = 'none';
        if (logoutBtn) logoutBtn.style.display = 'none';
        if (loginLink) loginLink.style.display = 'inline-block';
        if (adminDashboardLink) adminDashboardLink.style.display = 'none';
    }


    // Initial UI update when script loads
    updateAuthUI();

    // Event listener for "Thông tin" button
    if (userInfoBtn) {
        userInfoBtn.addEventListener('click', async function (event) { // Made async
            event.preventDefault();

            const jwtToken = localStorage.getItem('jwtToken');

            if (!jwtToken) {
                alert('Vui lòng đăng nhập để xem thông tin.');
                window.location.href = '/api/auth/login';
                return;
            }

            try {
                const response = await fetch(USER_INFO_API, {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + jwtToken,
                        'Content-Type': 'application/json'
                    }
                });

                if (response.status === 401 || response.status === 403) {
                    localStorage.removeItem('jwtToken');
                    sessionStorage.removeItem('userInfo');
                    alert('Phiên đăng nhập đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại.');
                    window.location.href = '/api/auth/login';
                    return;
                }

                const data = await response.json();
                if (data.success && data.data) {
                    sessionStorage.setItem('userInfo', JSON.stringify(data.data));
                    updateAuthUI();
                    window.location.href = '/user-info';
                } else {
                    alert(data.message || 'Không thể lấy thông tin người dùng.');
                }
            } catch (error) {
                console.error('Lỗi lấy thông tin:', error);
                alert('Có lỗi xảy ra khi lấy thông tin người dùng.');
            }
        });
    }

    // Event listener for "Đăng xuất" button
    if (logoutBtn) {
        logoutBtn.addEventListener('click', function (event) {
            event.preventDefault();

            localStorage.removeItem('jwtToken');
            sessionStorage.removeItem('userInfo');

            showLoggedOutState();
            alert('Bạn đã đăng xuất thành công!');
            window.location.href = '/api/auth/login';
        });
    }
});