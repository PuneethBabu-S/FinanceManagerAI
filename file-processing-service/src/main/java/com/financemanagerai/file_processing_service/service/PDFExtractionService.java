package com.financemanagerai.file_processing_service.service;

import com.financemanagerai.file_processing_service.dto.ExtractedTransactionDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PDFExtractionService {

    // Pattern to match transaction lines from bank statements
    // Matches: Date, Description, Amount patterns
    private static final Pattern TRANSACTION_PATTERN = Pattern.compile(
            "^(\\d{2}[-/]\\d{2}[-/]\\d{2,4})\\s+(.+?)\\s+(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)\\s*$",
            Pattern.MULTILINE
    );

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("MM-dd-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ofPattern("dd-MM-yy")
    };

    public List<ExtractedTransactionDTO> extractTransactionsFromPDF(MultipartFile file) throws IOException {
        List<ExtractedTransactionDTO> transactions = new ArrayList<>();

        try (PDDocument document = PDDocument.load(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String pdfText = stripper.getText(document);

            // Extract transactions from text
            String[] lines = pdfText.split("\n");
            for (String line : lines) {
                ExtractedTransactionDTO transaction = parseTransactionLine(line);
                if (transaction != null) {
                    transactions.add(transaction);
                }
            }
        }

        return transactions;
    }

    private ExtractedTransactionDTO parseTransactionLine(String line) {
        // Remove extra whitespaces and trim
        line = line.trim().replaceAll("\\s+", " ");

        if (line.isEmpty()) {
            return null;
        }

        try {
            // Try to match transaction pattern
            Matcher matcher = TRANSACTION_PATTERN.matcher(line);
            if (matcher.find()) {
                String dateStr = matcher.group(1);
                String description = matcher.group(2).trim();
                String amountStr = matcher.group(3).replaceAll(",", "");

                LocalDate date = parseDate(dateStr);
                Double amount = Double.parseDouble(amountStr);

                return ExtractedTransactionDTO.builder()
                        .date(date)
                        .amount(amount)
                        .description(description)
                        .merchant(extractMerchantName(description))
                        .paymentMethod("CARD") // default, can be enhanced
                        .currency("INR") // default, can be enhanced
                        .build();
            }
        } catch (Exception e) {
            // Skip lines that don't match expected format
            return null;
        }

        return null;
    }

    private LocalDate parseDate(String dateStr) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate date = LocalDate.parse(dateStr, formatter);
                // Handle year overflow (if parsed year is in 2000s but should be recent)
                if (date.getYear() > 2100) {
                    date = date.minusYears(100);
                }
                return date;
            } catch (Exception e) {
                // Try next formatter
            }
        }
        throw new RuntimeException("Unable to parse date: " + dateStr);
    }

    private String extractMerchantName(String description) {
        // Extract first meaningful word as merchant name
        String[] parts = description.split("\\s+");
        if (parts.length > 0) {
            return parts[0];
        }
        return description;
    }
}


