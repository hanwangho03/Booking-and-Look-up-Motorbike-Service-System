// src/main/resources/static/js/admin.js

document.addEventListener('DOMContentLoaded', function() {
    const sidebar = document.getElementById('sidebar');
    const sidebarToggle = document.getElementById('sidebarToggle');
    const navbarToggle = document.getElementById('navbarToggle');
    const logoutButton = document.getElementById('logoutButton');

    // --- Định nghĩa các hằng số API ---
    const USER_INFO_API = '/api/user/info';
    const LOGIN_PAGE_URL = '/auth/login';
    const ACCESS_DENIED_PAGE_URL = '/admin/error/403'; // URL trang lỗi 403

    function showTemporaryMessage(msg, type = 'error') {
        console.error(`[Admin Role Check ${type.toUpperCase()}]: ${msg}`);
    }


    // --- Logic kiểm tra vai trò người dùng khi tải trang ---
    async function checkUserRoleOnAdminPageLoad() {
        try {
            const token = localStorage.getItem('jwtToken') || sessionStorage.getItem('jwtToken');
            if (!token) {
                showTemporaryMessage('Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.');
                setTimeout(() => {
                    window.location.href = LOGIN_PAGE_URL;
                }, 1000); // Chuyển hướng nhanh hơn
                return;
            }

            const response = await fetch(USER_INFO_API, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                }
            });

            const userInfoData = await response.json();

            if (response.ok && userInfoData.success && userInfoData.data) {
                const role = userInfoData.data.role;
                if (role !== 'admin') {
                    showTemporaryMessage('Bạn không có quyền truy cập trang quản trị.', 'error');
                    window.location.href = ACCESS_DENIED_PAGE_URL;
                    return;
                }
                console.log('User is an admin. Proceeding to load admin functionalities.');
            } else {
                showTemporaryMessage(userInfoData.message || 'Không thể lấy thông tin người dùng. Vui lòng đăng nhập lại.', 'error');
                setTimeout(() => {
                    window.location.href = LOGIN_PAGE_URL;
                }, 1000);
            }
        } catch (error) {
            console.error('Lỗi khi kiểm tra thông tin người dùng trên trang admin:', error);
            showTemporaryMessage('Có lỗi xảy ra khi kiểm tra quyền truy cập. Vui lòng thử lại sau.', 'error');
            setTimeout(() => {
                window.location.href = LOGIN_PAGE_URL;
            }, 1000);
        }
    }

    // Gọi hàm kiểm tra vai trò ngay lập tức khi DOMContentLoaded
    checkUserRoleOnAdminPageLoad();


    // Toggle Sidebar từ Sidebar Header
    if (sidebarToggle) {
        sidebarToggle.addEventListener('click', function() {
            sidebar.classList.toggle('collapsed');
        });
    }

    // Toggle Sidebar từ Navbar (thường dùng cho mobile/tablet)
    if (navbarToggle) {
        navbarToggle.addEventListener('click', function() {
            sidebar.classList.toggle('active');
        });
    }

    // Xử lý Logout
    if (logoutButton) {
        logoutButton.addEventListener('click', function(event) {
            event.preventDefault();
            if (confirm('Bạn có chắc chắn muốn đăng xuất không?')) {
                // Xóa token hoặc thông tin đăng nhập khỏi localStorage/sessionStorage
                localStorage.removeItem('jwtToken');
                sessionStorage.removeItem('userInfo');

                // Chuyển hướng về trang đăng nhập
                window.location.href = LOGIN_PAGE_URL;
            }
        });
    }

    // Xử lý Active Menu Item
    const currentPath = window.location.pathname;
    const sidebarLinks = document.querySelectorAll('.sidebar-nav ul li a');

    sidebarLinks.forEach(link => {
        if (currentPath.startsWith(link.getAttribute('href'))) {
            link.parentElement.classList.add('active');
        } else {
            link.parentElement.classList.remove('active');
        }
    });

    // Xử lý hiển thị dropdown user
    const userMenu = document.querySelector('.user-menu');
    if (userMenu) {
        userMenu.addEventListener('click', function(event) {
            event.stopPropagation();
            const dropdownContent = userMenu.querySelector('.dropdown-content');
            dropdownContent.style.display = dropdownContent.style.display === 'block' ? 'none' : 'block';
        });

        document.addEventListener('click', function(event) {
            const dropdownContent = userMenu.querySelector('.dropdown-content');
            if (!userMenu.contains(event.target) && dropdownContent.style.display === 'block') {
                dropdownContent.style.display = 'none';
            }
        });
    }
});