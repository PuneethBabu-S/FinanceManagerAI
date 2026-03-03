# File Processing Service

Part of the **Finance Manager AI** microservices architecture. This service handles PDF bank statement processing, transaction extraction, and AI-powered categorization.

## Overview

The File Processing Service provides a unified pipeline for:
1. **PDF Statement Upload & Extraction** - Parse bank statements and extract transaction details
2. **Bulk Transaction Categorization** - Use GenAI service to categorize transactions in batches
3. **Expense Creation** - Automatically create expenses in the Expense Service with categorized transactions

## Architecture

```
┌─────────────────────────────────────────────────┐
│  PDF Bank Statement                             │
└────────────────┬────────────────────────────────┘
                 │
        ┌────────▼────────┐
        │  PDF Extraction │
        │    (PDFBox)     │
        └────────┬────────┘
                 │
        ┌────────▼──────────────────┐
        │ ExtractedTransactionDTOs  │
        └────────┬──────────────────┘
                 │
        ┌────────▼─────────────────────────┐
        │  Batch Categorization (GenAI)   │
        │  - 20 transactions per batch    │
        │  - AI-powered classification    │
        └────────┬──────────────────────┐
                 │                      │
        ┌────────▼──────────┐   ┌──────▼──────────────┐
        │  Category Mapping │   │  Expense Creation  │
        │  (via GenAI)      │   │  (Expense Service) │
        └───────────────────┘   └────────────────────┘
```

## API Endpoints

### Upload & Process Statement

**POST** `/api/statements/upload`

Uploads a PDF bank statement and processes it end-to-end.

**Request:**
```http
POST /api/statements/upload
Content-Type: multipart/form-data
Authorization: Bearer <JWT_TOKEN>

file: <bank_statement.pdf>
```

**Response:**
```json
{
  "statementId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "totalTransactionsExtracted": 25,
  "totalTransactionsCategorized": 25,
  "totalExpensesCreated": 25,
  "message": "Statement processed successfully",
  "errors": []
}
```

**Status Codes:**
- `200 OK` - Statement processed successfully
- `400 BAD REQUEST` - Invalid PDF or empty file
- `401 UNAUTHORIZED` - Missing or invalid authentication token
- `500 INTERNAL SERVER ERROR` - Processing error

## Dependencies

### Core Dependencies
- **Spring Boot 3.5.10** - Framework
- **Spring Security & OAuth2** - Authentication
- **Spring Data JPA** - Database access
- **PostgreSQL** - Relational database

### Processing Libraries
- **Apache PDFBox 3.0.1** - PDF text extraction
- **org.json** - JSON parsing and manipulation

### Communication
- **Spring WebFlux** - Async HTTP client for inter-service calls
- **RestTemplate** - Synchronous REST client

## Configuration

### Application Properties

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/file_processing_db
    username: postgres
    password: pass
  
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

server:
  port: 8083

# External Services
expense-service:
  url: http://localhost:8082
  
genai-service:
  url: http://localhost:8084
```

## Processing Flow

### 1. PDF Extraction
- Accepts MultipartFile (PDF)
- Uses PDFBox to extract text content
- Parses transaction lines using regex patterns
- Supports multiple date formats (dd/MM/yyyy, MM/dd/yyyy, etc.)
- Returns `List<ExtractedTransactionDTO>`

### 2. Categorization
- Fetches available expense categories from Expense Service
- Batches transactions (20 per batch for efficiency)
- Calls GenAI service with formatted prompt
- Receives category suggestions
- Matches suggestions to available categories
- Returns `List<CategorizedTransactionDTO>`

### 3. Expense Creation
- For each categorized transaction:
  - Resolves category ID from category name
  - Creates `ExpenseRequestDTO`
  - Posts to Expense Service API
  - Tracks success/failure
- Returns count of created expenses

## Error Handling

- **PDF Parsing Errors**: Caught and reported; service continues with extracted transactions
- **GenAI Service Unavailable**: Falls back to default categorization (LOW confidence)
- **Category Not Found**: Transaction skipped with error logged
- **Expense Creation Failure**: Logged; processing continues with next transaction

## DTOs

### ExtractedTransactionDTO
```java
LocalDate date;
Double amount;
String description;
String merchant;
String paymentMethod;  // CARD, CASH, UPI, etc.
String currency;       // INR, USD, etc.
```

### CategorizedTransactionDTO
```java
ExtractedTransactionDTO transaction;
String suggestedCategory;
String confidence;  // HIGH, MEDIUM, LOW
```

### StatementProcessingResponseDTO
```java
String statementId;
String status;                    // PROCESSING, COMPLETED, FAILED
Long totalTransactionsExtracted;
Long totalTransactionsCategorized;
Long totalExpensesCreated;
List<String> errors;
String message;
```

## Transaction Parsing

The service uses regex patterns to identify transaction lines:

```regex
^(\d{2}[-/]\d{2}[-/]\d{2,4})\s+(.+?)\s+(\d+(?:,\d{3})*(?:\.\d{2})?)\s*$
```

Matches:
- **Date**: dd/MM/yyyy, dd-MM-yyyy, MM/dd/yyyy, etc.
- **Description**: Merchant name and transaction details
- **Amount**: Numeric amount with optional thousand separators

Example:
```
03/03/2026 Amazon Purchase 1,250.50
```

## Batching Strategy

Transactions are processed in batches of 20 to optimize:
- **API calls to GenAI service** (fewer round trips)
- **Processing efficiency** (cost-effective)
- **Error recovery** (batch failures don't affect other batches)

## Security

- **Authentication**: OAuth2 JWT tokens (from Keycloak)
- **Authorization**: User can only process statements for themselves
- **File Upload Limits**: Max 50MB per file
- **Input Validation**: PDF file extension check, empty file validation

## Building & Running

### Build
```bash
cd file-processing-service
./gradlew build
```

### Run Locally
```bash
./gradlew bootRun
```

### Docker
```bash
docker build -t file-processing-service:latest .
docker run -p 8083:8083 file-processing-service:latest
```

## Integration Points

### Expense Service
- **GET** `/api/categories` - Fetch available expense categories
- **POST** `/api/expenses/add` - Create new expense

### GenAI Service
- **POST** `/genai/query` - Generic AI query (legacy)
- **POST** `/genai/categorize-batch` - Bulk transaction categorization

## Future Enhancements

1. **Bank-Specific Parsers** - Support for HDFC, ICICI, AXIS, etc.
2. **Async Processing** - Use Kafka for processing large files
3. **Transaction Storage** - Store extracted transactions for audit/reprocessing
4. **Duplicate Detection** - Advanced matching before expense creation
5. **Manual Review Queue** - For low-confidence categorizations
6. **Receipt Storage** - Integration with S3 for receipt images
7. **Processing History** - Track statement upload history
8. **Webhook Notifications** - Notify on completion/failure

## Troubleshooting

### "PDF extraction failed: unable to parse PDF"
- Ensure PDF is a valid text-based PDF (not scanned image)
- Check PDF format compatibility

### "No transactions found in the PDF"
- Statement format may not match expected pattern
- Add custom parser for specific bank format

### "GenAI service unavailable"
- Check if genai-service is running on configured URL
- Default fallback categorization is applied (LOW confidence)

### "Category not found"
- Ensure category exists in Expense Service
- Check category name matching (case-sensitive)

## Testing

```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests FileProcessingServiceApplicationTests
```

## Contributing

1. Follow Spring Boot best practices
2. Add unit tests for new features
3. Update this README for API changes
4. Use semantic commit messages

## Support

For issues or questions, refer to the main Finance Manager AI project documentation.

