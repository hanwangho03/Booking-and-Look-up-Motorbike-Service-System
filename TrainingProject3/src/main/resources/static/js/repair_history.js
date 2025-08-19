let currentUserInfo = null;

function loadUserInfoFromLocalStorage() {
    try {
        const userInfoRaw = localStorage.getItem("userInfo") || sessionStorage.getItem("userInfo");
        if (userInfoRaw) {
            currentUserInfo = JSON.parse(userInfoRaw);
            console.log("Thông tin người dùng từ localStorage:", currentUserInfo);
        } else {
            console.warn("Không tìm thấy thông tin người dùng trong localStorage/sessionStorage");
        }
    } catch (e) {
        console.error("Lỗi khi parse userInfo:", e);
    }
}

document.addEventListener("DOMContentLoaded", () => {
    loadUserInfoFromLocalStorage();

    const lastQuery = localStorage.getItem('lastQuery');
    if (lastQuery) {
        document.getElementById('query').value = lastQuery;
        document.getElementById('lookupForm').dispatchEvent(new Event('submit'));
        localStorage.removeItem('lastQuery');
    }
});

document.getElementById('lookupForm').addEventListener('submit', async function(event) {
    event.preventDefault();

    const query = document.getElementById('query').value.trim();
    const messageDiv = document.getElementById('message');
    const resultsContainer = document.getElementById('resultsContainer');
    const dynamicResultsContent = document.getElementById('dynamicResultsContent');

    messageDiv.style.display = 'none';
    messageDiv.className = 'message';
    resultsContainer.style.display = 'none';
    if (dynamicResultsContent) dynamicResultsContent.innerHTML = '';

    if (!query) {
        messageDiv.textContent = 'Vui lòng nhập số điện thoại hoặc biển số xe.';
        messageDiv.classList.add('error-message');
        messageDiv.style.display = 'block';
        return;
    }

    try {
        const repairHistoryResponsePromise = fetch(`/api/repair-history/search?query=${encodeURIComponent(query)}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const maintenanceStatusResponsePromise = fetch(`/api/repair-history/maintenance-status?query=${encodeURIComponent(query)}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const [repairHistoryResponse, maintenanceStatusResponse] = await Promise.all([
            repairHistoryResponsePromise,
            maintenanceStatusResponsePromise
        ]);

        const repairHistoryData = await repairHistoryResponse.json();
        const maintenanceStatusData = await maintenanceStatusResponse.json();

        let isRepairHistorySuccess = repairHistoryResponse.ok;
        let isMaintenanceStatusSuccess = maintenanceStatusResponse.ok;

        if (isRepairHistorySuccess && isMaintenanceStatusSuccess) {
            if ((repairHistoryData.data && repairHistoryData.data.length > 0) || (maintenanceStatusData.data && maintenanceStatusData.data.length > 0)) {
                messageDiv.textContent = "Tra cứu thành công!";
                messageDiv.classList.add('success-message');
                messageDiv.style.display = 'block';

                displayAllResults(repairHistoryData.data, maintenanceStatusData.data);
                resultsContainer.style.display = 'block';
            } else {
                messageDiv.textContent = 'Không tìm thấy thông tin phù hợp với yêu cầu của bạn.';
                messageDiv.classList.add('error-message');
                messageDiv.style.display = 'block';
                if (dynamicResultsContent) dynamicResultsContent.innerHTML = '<p class="no-results">Không tìm thấy lịch sử sửa chữa hoặc trạng thái bảo trì nào.</p>';
                resultsContainer.style.display = 'block';
            }
        } else {
            let errorMessageParts = [];
            errorMessageParts.push("Có vấn đề xảy ra trong quá trình tra cứu. Vui lòng kiểm tra console để biết chi tiết.");

            if (!isRepairHistorySuccess) {
                if (repairHistoryData.message) {
                    errorMessageParts.push("Lịch sử: " + repairHistoryData.message);
                } else if (repairHistoryData.error && repairHistoryData.error.message) {
                    errorMessageParts.push("Lịch sử: " + repairHistoryData.error.message);
                } else {
                    errorMessageParts.push("Lịch sử: Lỗi không xác định (" + repairHistoryResponse.status + ")");
                }
            }

            if (!isMaintenanceStatusSuccess) {
                if (maintenanceStatusData.message) {
                    errorMessageParts.push("Bảo trì: " + maintenanceStatusData.message);
                } else if (maintenanceStatusData.error && maintenanceStatusData.error.message) {
                    errorMessageParts.push("Bảo trì: " + maintenanceStatusData.error.message);
                } else {
                    errorMessageParts.push("Bảo trì: Lỗi không xác định (" + maintenanceStatusResponse.status + ")");
                }
            }

            messageDiv.textContent = errorMessageParts.join(" ");
            messageDiv.classList.add('error-message');
            messageDiv.style.display = 'block';
            if (dynamicResultsContent) dynamicResultsContent.innerHTML = '';
            console.error('Lỗi tra cứu lịch sử:', repairHistoryData);
            console.error('Lỗi tra cứu trạng thái bảo trì:', maintenanceStatusData);
        }
    } catch (error) {
        console.error('Lỗi khi gửi yêu cầu tra cứu:', error);
        messageDiv.textContent = 'Có lỗi xảy ra trong quá trình kết nối hoặc xử lý dữ liệu. Vui lòng thử lại sau.';
        messageDiv.classList.add('error-message');
        messageDiv.style.display = 'block';
    }
});

function displayAllResults(repairHistoryData, maintenanceStatusData) {
    const dynamicResultsContent = document.getElementById('dynamicResultsContent');
    if (!dynamicResultsContent) {
        console.error("Lỗi: Không tìm thấy phần tử 'dynamicResultsContent'. Vui lòng kiểm tra HTML.");
        return;
    }

    dynamicResultsContent.innerHTML = '';

    const combinedData = {};

    if (repairHistoryData && repairHistoryData.length > 0) {
        repairHistoryData.forEach(item => {
            if (!combinedData[item.vehicle.licensePlate]) {
                combinedData[item.vehicle.licensePlate] = {
                    vehicle: item.vehicle,
                    repairHistories: [],
                    maintenanceStatus: null
                };
            }
            combinedData[item.vehicle.licensePlate].repairHistories = item.repairHistories;
        });
    }

    if (maintenanceStatusData && maintenanceStatusData.length > 0) {
        maintenanceStatusData.forEach(item => {
            if (!combinedData[item.vehicle.licensePlate]) {
                combinedData[item.vehicle.licensePlate] = {
                    vehicle: item.vehicle,
                    repairHistories: [],
                    maintenanceStatus: null
                };
            }
            combinedData[item.vehicle.licensePlate].maintenanceStatus = item;
        });
    }

    const vehicleLicensePlates = Object.keys(combinedData);

    if (vehicleLicensePlates.length === 0) {
        const noResultsContent = document.createElement('p');
        noResultsContent.classList.add('no-results');
        noResultsContent.textContent = 'Không tìm thấy lịch sử sửa chữa hoặc trạng thái bảo trì nào cho thông tin bạn đã nhập.';
        dynamicResultsContent.appendChild(noResultsContent);
        return;
    }

    vehicleLicensePlates.forEach(licensePlate => {
        const item = combinedData[licensePlate];

        const vehicleInfoDiv = document.createElement('div');
        vehicleInfoDiv.classList.add('vehicle-info');
        vehicleInfoDiv.innerHTML = `
            <h2>Thông tin xe: ${item.vehicle.licensePlate}</h2>
            <p><strong>Hãng xe:</strong> ${item.vehicle.brand || 'N/A'}</p>
            <p><strong>Dòng xe:</strong> ${item.vehicle.model || 'N/A'}</p>
            <p><strong>Năm sản xuất:</strong> ${item.vehicle.year || 'N/A'}</p>
        `;
        dynamicResultsContent.appendChild(vehicleInfoDiv);

        if (item.maintenanceStatus) {
            const statusClass = item.maintenanceStatus.status.toLowerCase().replace(/\s/g, '-');
            const maintenanceStatusDiv = document.createElement('div');
            maintenanceStatusDiv.classList.add('maintenance-status');
            maintenanceStatusDiv.innerHTML = `
                <h3>Trạng thái bảo trì:</h3>
                <ul>
                    <li><strong>Ngày bảo dưỡng gần nhất:</strong> ${formatDate(item.maintenanceStatus.lastMaintenanceDate) || 'Chưa có lịch sử'}</li>
                    <li><strong>Ngày bảo dưỡng tiếp theo dự kiến:</strong> ${formatDate(item.maintenanceStatus.nextRecommendedMaintenanceDate) || 'N/A'}</li>
                    <li><strong>Chu kỳ bảo dưỡng mặc định:</strong> ${item.maintenanceStatus.assumedMaintenanceIntervalMonths || 'N/A'} tháng</li>
                    <li><strong>Tình trạng:</strong> <span class="status-${statusClass}">${item.maintenanceStatus.status || 'N/A'}</span></li>
                    <li><strong>Ghi chú:</strong> ${item.maintenanceStatus.notes || 'N/A'}</li>
                </ul>
            `;
            dynamicResultsContent.appendChild(maintenanceStatusDiv);
        } else {
            const noMaintenanceStatusDiv = document.createElement('div');
            noMaintenanceStatusDiv.classList.add('no-results');
            noMaintenanceStatusDiv.textContent = 'Không tìm thấy trạng thái bảo trì cho xe này.';
            dynamicResultsContent.appendChild(noMaintenanceStatusDiv);
        }

        if (item.repairHistories && item.repairHistories.length > 0) {
            const repairHistoryListDiv = document.createElement('div');
            repairHistoryListDiv.innerHTML = '<h3>Lịch sử sửa chữa chi tiết:</h3>';
            item.repairHistories.forEach(history => {
                const historyItemDiv = document.createElement('div');
                historyItemDiv.classList.add('repair-history-item');

                const renderStars = (rating) => {
                    if (rating === null || rating === undefined) {
                        return '';
                    }
                    let stars = '';
                    for (let i = 0; i < rating; i++) {
                        stars += '★';
                    }
                    for (let i = rating; i < 5; i++) {
                        stars += '☆';
                    }
                    return `<span class="rating-stars">${stars}</span>`;
                };

                historyItemDiv.innerHTML = `
                    <h4>Ngày sửa: ${formatDateTime(history.sessionDate) || 'N/A'}</h4>
                    <div class="repair-details">
                        <ul>
                            <li><strong>Kỹ thuật viên:</strong> ${history.technicianFullName || 'N/A'}</li>
                            <li><strong>Tổng chi phí:</strong> ${formatCurrency(history.totalCost) || 'N/A'}</li>
                            <li><strong>Ghi chú kỹ thuật viên:</strong> ${history.technicianNotes || 'Không có'}</li>
                        </ul>
                        <h5>Dịch vụ đã thực hiện:</h5>
                        <ul>
                            ${history.servicesPerformed && history.servicesPerformed.length > 0 ?
                    history.servicesPerformed.map(s => `<li>- ${s.serviceName} (${formatCurrency(s.serviceCost)})</li>`).join('') :
                    '<li>Không có dịch vụ nào.</li>'
                }
                        </ul>
                        <h5>Phụ tùng thay thế:</h5>
                        <ul>
                            ${history.partsUsed && history.partsUsed.length > 0 ?
                    history.partsUsed.map(p => `<li>- ${p.partName} (SL: ${p.quantity}, Đơn giá: ${formatCurrency(p.unitPrice)})</li>`).join('') :
                    '<li>Không có phụ tùng nào.</li>'
                }
                        </ul>
                        <div class="review-section" id="review-section-${history.id}">
                            ${history.customerRating !== null && history.customerRating !== undefined ? `
                                <h5>Đánh giá của khách hàng:</h5>
                                <ul>
                                    <li><strong>Người đánh giá:</strong> ${history.reviewerFullName || 'Ẩn danh'}</li>
                                    <li><strong>Điểm:</strong> ${renderStars(history.customerRating)}</li>
                                    <li><strong>Bình luận:</strong> ${history.customerComment || 'Không có bình luận.'}</li>
                                </ul>
                                <div class="review-actions">
                                ${currentUserInfo && currentUserInfo.customerId === history.customerId ? `
                                    <button class="edit-review-btn"
                                            data-session-id="${history.id}"
                                            data-review-id="${history.reviewId}"
                                            data-rating="${history.customerRating}"
                                            data-comment="${history.customerComment}">Chỉnh sửa đánh giá</button>
                                    <button class="export-pdf-btn" data-session-id="${history.id}">Xuất PDF</button>
                                ` : `
                                    <button class="export-pdf-btn" data-session-id="${history.id}">Xuất PDF</button>
                                `}
                                </div>
                            ` : `
                                <p>Chưa có đánh giá cho phiên sửa chữa này.</p>
                                <div class="review-actions">
                                ${currentUserInfo && currentUserInfo.role === 'customer' ? `
                                    <button class="add-review-btn" data-session-id="${history.id}">Thêm đánh giá</button>
                                    <button class="export-pdf-btn" data-session-id="${history.id}">Xuất PDF</button>
                                ` : `
                                    <button class="export-pdf-btn" data-session-id="${history.id}">Xuất PDF</button>
                                `}
                                </div>
                            `}
                        </div>
                    </div>
                `;
                repairHistoryListDiv.appendChild(historyItemDiv);
            });
            dynamicResultsContent.appendChild(repairHistoryListDiv);
            addReviewButtonListeners();
            addExportPdfButtonListeners();
        } else {
            const noHistoryDiv = document.createElement('div');
            noHistoryDiv.classList.add('no-results');
            noHistoryDiv.textContent = 'Không có lịch sử sửa chữa nào cho xe này.';
            dynamicResultsContent.appendChild(noHistoryDiv);
        }
    });
}

function addReviewButtonListeners() {
    document.querySelectorAll('.add-review-btn').forEach(button => {
        button.onclick = (e) => {
            const sessionId = e.target.dataset.sessionId;
            showReviewForm(sessionId, null, null, null);
        };
    });

    document.querySelectorAll('.edit-review-btn').forEach(button => {
        button.onclick = (e) => {
            const sessionId = e.target.dataset.sessionId;
            const reviewId = e.target.dataset.reviewId;
            const rating = parseInt(e.target.dataset.rating);
            const comment = e.target.dataset.comment;
            showReviewForm(sessionId, reviewId, rating, comment);
        };
    });
}

function addExportPdfButtonListeners() {
    document.querySelectorAll('.export-pdf-btn').forEach(button => {
        button.onclick = (e) => {
            const sessionId = e.target.dataset.sessionId;
            handleExportPdfBySession(sessionId);
        };
    });
}

function showReviewForm(serviceSessionId, reviewId, currentRating, currentComment) {
    const reviewSection = document.getElementById(`review-section-${serviceSessionId}`);
    if (!reviewSection) return;

    Array.from(reviewSection.children).forEach(child => {
        if (!child.classList.contains('review-form-container')) {
            child.style.display = 'none';
        }
    });

    let formContainer = reviewSection.querySelector('.review-form-container');
    if (formContainer) {
        formContainer.remove();
    }

    formContainer = document.createElement('div');
    formContainer.classList.add('review-form-container');
    formContainer.innerHTML = `
        <h5>${reviewId ? 'Chỉnh sửa đánh giá' : 'Thêm đánh giá mới'}:</h5>
        <input type="hidden" id="review-form-session-id" value="${serviceSessionId}">
        <input type="hidden" id="review-form-review-id" value="${reviewId || ''}">
        <label for="review-rating">Điểm đánh giá (1-5):</label>
        <input type="number" id="review-rating" min="1" max="5" value="${currentRating || 5}" required>
        <label for="review-comment">Bình luận (tối đa 500 ký tự):</label>
        <textarea id="review-comment" rows="4" maxlength="500">${currentComment || ''}</textarea>
        <button id="submit-review-btn">Gửi đánh giá</button>
        <button id="cancel-review-btn" class="cancel-btn">Hủy</button>
    `;
    reviewSection.appendChild(formContainer);

    document.getElementById('submit-review-btn').onclick = () => submitReview(serviceSessionId);
    document.getElementById('cancel-review-btn').onclick = () => hideReviewForm(serviceSessionId);
}

function hideReviewForm(serviceSessionId) {
    const reviewSection = document.getElementById(`review-section-${serviceSessionId}`);
    if (!reviewSection) return;

    const formContainer = reviewSection.querySelector('.review-form-container');
    if (formContainer) {
        formContainer.remove();
    }

    Array.from(reviewSection.children).forEach(child => {
        if (!child.classList.contains('review-form-container')) {
            child.style.display = '';
        }
    });
}

async function submitReview(serviceSessionId) {
    const messageDiv = document.getElementById('message');
    messageDiv.style.display = 'none';

    const rating = document.getElementById('review-rating').value;
    const comment = document.getElementById('review-comment').value;
    const reviewId = document.getElementById('review-form-review-id').value;

    if (!rating || rating < 1 || rating > 5) {
        messageDiv.textContent = 'Vui lòng nhập điểm đánh giá từ 1 đến 5.';
        messageDiv.classList.add('error-message');
        messageDiv.style.display = 'block';
        return;
    }

    const payload = {
        serviceSessionId: parseInt(serviceSessionId),
        rating: parseInt(rating),
        comment: comment.trim(),
        customerId: currentUserInfo.customerId
    };

    if (reviewId) {
        payload.id = parseInt(reviewId);
    }

    try {
        const response = await fetch('/api/reviews', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        const result = await response.json();

        if (response.ok && result.success) {
            messageDiv.textContent = result.message;
            messageDiv.classList.remove('error-message');
            messageDiv.classList.add('success-message');
            messageDiv.style.display = 'block';

            const query = document.getElementById('query').value;
            localStorage.setItem('lastQuery', query);
            location.reload();
        } else {
            messageDiv.textContent = result.message || 'Lỗi khi gửi đánh giá.';
            messageDiv.classList.remove('success-message');
            messageDiv.classList.add('error-message');
            messageDiv.style.display = 'block';
            console.error('Error submitting review:', result);
        }
    } catch (error) {
        console.error('Network error submitting review:', error);
        messageDiv.textContent = 'Có lỗi mạng khi gửi đánh giá. Vui lòng thử lại.';
        messageDiv.classList.add('error-message');
        messageDiv.style.display = 'block';
    }
}

async function handleExportPdfBySession(serviceSessionId) {
    const messageDiv = document.getElementById('message');

    messageDiv.textContent = 'Đang tạo báo cáo PDF... Vui lòng đợi.';
    messageDiv.classList.remove('error-message');
    messageDiv.classList.add('info-message');
    messageDiv.style.display = 'block';

    try {
        const response = await fetch(`/api/repair-history/pdf-by-session?serviceSessionId=${encodeURIComponent(serviceSessionId)}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.style.display = 'none';
            a.href = url;

            const contentDisposition = response.headers.get('Content-Disposition');
            let filename = `repair_history_${serviceSessionId}.pdf`;
            if (contentDisposition && contentDisposition.indexOf('attachment') !== -1) {
                const filenameMatch = contentDisposition.match(/filename="?([^"]*)"?/);
                if (filenameMatch && filenameMatch[1]) {
                    filename = filenameMatch[1];
                }
            }
            a.download = filename;

            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);

            messageDiv.textContent = 'Đã tạo và tải xuống báo cáo PDF thành công!';
            messageDiv.classList.remove('info-message', 'error-message');
            messageDiv.classList.add('success-message');
        } else {
            const errorData = await response.json();
            messageDiv.textContent = errorData.message || `Lỗi khi xuất PDF: ${response.statusText}`;
            messageDiv.classList.remove('info-message', 'success-message');
            messageDiv.classList.add('error-message');
            console.error('Lỗi khi xuất PDF:', response.status, errorData);
        }
    } catch (error) {
        console.error('Lỗi mạng hoặc xử lý khi xuất PDF:', error);
        messageDiv.textContent = 'Có lỗi xảy ra khi yêu cầu xuất PDF. Vui lòng thử lại.';
        messageDiv.classList.remove('info-message', 'success-message');
        messageDiv.classList.add('error-message');
    }

    setTimeout(() => {
        messageDiv.style.display = 'none';
    }, 5000);
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

function formatDate(isoString) {
    if (!isoString) return '';
    const date = new Date(isoString);
    return date.toLocaleDateString('vi-VN');
}