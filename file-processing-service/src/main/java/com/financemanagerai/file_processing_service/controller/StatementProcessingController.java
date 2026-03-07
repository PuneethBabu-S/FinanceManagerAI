package com.financemanagerai.file_processing_service.controller;

import com.financemanagerai.file_processing_service.dto.StatementProcessingResponseDTO;
import com.financemanagerai.file_processing_service.service.StatementProcessingService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/statements")
public class StatementProcessingController {

    private final StatementProcessingService statementProcessingService;

    public StatementProcessingController(StatementProcessingService statementProcessingService) {
        this.statementProcessingService = statementProcessingService;
    }

    private String getRequester(Authentication authentication) {
        return authentication.getName();
    }

    /**
     * Extract the JWT token from authentication to pass to downstream services
     */
    private String getAuthToken(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getTokenValue();
        }
        // Fallback for other authentication types
        return authentication.getCredentials() != null ? authentication.getCredentials().toString() : "";
    }

    /**
     * Upload and process a bank statement file (CSV or Excel)
     * Extracts transactions with headers, categorizes them using AI, and creates expenses
     *
     * Supported formats: .csv, .xlsx, .xls
     * The first row is expected to contain column headers
     */
    @PostMapping("/upload")
    public StatementProcessingResponseDTO uploadStatement(
            @RequestParam("file") MultipartFile file, Authentication authentication) {

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
        return statementProcessingService.processStatement(file, getRequester(authentication), getAuthToken(authentication));
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

