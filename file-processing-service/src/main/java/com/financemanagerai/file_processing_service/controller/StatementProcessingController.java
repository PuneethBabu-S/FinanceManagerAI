package com.financemanagerai.file_processing_service.controller;

import com.financemanagerai.file_processing_service.dto.StatementProcessingResponseDTO;
import com.financemanagerai.file_processing_service.service.StatementProcessingService;
//import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/statements")
public class StatementProcessingController {

    private final StatementProcessingService statementProcessingService;

    public StatementProcessingController(StatementProcessingService statementProcessingService) {
        this.statementProcessingService = statementProcessingService;
    }

//    private String getRequester(Authentication authentication) {
//        return authentication.getName();
//    }
//
//    private String getAuthToken(Authentication authentication) {
//        // In a real scenario, extract the token from the JWT
//        // For now, return a placeholder
//        return "Bearer " + authentication.getPrincipal().toString();
//    }

    /**
     * Upload and process a bank statement file (CSV or Excel)
     * Extracts transactions with headers, categorizes them using AI, and creates expenses
     *
     * Supported formats: .csv, .xlsx, .xls
     * The first row is expected to contain column headers
     */
    @PostMapping("/upload")
    public StatementProcessingResponseDTO uploadStatement(
            @RequestParam("file") MultipartFile file) {

        // Validate file
        if (file.isEmpty()) {
            return StatementProcessingResponseDTO.builder()
                    .status("FAILED")
                    .message("File is empty")
                    .build();
        }

        if (!isValidFile(file.getOriginalFilename())) {
            return StatementProcessingResponseDTO.builder()
                    .status("FAILED")
                    .message("File must be in CSV or Excel (.xlsx/.xls) format")
                    .build();
        }

        // Process statement
        return statementProcessingService.processStatement(file, "username", "token");
    }

    private boolean isValidFile(String filename) {
        if (filename == null) {
            return false;
        }

        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".csv") ||
               lowerFilename.endsWith(".xlsx") ||
               lowerFilename.endsWith(".xls");
    }
}

