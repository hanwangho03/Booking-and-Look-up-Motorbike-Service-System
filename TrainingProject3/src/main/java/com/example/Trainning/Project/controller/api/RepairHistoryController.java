package com.example.Trainning.Project.controller.api;

import com.example.Trainning.Project.dto.repair.VehicleMaintenanceStatusDTO;
import com.example.Trainning.Project.dto.repair.VehicleRepairHistoryResponse;
import com.example.Trainning.Project.dto.response.ApiError;
import com.example.Trainning.Project.dto.response.ApiResponse;
import com.example.Trainning.Project.service.PdfService;
import com.example.Trainning.Project.service.RepairHistoryService;
import com.lowagie.text.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/repair-history")
public class RepairHistoryController {

    @Autowired
    private RepairHistoryService repairHistoryService;
    @Autowired
    private PdfService pdfService;
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<VehicleRepairHistoryResponse>>> searchRepairHistory(
            @RequestParam String query) {
        try {
            List<VehicleRepairHistoryResponse> results = repairHistoryService.searchRepairHistory(query);

            ApiResponse<List<VehicleRepairHistoryResponse>> apiResponse = new ApiResponse<>(true, "Tra cứu lịch sử sửa chữa thành công!", results, HttpStatus.OK.value());
            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            System.err.println("Lỗi trong quá trình tra cứu lịch sử sửa chữa: " + e.getMessage());
            e.printStackTrace();
            ApiError error = new ApiError("Đã xảy ra lỗi không mong muốn trong quá trình tra cứu lịch sử sửa chữa: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
            ApiResponse<List<VehicleRepairHistoryResponse>> apiResponse = new ApiResponse<>(false, "Tra cứu thất bại.", error, HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }
    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> generateRepairHistoryPdf(@RequestParam("licensePlate") String licensePlate) {
        try {
            List<VehicleRepairHistoryResponse> repairHistories = repairHistoryService.searchRepairHistory(licensePlate);
            byte[] pdfBytes = repairHistoryService.generateRepairHistoryPdfByLicensePlate(licensePlate);
            if (pdfBytes == null || pdfBytes.length == 0) {
                throw new RuntimeException("Không thể tạo file PDF: File rỗng hoặc lỗi xử lý.");
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=repair_history_" + licensePlate + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (IllegalArgumentException e) {
            ApiError error = new ApiError("Không tìm thấy xe với biển số: " + licensePlate, HttpStatus.NOT_FOUND.value());
            ApiResponse<String> apiResponse = new ApiResponse<>(false, "Tạo PDF thất bại.", error, HttpStatus.NOT_FOUND.value());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(apiResponse);
        } catch (Exception e) {
            System.err.println("Lỗi khi tạo PDF: " + e.getMessage());
            e.printStackTrace();
            ApiError error = new ApiError("Lỗi khi tạo file PDF: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
            ApiResponse<String> apiResponse = new ApiResponse<>(false, "Tạo PDF thất bại.", error, HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(apiResponse);
        }
    }

    @GetMapping(value = "/pdf-by-session", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> generateRepairHistoryPdfBySession(@RequestParam("serviceSessionId") Long serviceSessionId) {
        try {
            VehicleRepairHistoryResponse repairHistory = repairHistoryService.getRepairHistoryBySessionId(serviceSessionId);
            byte[] pdfBytes = repairHistoryService.generateRepairHistoryPdfBySessionId(serviceSessionId);
            if (pdfBytes == null || pdfBytes.length == 0) {
                throw new RuntimeException("Không thể tạo file PDF: File rỗng hoặc lỗi xử lý.");
            }

            String licensePlate = repairHistory.getVehicle().getLicensePlate();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=repair_session_" + serviceSessionId + "_" + licensePlate + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (IllegalArgumentException e) {
            ApiError error = new ApiError("Không tìm thấy phiên sửa chữa với ID: " + serviceSessionId, HttpStatus.NOT_FOUND.value());
            ApiResponse<String> apiResponse = new ApiResponse<>(false, "Tạo PDF thất bại.", error, HttpStatus.NOT_FOUND.value());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(apiResponse);
        } catch (Exception e) {
            System.err.println("Lỗi khi tạo PDF cho phiên sửa chữa: " + e.getMessage());
            e.printStackTrace();
            ApiError error = new ApiError("Lỗi khi tạo file PDF: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
            ApiResponse<String> apiResponse = new ApiResponse<>(false, "Tạo PDF thất bại.", error, HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(apiResponse);
        }
    }

    @GetMapping("/maintenance-status")
    public ResponseEntity<ApiResponse<List<VehicleMaintenanceStatusDTO>>> getMaintenanceStatus(
            @RequestParam String query) {
        try {
            List<VehicleMaintenanceStatusDTO> result = repairHistoryService.checkMaintenanceStatus(query);
            return ResponseEntity.ok(new ApiResponse<>(true, "Tra cứu trạng thái bảo trì thành công!", result, HttpStatus.OK.value()));
        } catch (Exception e) {
            System.err.println("ERROR: Lỗi khi tra cứu trạng thái bảo trì: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(false, "Lỗi khi tra cứu trạng thái bảo trì: " + e.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}