package com.example.TrainingProject2.service;

import com.example.TrainingProject2.model.Appointment;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    public CompletableFuture<Void> sendAppointmentStatusEmail(Appointment appointment, String subject, String templateName, String cancellationReason) { // BỔ SUNG THAM SỐ NÀY
        return CompletableFuture.runAsync(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                String customerEmail = appointment.getCustomer().getEmail();
                if (customerEmail == null || customerEmail.trim().isEmpty()) {
                    logger.warn("Customer email is empty for appointment ID {}. Skipping email sending.", appointment.getId());
                    return;
                }
                helper.setTo(customerEmail);
                helper.setSubject(subject);
                helper.setFrom("anhthuan060420033@gmail.com");
                // TRUYỀN THÊM LÝ DO VÀO generateEmailContent
                String htmlContent = generateEmailContent(appointment, templateName, cancellationReason);
                helper.setText(htmlContent, true);
                mailSender.send(message);
                logger.info("Email sent successfully for appointment ID {}.", appointment.getId());

            } catch (MessagingException e) {
                logger.error("Error creating MIME message for appointment ID {}: {}", appointment.getId(), e.getMessage());
            } catch (MailException e) {
                logger.error("Error sending email to {} for appointment ID {}: {}", appointment.getCustomer().getEmail(), appointment.getId(), e.getMessage());
            } catch (Exception e) {
                logger.error("An unexpected error occurred while sending email for appointment ID {}: {}", appointment.getId(), e.getMessage());
            }
        });
    }

    private String generateEmailContent(Appointment appointment, String templateName, String cancellationReason) { // BỔ SUNG THAM SỐ NÀY
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

        String customerName = appointment.getCustomer().getName();
        String serviceName = appointment.getService().getName();
        String startTime = appointment.getStartTime().format(dateFormatter);
        String technicianName = appointment.getTechnician().getName();

        String statusVietnamese = mapStatusToVietnamese(appointment.getStatus());

        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("<!DOCTYPE html>");
        contentBuilder.append("<html lang='vi'>");
        contentBuilder.append("<head>");
        contentBuilder.append("<meta charset='UTF-8'>");
        contentBuilder.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        contentBuilder.append("<title>Thông báo Lịch hẹn</title>");
        contentBuilder.append("</head>");
        contentBuilder.append("<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333; background-color: #f4f4f4; margin: 0; padding: 20px;'>");

        contentBuilder.append("<div style='max-width: 600px; margin: 20px auto; padding: 20px; background-color: #ffffff; border-radius: 8px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);'>");

        // Header
        contentBuilder.append("<div style='text-align: center; padding-bottom: 20px; border-bottom: 1px solid #eee;'>");
        contentBuilder.append("<h1 style='color: #007bff; margin: 0;'>Thông báo Lịch hẹn</h1>");
        contentBuilder.append("</div>");

        // Body Content
        contentBuilder.append("<div style='padding: 20px 0;'>");
        contentBuilder.append("<p>Kính gửi <strong>").append(customerName).append("</strong>,</p>");

        if ("approved".equals(templateName)) {
            contentBuilder.append("<p>Chúng tôi vui mừng thông báo lịch hẹn của quý khách đã <strong style='color: #28a745;'>được phê duyệt</strong> thành công!</p>");
            contentBuilder.append("<p>Dưới đây là chi tiết lịch hẹn của quý khách:</p>");
            contentBuilder.append("<ul style='list-style-type: none; padding: 0;'>");
            contentBuilder.append("<li style='margin-bottom: 10px;'><strong>Dịch vụ:</strong> <span style='color: #007bff;'><strong>").append(serviceName).append("</strong></span></li>");
            contentBuilder.append("<li style='margin-bottom: 10px;'><strong>Thời gian:</strong> <span style='color: #28a745;'><strong>").append(startTime).append("</strong></span></li>");
            contentBuilder.append("<li style='margin-bottom: 10px;'><strong>Kỹ thuật viên:</strong> <strong>").append(technicianName).append("</strong></li>");
            contentBuilder.append("<li style='margin-bottom: 10px;'><strong>Trạng thái:</strong> <span style='color: #28a745; font-weight: bold;'>").append(statusVietnamese).append("</span></li>");
            contentBuilder.append("</ul>");
            contentBuilder.append("<p>Chúng tôi rất mong được phục vụ quý khách tại cơ sở của chúng tôi. Vui lòng đến đúng giờ để trải nghiệm dịch vụ tốt nhất.</p>");
        } else if ("cancelled".equals(templateName)) {
            contentBuilder.append("<p>Chúng tôi rất tiếc phải thông báo rằng lịch hẹn của quý khách cho dịch vụ <strong>").append(serviceName).append("</strong> vào lúc <strong>")
                    .append(startTime).append("</strong> với kỹ thuật viên <strong>").append(technicianName).append("</strong> đã <strong style='color: #dc3545;'>bị hủy</strong>.</p>");

            // BỔ SUNG LÝ DO HỦY VÀO EMAIL
            if (cancellationReason != null && !cancellationReason.trim().isEmpty()) {
                contentBuilder.append("<p><strong>Lý do hủy:</strong> ").append(cancellationReason).append("</p>");
            } else {
                contentBuilder.append("<p>Lý do hủy: Không có lý do cụ thể được cung cấp.</p>");
            }

            contentBuilder.append("<p>Nếu quý khách muốn đặt lại lịch hẹn hoặc có bất kỳ thắc mắc nào, xin vui lòng liên hệ với chúng tôi qua số điện thoại <strong>0356651701</strong> hoặc truy cập trang web <a href='https://tiemsuaxetantam.com' style='color: #007bff; text-decoration: none;'><strong>tiemsuaxetantam.com</strong></a> của chúng tôi.</p>");
        } else {
            contentBuilder.append("<p>Đây là thông báo về trạng thái lịch hẹn của quý khách:</p>");
            contentBuilder.append("<ul style='list-style-type: none; padding: 0;'>");
            contentBuilder.append("<li style='margin-bottom: 10px;'><strong>Dịch vụ:</strong> <span style='color: #007bff;'><strong>").append(serviceName).append("</strong></span></li>");
            contentBuilder.append("<li style='margin-bottom: 10px;'><strong>Thời gian:</strong> <span style='color: #555;'><strong>").append(startTime).append("</strong></span></li>");
            contentBuilder.append("<li style='margin-bottom: 10px;'><strong>Kỹ thuật viên:</strong> <strong>").append(technicianName).append("</strong></li>");
            contentBuilder.append("<li style='margin-bottom: 10px;'><strong>Trạng thái:</strong> <span style='color: #ffc107; font-weight: bold;'>").append(statusVietnamese).append("</span></li>");
            contentBuilder.append("</ul>");
        }

        contentBuilder.append("<p>Trân trọng,<br>Đội ngũ của chúng tôi</p>");
        contentBuilder.append("<p style='font-size: 0.9em; color: #777; margin-top: 20px; text-align: center;'>");
        contentBuilder.append("Đây là email tự động, vui lòng không trả lời email này.");
        contentBuilder.append("</p>");
        contentBuilder.append("</div>");

        // Footer
        contentBuilder.append("<div style='text-align: center; padding-top: 20px; border-top: 1px solid #eee; font-size: 0.8em; color: #999;'>");
        contentBuilder.append("<p>&copy; 2025 <strong>Tiệm Sửa Xe Tận Tâm</strong>. Mọi quyền được bảo lưu.</p>");
        contentBuilder.append("<p><strong>Đường số 6, Dân chủ, Bình Thọ, Thủ Đức</strong> | <strong>0356651701</strong> | <a href='https://tiemsuaxetantam.com' style='color: #007bff; text-decoration: none;'><strong>tiemsuaxetantam.com</strong></a></p>");
        contentBuilder.append("</div>");

        contentBuilder.append("</div>");
        contentBuilder.append("</body></html>");
        return contentBuilder.toString();
    }

    private String mapStatusToVietnamese(Appointment.Status status) {
        return switch (status) {
            case cho_xac_nhan -> "Chờ xác nhận";
            case da_xac_nhan -> "Đã xác nhận";
            case da_huy -> "Đã hủy";
            case da_hoan_thanh -> "Đã hoàn thành";
        };
    }
    public CompletableFuture<Void> sendOtpEmail(String email, String otp){
        return CompletableFuture.runAsync(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                if (email == null || email.trim().isEmpty()) {
                    logger.warn("Recipient email for OTP is empty. Skipping email sending.");
                    return;
                }

                helper.setTo(email);
                helper.setSubject("Mã xác thực OTP của bạn - Tiệm Sửa Xe Tận Tâm");
                helper.setFrom("anhthuan060420033@gmail.com");

                String htmlContent = generateOtpEmailContent(otp);
                helper.setText(htmlContent, true);

                mailSender.send(message);
                logger.info("OTP email sent successfully to {}", email);
            } catch (MessagingException e) {
                logger.error("Error creating MIME message for OTP email to {}: {}", email, e.getMessage(), e);
                throw new RuntimeException("Không thể gửi mã OTP đến email của bạn. Vui lòng thử lại sau.", e);
            } catch (MailException e){
                logger.error("Error sending OTP email to {}: {}", email, e.getMessage(), e);
                throw new RuntimeException("Không thể gửi mã OTP đến email của bạn. Vui lòng thử lại sau.", e);
            } catch (Exception e) {
                logger.error("An unexpected error occurred while sending OTP email to {}: {}", email, e.getMessage(), e);
                throw new RuntimeException("Có lỗi xảy ra khi gửi mã OTP. Vui lòng thử lại.", e);
            }
        });
    }

    // Phương thức mới để tạo nội dung HTML cho email OTP
    private String generateOtpEmailContent(String otp) {
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("<!DOCTYPE html>");
        contentBuilder.append("<html lang='vi'>");
        contentBuilder.append("<head>");
        contentBuilder.append("<meta charset='UTF-8'>");
        contentBuilder.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        contentBuilder.append("<title>Mã Xác Thực OTP</title>");
        contentBuilder.append("</head>");
        contentBuilder.append("<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333; background-color: #f4f4f4; margin: 0; padding: 20px;'>");

        contentBuilder.append("<div style='max-width: 600px; margin: 20px auto; padding: 20px; background-color: #ffffff; border-radius: 8px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);'>");

        // Header
        contentBuilder.append("<div style='text-align: center; padding-bottom: 20px; border-bottom: 1px solid #eee;'>");
        contentBuilder.append("<h1 style='color: #007bff; margin: 0;'>Mã Xác Thực Của Bạn</h1>");
        contentBuilder.append("</div>");

        // Body Content
        contentBuilder.append("<div style='padding: 20px 0;'>");
        contentBuilder.append("<p>Kính gửi quý khách,</p>");
        contentBuilder.append("<p>Đây là mã xác thực OTP của quý khách:</p>");
        contentBuilder.append("<div style='text-align: center; margin: 20px 0;'>");
        contentBuilder.append("<strong style='font-size: 28px; color: #28a745; background-color: #e2f9e4; padding: 10px 20px; border-radius: 5px; letter-spacing: 2px;'>").append(otp).append("</strong>");
        contentBuilder.append("</div>");
        contentBuilder.append("<p>Mã này sẽ hết hạn trong <strong>5 phút</strong>. Vui lòng không chia sẻ mã này với bất kỳ ai.</p>");
        contentBuilder.append("<p>Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email này.</p>");
        contentBuilder.append("<p>Trân trọng,<br>Đội ngũ <strong>Tiệm Sửa Xe Tận Tâm</strong></p>");
        contentBuilder.append("<p style='font-size: 0.9em; color: #777; margin-top: 20px; text-align: center;'>");
        contentBuilder.append("Đây là email tự động, vui lòng không trả lời email này.");
        contentBuilder.append("</p>");
        contentBuilder.append("</div>");

        // Footer
        contentBuilder.append("<div style='text-align: center; padding-top: 20px; border-top: 1px solid #eee; font-size: 0.8em; color: #999;'>");
        contentBuilder.append("<p>&copy; 2025 <strong>Tiệm Sửa Xe Tận Tâm</strong>. Mọi quyền được bảo lưu.</p>");
        contentBuilder.append("<p><strong>Đường số 6, Dân chủ, Bình Thọ, Thủ Đức</strong> | <strong>0356651701</strong> | <a href='https://tiemsuaxetantam.com' style='color: #007bff; text-decoration: none;'><strong>tiemsuaxetantam.com</strong></a></p>");
        contentBuilder.append("</div>");

        contentBuilder.append("</div>");
        contentBuilder.append("</body></html>");
        return contentBuilder.toString();
    }
}
