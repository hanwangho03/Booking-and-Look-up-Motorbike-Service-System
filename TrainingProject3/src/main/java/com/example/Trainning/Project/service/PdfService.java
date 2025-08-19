package com.example.Trainning.Project.service;

import com.example.Trainning.Project.dto.repair.PartUsedDTO;
import com.example.Trainning.Project.dto.repair.RepairHistoryDTO;
import com.example.Trainning.Project.dto.repair.ServicePerformedDTO;
import com.example.Trainning.Project.dto.repair.VehicleRepairHistoryResponse;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

@Service
public class PdfService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    // Đường dẫn tới file font trong thư mục resources
    public static final String FONT_PATH = "fonts/DejaVuSans.ttf";

    public byte[] generateRepairHistoryPdf(List<VehicleRepairHistoryResponse> repairHistories) throws DocumentException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, baos);

        document.open();


        BaseFont baseFont = BaseFont.createFont(FONT_PATH, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);

        // Tạo các kiểu font khác nhau từ BaseFont
        Font titleFont = new Font(baseFont, 24, Font.BOLD);
        Font subTitleFont = new Font(baseFont, 16, Font.BOLD);
        Font itemTitleFont = new Font(baseFont, 12, Font.BOLD);
        Font normalFont = new Font(baseFont, 10);
        Font normalItalicFont = new Font(baseFont, 10, Font.ITALIC);
        Font boldFont = new Font(baseFont, 11, Font.BOLD);
        Font tableHeaderFont = new Font(baseFont, 9, Font.BOLD);
        Font tableCellFont = new Font(baseFont, 9);
        Font separatorFont = new Font(baseFont, 8);


        Paragraph title = new Paragraph("Báo Cáo Lịch Sử Sửa Chữa Xe", titleFont);
        title.setAlignment(Paragraph.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        if (repairHistories.isEmpty()) {
            Paragraph noData = new Paragraph("Không có dữ liệu lịch sử sửa chữa để xuất.", normalFont);
            noData.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(noData);
        } else {
            for (VehicleRepairHistoryResponse vehicleHistory : repairHistories) {
                Paragraph vehicleInfoTitle = new Paragraph("Thông tin xe: " + vehicleHistory.getVehicle().getLicensePlate() + " - " + vehicleHistory.getVehicle().getBrand() + " " + vehicleHistory.getVehicle().getModel(), subTitleFont);
                vehicleInfoTitle.setSpacingBefore(15);
                vehicleInfoTitle.setSpacingAfter(10);
                document.add(vehicleInfoTitle);

                if (vehicleHistory.getRepairHistories().isEmpty()) {
                    Paragraph noRepair = new Paragraph("Xe này chưa có lịch sử sửa chữa.", normalItalicFont);
                    noRepair.setSpacingAfter(10);
                    document.add(noRepair);
                } else {
                    for (RepairHistoryDTO repair : vehicleHistory.getRepairHistories()) {
                        document.add(new Paragraph("---", separatorFont));

                        Paragraph repairSessionTitle = new Paragraph("Phiên sửa chữa: " + repair.getSessionDate().format(DATE_TIME_FORMATTER), itemTitleFont);
                        repairSessionTitle.setSpacingBefore(5);
                        repairSessionTitle.setSpacingAfter(5);
                        document.add(repairSessionTitle);

                        document.add(new Paragraph("Tổng chi phí: " + String.format("%,.0f VNĐ", repair.getTotalCost()), normalFont));
                        document.add(new Paragraph("Ghi chú kỹ thuật viên: " + repair.getTechnicianNotes(), normalFont));
                        document.add(new Paragraph("Kỹ thuật viên: " + repair.getTechnicianFullName(), normalFont));
                        document.add(new Paragraph("Đánh giá khách hàng: " + (repair.getCustomerRating() != null ? repair.getCustomerRating() + " sao" : "Chưa đánh giá"), normalFont));
                        document.add(new Paragraph("Bình luận khách hàng: " + (repair.getCustomerComment() != null ? repair.getCustomerComment() : "Không có"), normalFont));
                        document.add(new Paragraph("Người đánh giá: " + (repair.getReviewerFullName() != null ? repair.getReviewerFullName() : "Không rõ"), normalFont));

                        if (!repair.getServicesPerformed().isEmpty()) {
                            document.add(new Paragraph("Dịch vụ đã thực hiện:", boldFont));
                            PdfPTable servicesTable = new PdfPTable(2);
                            servicesTable.setWidthPercentage(90);
                            servicesTable.setSpacingBefore(5);
                            servicesTable.setSpacingAfter(5);

                            Stream.of("Tên Dịch Vụ", "Chi Phí").forEach(headerTitle -> {
                                PdfPCell header = new PdfPCell();
                                header.setBackgroundColor(Color.LIGHT_GRAY);
                                header.setBorderWidth(1);
                                header.setPhrase(new Phrase(headerTitle, tableHeaderFont));
                                servicesTable.addCell(header);
                            });

                            for (ServicePerformedDTO service : repair.getServicesPerformed()) {
                                servicesTable.addCell(new Phrase(service.getServiceName(), tableCellFont));
                                servicesTable.addCell(new Phrase(String.format("%,.0f VNĐ", service.getServiceCost()), tableCellFont));
                            }
                            document.add(servicesTable);
                        }

                        if (!repair.getPartsUsed().isEmpty()) {
                            document.add(new Paragraph("Phụ tùng đã sử dụng:", boldFont));
                            PdfPTable partsTable = new PdfPTable(3);
                            partsTable.setWidthPercentage(90);
                            partsTable.setSpacingBefore(5);
                            partsTable.setSpacingAfter(5);

                            Stream.of("Tên Phụ Tùng", "Số Lượng", "Đơn Giá").forEach(headerTitle -> {
                                PdfPCell header = new PdfPCell();
                                header.setBackgroundColor(Color.LIGHT_GRAY);
                                header.setBorderWidth(1);
                                header.setPhrase(new Phrase(headerTitle, tableHeaderFont));
                                partsTable.addCell(header);
                            });

                            for (PartUsedDTO part : repair.getPartsUsed()) {
                                partsTable.addCell(new Phrase(part.getPartName(), tableCellFont));
                                partsTable.addCell(new Phrase(String.valueOf(part.getQuantity()), tableCellFont));
                                partsTable.addCell(new Phrase(String.format("%,.0f VNĐ", part.getUnitPrice()), tableCellFont));
                            }
                            document.add(partsTable);
                        }
                    }
                }
                document.add(new Paragraph("\n"));
            }
        }

        document.close();
        return baos.toByteArray();
    }
}