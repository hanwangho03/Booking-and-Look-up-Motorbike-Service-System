// src/main/resources/static/js/admin_dashboard.js

let allRepairHistories = [];
let filteredRepairHistories = [];
let currentPage = 1;
const rowsPerPage = 10;
const API_URL = '/api/admin/repair-history/all';
const USER_INFO_API = '/api/user/info';

document.addEventListener('DOMContentLoaded', () => {
    // Kiểm tra vai trò người dùng trước khi tải dữ liệu
    checkUserRole();
    document.getElementById('applyFilterBtn').addEventListener('click', applyFilter);
    document.getElementById('clearFilterBtn').addEventListener('click', clearFilter);
});

async function checkUserRole() {
    const messageDiv = document.getElementById('message');
    messageDiv.style.display = 'none';
    messageDiv.className = 'message';

    try {
        const token = localStorage.getItem('jwtToken') || sessionStorage.getItem('jwtToken');
        if (!token) {
            messageDiv.textContent = 'Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.';
            messageDiv.classList.add('error-message');
            messageDiv.style.display = 'block';
            setTimeout(() => {
                window.location.href = '/auth/login';
            }, 2000);
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
                messageDiv.textContent = 'Bạn không có quyền truy cập trang này.';
                messageDiv.classList.add('error-message');
                messageDiv.style.display = 'block';
                setTimeout(() => {
                    window.location.href = '/admin/error/403';
                });
                return;
            }
            fetchRepairHistories();
        } else {
            messageDiv.textContent = userInfoData.message || 'Không thể lấy thông tin người dùng.';
            messageDiv.classList.add('error-message');
            messageDiv.style.display = 'block';
            setTimeout(() => {
                window.location.href = '/auth/login';
            }, 2000);
        }
    } catch (error) {
        console.error('Lỗi khi lấy thông tin người dùng:', error);
        messageDiv.textContent = 'Có lỗi xảy ra khi kiểm tra thông tin người dùng. Vui lòng thử lại sau.';
        messageDiv.classList.add('error-message');
        messageDiv.style.display = 'block';
        setTimeout(() => {
            window.location.href = '/auth/login';
        }, 2000);
    }
}

async function fetchRepairHistories() {
    const messageDiv = document.getElementById('message');
    messageDiv.style.display = 'none';
    messageDiv.className = 'message';

    try {
        const token = localStorage.getItem('jwtToken') || sessionStorage.getItem('jwtToken');
        if (!token) {
            messageDiv.textContent = 'Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.';
            messageDiv.classList.add('error-message');
            messageDiv.style.display = 'block';
            console.error("No JWT token found.");
            setTimeout(() => {
                window.location.href = '/auth/login';
            }, 2000);
            return;
        }

        const response = await fetch(API_URL, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });

        const apiResponse = await response.json();

        if (response.ok && apiResponse.success) {
            allRepairHistories = apiResponse.data || [];
            filteredRepairHistories = [...allRepairHistories];
            displayRepairHistories(filteredRepairHistories);
            updateSummary();
            messageDiv.textContent = apiResponse.message || 'Tải dữ liệu lịch sử sửa chữa thành công.';
            messageDiv.classList.add('success-message');
        } else {
            messageDiv.textContent = apiResponse.message || 'Lỗi khi tải dữ liệu lịch sử sửa chữa.';
            messageDiv.classList.add('error-message');
            console.error('Error fetching repair histories:', apiResponse);
        }
    } catch (error) {
        messageDiv.textContent = 'Lỗi mạng hoặc không thể kết nối đến máy chủ.';
        messageDiv.classList.add('error-message');
        console.error('Network or server error:', error);
    } finally {
        messageDiv.style.display = 'block';
    }
}

function applyFilter() {
    const filterDateStr = document.getElementById('filterDate').value;
    if (filterDateStr) {
        const filterDate = new Date(filterDateStr);
        filteredRepairHistories = allRepairHistories.filter(historyEntry => {
            const sessionDate = new Date(historyEntry.repairHistory.sessionDate);
            return sessionDate.toDateString() === filterDate.toDateString();
        });
    } else {
        filteredRepairHistories = [...allRepairHistories];
    }
    currentPage = 1;
    displayRepairHistories(filteredRepairHistories);
    updateSummary();
}

function clearFilter() {
    document.getElementById('filterDate').value = '';
    filteredRepairHistories = [...allRepairHistories];
    currentPage = 1;
    displayRepairHistories(filteredRepairHistories);
    updateSummary();
}

function displayRepairHistories(histories) {
    const tableBody = document.querySelector('#repairHistoryTable tbody');
    tableBody.innerHTML = '';

    const startIndex = (currentPage - 1) * rowsPerPage;
    const endIndex = Math.min(startIndex + rowsPerPage, histories.length);
    const paginatedHistories = histories.slice(startIndex, endIndex);

    if (paginatedHistories.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="10" style="text-align: center;">Không tìm thấy lịch sử sửa chữa nào.</td></tr>';
        updatePagination(0, 0);
        return;
    }

    paginatedHistories.forEach((historyEntry, index) => {
        const row = tableBody.insertRow();
        const repair = historyEntry.repairHistory;

        row.insertCell().textContent = startIndex + index + 1;
        row.insertCell().textContent = historyEntry.licensePlate;
        row.insertCell().textContent = historyEntry.customerFullName || 'N/A';
        row.insertCell().textContent = historyEntry.customerPhoneNumber || 'N/A';
        row.insertCell().textContent = formatDateTime(repair.sessionDate);
        row.insertCell().textContent = repair.technicianFullName || 'N/A';
        row.insertCell().textContent = formatCurrency(repair.totalCost);

        const ratingCell = row.insertCell();
        ratingCell.innerHTML = renderStars(repair.customerRating);

        row.insertCell().textContent = repair.customerComment || 'Chưa có bình luận';

        const detailCell = row.insertCell();
        const detailButton = document.createElement('button');
        detailButton.textContent = 'Xem chi tiết';
        detailButton.onclick = () => showDetailModal(repair);
        detailCell.appendChild(detailButton);
    });

    updatePagination(histories.length, paginatedHistories.length);
}

function updateSummary() {
    document.getElementById('totalRepairs').textContent = filteredRepairHistories.length;
    const totalRevenue = filteredRepairHistories.reduce((sum, entry) => sum + entry.repairHistory.totalCost, 0);
    document.getElementById('totalRevenue').textContent = formatCurrency(totalRevenue);
}

function updatePagination(totalRows, currentRows) {
    const paginationDiv = document.getElementById('pagination');
    paginationDiv.innerHTML = '';
    const totalPages = Math.ceil(totalRows / rowsPerPage);

    for (let i = 1; i <= totalPages; i++) {
        const button = document.createElement('button');
        button.textContent = i;
        button.classList.toggle('active', i === currentPage);
        button.onclick = () => {
            currentPage = i;
            displayRepairHistories(filteredRepairHistories);
        };
        paginationDiv.appendChild(button);
    }
}

function showDetailModal(repairHistory) {
    let modal = document.getElementById('detailModal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'detailModal';
        modal.classList.add('modal');
        document.body.appendChild(modal);
    }

    modal.innerHTML = `
        <div class="modal-content">
            <span class="close-button" onclick="closeDetailModal()">×</span>
            <h3>Chi tiết phiên sửa chữa</h3>
            <ul>
                <li><strong>Mã phiên:</strong> ${repairHistory.id}</li>
                <li><strong>Ngày sửa:</strong> ${formatDateTime(repairHistory.sessionDate)}</li>
                <li><strong>Kỹ thuật viên:</strong> ${repairHistory.technicianFullName || 'N/A'}</li>
                <li><strong>Tổng chi phí:</strong> ${formatCurrency(repairHistory.totalCost)}</li>
                <li><strong>Ghi chú kỹ thuật viên:</strong> ${repairHistory.technicianNotes || 'Không có'}</li>
            </ul>
            <h4>Dịch vụ đã thực hiện:</h4>
            <ul>
                ${repairHistory.servicesPerformed && repairHistory.servicesPerformed.length > 0 ?
        repairHistory.servicesPerformed.map(s => `<li>- ${s.serviceName} (${formatCurrency(s.serviceCost)})</li>`).join('') :
        '<li>Không có dịch vụ nào.</li>'
    }
            </ul>
            <h4>Phụ tùng thay thế:</h4>
            <ul>
                ${repairHistory.partsUsed && repairHistory.partsUsed.length > 0 ?
        repairHistory.partsUsed.map(p => `<li>- ${p.partName} (SL: ${p.quantity}, Đơn giá: ${formatCurrency(p.unitPrice)})</li>`).join('') :
        '<li>Không có phụ tùng nào.</li>'
    }
            </ul>
            <h4>Đánh giá khách hàng:</h4>
            <ul>
                <li><strong>Người đánh giá:</strong> ${repairHistory.reviewerFullName || 'Ẩn danh'}</li>
                <li><strong>Điểm:</strong> ${renderStars(repairHistory.customerRating)}</li>
                <li><strong>Bình luận:</strong> ${repairHistory.customerComment || 'Không có bình luận.'}</li>
            </ul>
        </div>
    `;
    modal.style.display = 'block';
}

function closeDetailModal() {
    document.getElementById('detailModal').style.display = 'none';
}

function formatCurrency(amount) {
    if (amount === null || amount === undefined) {
        return '';
    }
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
}

function formatDateTime(isoString) {
    if (!isoString) return '';
    const date = new Date(isoString);
    return date.toLocaleString('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
    });
}

function renderStars(rating) {
    if (rating === null || rating === undefined) {
        return 'Chưa có đánh giá';
    }
    let stars = '';
    for (let i = 0; i < rating; i++) {
        stars += '★';
    }
    for (let i = rating; i < 5; i++) {
        stars += '☆';
    }
    return `<span class="rating-stars">${stars}</span>`;
}