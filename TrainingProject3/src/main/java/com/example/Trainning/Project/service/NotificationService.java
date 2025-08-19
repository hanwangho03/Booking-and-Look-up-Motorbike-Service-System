package com.example.Trainning.Project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Gửi một email nhắc nhở bảo trì cho khách hàng.
     *
     * @param toEmail Địa chỉ email của người nhận.
     * @param customerName Tên khách hàng (để cá nhân hóa).
     * @param licensePlate Biển số xe cần nhắc nhở.
     * @param lastMaintenanceDate Ngày bảo dưỡng gần nhất.
     * @param nextRecommendedMaintenanceDate Ngày bảo dưỡng tiếp theo dự kiến.
     * @param status Trạng thái bảo trì (Overdue, Due Soon, etc.).
     * @param notes Ghi chú bổ sung về trạng thái.
     */
    public void sendMaintenanceReminderEmail(String toEmail, String customerName, String licensePlate,
                                             String lastMaintenanceDate, String nextRecommendedMaintenanceDate,
                                             String status, String notes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Nhắc nhở bảo dưỡng xe của bạn - " + licensePlate);

        // Xây dựng nội dung email
        StringBuilder emailBody = new StringBuilder();
        emailBody.append("Kính gửi ").append(customerName).append(",\n\n");
        emailBody.append("Chúng tôi gửi email này để nhắc nhở về tình trạng bảo dưỡng của chiếc xe ");
        emailBody.append(licensePlate).append(" của bạn.\n\n");

        emailBody.append("Chi tiết bảo dưỡng:\n");
        emailBody.append("- Biển số xe: ").append(licensePlate).append("\n");
        emailBody.append("- Ngày bảo dưỡng gần nhất: ").append(lastMaintenanceDate).append("\n");
        emailBody.append("- Ngày bảo dưỡng tiếp theo dự kiến: ").append(nextRecommendedMaintenanceDate).append("\n");
        emailBody.append("- Tình trạng hiện tại: ").append(status).append("\n");
        emailBody.append("- Ghi chú: ").append(notes).append("\n\n");

        if ("Overdue".equals(status)) {
            emailBody.append("Xe của bạn đã quá hạn bảo dưỡng. Vui lòng liên hệ với chúng tôi để sắp xếp lịch hẹn sớm nhất để đảm bảo xe luôn hoạt động tốt và an toàn.\n");
        } else if ("Due Soon".equals(status)) {
            emailBody.append("Xe của bạn sắp đến hạn bảo dưỡng. Vui lòng xem xét đặt lịch hẹn trong thời gian tới để xe được kiểm tra định kỳ.\n");
        } else {
            emailBody.append("Xe của bạn hiện đang có tình trạng bảo dưỡng tốt. Bạn có thể liên hệ khi có nhu cầu.\n");
        }

        emailBody.append("\nTrân trọng,\n");
        emailBody.append("Đội ngũ Dịch vụ bảo dưỡng xe.");

        message.setText(emailBody.toString());

        try {
            mailSender.send(message);
            System.out.println("DEBUG: Email nhắc nhở đã được gửi thành công đến " + toEmail + " cho xe " + licensePlate);
        } catch (MailException e) {
            System.err.println("ERROR: Không thể gửi email nhắc nhở đến " + toEmail + " cho xe " + licensePlate + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}