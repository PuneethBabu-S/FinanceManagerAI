package com.financemanagerai.file_processing_service.service;

import com.financemanagerai.file_processing_service.dto.ExtractedTransactionDTO;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class FileExtractionService {

    /**
     * Extract transactions from CSV file
     * Do NOT treat first row specially; return all rows in order (first row may be headers)
     */
    public List<ExtractedTransactionDTO> extractTransactionsFromCSV(MultipartFile file) throws IOException {
        List<ExtractedTransactionDTO> transactions = new ArrayList<>();

        try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT)) {

            for (CSVRecord record : csvParser) {
                List<String> rowValues = new ArrayList<>();
                for (String value : record) {
                    rowValues.add(value);
                }

                ExtractedTransactionDTO row = ExtractedTransactionDTO.builder()
                        .isHeaderRow(false)
                        .columnHeaders(null)
                        .rowValues(rowValues)
                        .build();
                transactions.add(row);
            }
        }

        return transactions;
    }

    /**
     * Extract transactions from Excel file
     * Do NOT treat first row specially; return all rows in order (first row may be headers)
     */
    public List<ExtractedTransactionDTO> extractTransactionsFromExcel(MultipartFile file) throws IOException {
        List<ExtractedTransactionDTO> transactions = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0); // Get first sheet

            // Determine maximum number of columns across all rows to avoid dropping columns
            int maxCols = 0;
            for (Row row : sheet) {
                if (row == null) continue;
                maxCols = Math.max(maxCols, row.getLastCellNum() <= 0 ? 0 : row.getLastCellNum());
            }

            for (Row row : sheet) {
                if (row == null) continue;

                List<String> rowValues = new ArrayList<>();
                for (int i = 0; i < maxCols; i++) {
                    Cell cell = row.getCell(i);
                    rowValues.add(getCellValueAsString(cell));
                }

                ExtractedTransactionDTO dataRow = ExtractedTransactionDTO.builder()
                        .isHeaderRow(false)
                        .columnHeaders(null)
                        .rowValues(rowValues)
                        .build();
                transactions.add(dataRow);
            }
        }

        return transactions;
    }

    /**
     * Get cell value as string from Excel
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // Avoid scientific notation issues for large integers
                    double d = cell.getNumericCellValue();
                    if (d == Math.floor(d)) {
                        return String.valueOf((long) d);
                    }
                    return String.valueOf(d);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        double d = cell.getNumericCellValue();
                        if (d == Math.floor(d)) {
                            return String.valueOf((long) d);
                        }
                        return String.valueOf(d);
                    } catch (Exception ex) {
                        return cell.getCellFormula();
                    }
                }
            default:
                return "";
        }
    }

    /**
     * Determine file type and extract accordingly
     */
    public List<ExtractedTransactionDTO> extractTransactions(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();

        if (filename == null) {
            throw new IOException("File name is null");
        }

        String lower = filename.toLowerCase();
        if (lower.endsWith(".csv")) {
            return extractTransactionsFromCSV(file);
        } else if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
            return extractTransactionsFromExcel(file);
        } else {
            throw new IOException("Unsupported file format. Please use CSV or Excel (.xlsx/.xls) files");
        }
    }
}
