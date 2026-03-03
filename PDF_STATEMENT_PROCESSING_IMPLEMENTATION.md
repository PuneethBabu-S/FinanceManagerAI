# PDF Statement Processing Implementation Guide

## Overview

This guide covers the complete implementation of PDF statement processing with AI-powered transaction categorization in the Finance Manager AI system.

## Components Created

### 1. File Processing Service (New Microservice)
**Port:** 8083  
**Location:** `D:\Projects\FinanceManagerAI\file-processing-service`

#### Key Classes:
- **FileProcessingServiceApplication.java** - Main Spring Boot application
- **StatementProcessingController.java** - REST API endpoints
- **PDFExtractionService.java** - Extracts transactions from PDF files
- **CategorizationService.java** - Calls GenAI service for bulk categorization
- **ExpenseCreationService.java** - Integrates with Expense Service to create expenses
- **StatementProcessingService.java** - Orchestrates the complete pipeline

#### DTOs:
- **ExtractedTransactionDTO** - Raw transaction data from PDF
- **CategorizedTransactionDTO** - Transaction with suggested category
- **BulkCategorizationRequestDTO** - Request for batch categorization
- **StatementProcessingResponseDTO** - Response with processing status

### 2. GenAI Service Enhancement
**Port:** 8084  
**Location:** `D:\Projects\FinanceManagerAI\genaisvc`

#### New Endpoint:
- `POST /genai/categorize-batch` - Bulk transaction categorization

#### New Classes:
- **BulkCategorizationRequestDTO.java** - Request DTO with buildPrompt() method
- **TransactionDTO.java** - Transaction representation in GenAI service

### 3. Database Configuration
- Added `file_processing_db` to PostgreSQL instance
- Uses same postgres:15 container with separate database

## API Specification

### File Processing Service

#### 1. Upload & Process Statement
```
POST /api/statements/upload
Content-Type: multipart/form-data
Authorization: Bearer <JWT_TOKEN>

Parameters:
  file: <PDF_FILE> (binary)

Response (200 OK):
{
  "statementId": "uuid",
  "status": "COMPLETED|FAILED",
  "totalTransactionsExtracted": 25,
  "totalTransactionsCategorized": 25,
  "totalExpensesCreated": 25,
  "message": "Statement processed successfully",
  "errors": []
}
```

### GenAI Service

#### 1. Bulk Categorize Transactions
```
POST /genai/categorize-batch
Content-Type: application/json

Request:
{
  "transactions": [
    {
      "date": "2026-03-03",
      "amount": 1250.50,
      "description": "Amazon Purchase",
      "merchant": "Amazon"
    }
  ],
  "availableCategories": ["Groceries", "Utilities", "Shopping"]
}

Response (200 OK):
{
  "transactionCount": "1",
  "categorization": "Shopping\n"
}
```

## Processing Workflow

### Step 1: PDF Upload
```
Client sends PDF file to:
POST /api/statements/upload
```

### Step 2: Extract Transactions
```
PDFExtractionService:
1. Uses PDFBox to extract text from PDF
2. Parses lines using regex pattern
3. Extracts: date, amount, description, merchant
4. Returns List<ExtractedTransactionDTO>
```

### Step 3: Fetch Categories
```
ExpenseCreationService:
1. Calls Expense Service: GET /api/categories
2. Filters active categories
3. Returns List<String> (category names)
```

### Step 4: Bulk Categorization
```
CategorizationService:
1. Batches transactions (20 per batch)
2. Builds prompt for GenAI
3. Calls GenAI Service: POST /genai/categorize-batch
4. Parses response and matches categories
5. Returns List<CategorizedTransactionDTO>
```

### Step 5: Create Expenses
```
ExpenseCreationService:
1. For each categorized transaction:
   a. Resolve category ID from name
   b. Create ExpenseRequestDTO
   c. Call Expense Service: POST /api/expenses/add
2. Return count of created expenses
```

### Step 6: Return Status
```
StatementProcessingResponseDTO returned with:
- Total extracted transactions
- Total categorized transactions
- Total created expenses
- List of errors (if any)
```

## Configuration

### application.yaml (File Processing Service)
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

expense-service:
  url: http://localhost:8082
  
genai-service:
  url: http://localhost:8084
```

### Service Port Mapping
| Service | Port | Status |
|---------|------|--------|
| User Service | 8081 | Existing |
| Expense Service | 8082 | Existing |
| File Processing Service | 8083 | **NEW** |
| GenAI Service | 8084 | Existing (enhanced) |

## Dependencies Added

### File Processing Service (build.gradle)
```gradle
// PDF Processing
implementation 'org.apache.pdfbox:pdfbox:3.0.1'

// HTTP Client
implementation 'org.springframework.boot:spring-boot-starter-webflux'

// JSON Processing
implementation 'org.json:json:20231013'

// Lombok
compileOnly 'org.projectlombok:lombok'
annotationProcessor 'org.projectlombok:lombok'
```

### GenAI Service (build.gradle) - Added Lombok
```gradle
compileOnly 'org.projectlombok:lombok'
annotationProcessor 'org.projectlombok:lombok'
```

## Running the System

### Prerequisites
1. Docker & Docker Compose installed
2. Java 17+ installed
3. Gradle installed

### Start Infrastructure
```bash
cd D:\Projects\FinanceManagerAI
docker-compose up -d
```

This starts:
- PostgreSQL (with file_processing_db)
- MongoDB
- Redis
- Zookeeper & Kafka
- Elasticsearch
- Keycloak

### Build & Run Services

#### Terminal 1: User Service
```bash
cd user-service
./gradlew bootRun
```

#### Terminal 2: Expense Service
```bash
cd expense-service
./gradlew bootRun
```

#### Terminal 3: GenAI Service
```bash
cd genaisvc
./gradlew bootRun
```

#### Terminal 4: File Processing Service
```bash
cd file-processing-service
./gradlew bootRun
```

### Verify Services
```bash
# Check File Processing Service
curl http://localhost:8083/actuator/health

# Check GenAI Service
curl http://localhost:8084/actuator/health

# Check Expense Service
curl http://localhost:8082/actuator/health
```

## Testing

### Manual Test with cURL

#### 1. Get Authentication Token (from Keycloak)
```bash
# First, create a user in Keycloak at http://localhost:8080
# Then get token:
curl -X POST http://localhost:8080/realms/finance-manager/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=finance-client&username=testuser&password=testpass&grant_type=password"
```

#### 2. Upload PDF Statement
```bash
curl -X POST http://localhost:8083/api/statements/upload \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -F "file=@bank_statement.pdf"
```

#### 3. Expected Response
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

## Transaction Extraction Pattern

The PDF extraction service uses regex to identify transactions:

```regex
^(\d{2}[-/]\d{2}[-/]\d{2,4})\s+(.+?)\s+(\d+(?:,\d{3})*(?:\.\d{2})?)\s*$
```

### Expected PDF Format
```
Date         | Description/Merchant        | Amount
03/03/2026   | Amazon Purchase             | 1,250.50
04/03/2026   | Grocery Store               | 450.25
05/03/2026   | Fuel Station                | 2,000.00
```

### Supported Date Formats
- dd/MM/yyyy
- dd-MM-yyyy
- MM/dd/yyyy
- MM-dd-yyyy
- dd/MM/yy
- dd-MM-yy

## Error Handling

### PDF Extraction Errors
- Invalid PDF format → Returns error in response
- No transactions found → Returns FAILED status

### GenAI Service Errors
- Service unavailable → Falls back to default categorization (LOW confidence)
- Invalid response → Skips batch, continues with next

### Category Resolution Errors
- Category not found → Transaction skipped with error logged
- Multiple categories match → Uses first match

### Expense Creation Errors
- API call fails → Logs error, continues with next transaction
- Invalid category ID → Transaction skipped

## Performance Considerations

### Batch Processing
- **Batch Size:** 20 transactions per GenAI API call
- **Rationale:** 
  - Reduces API calls (cost-effective)
  - Minimizes latency
  - Provides good granularity for error recovery

### Concurrency
- Currently **synchronous** processing
- **Future:** Async processing with Kafka for large files

### PDF Size Limits
- **Max file size:** 50MB
- **Max request size:** 50MB
- Can be adjusted in `application.yaml`

## Scalability Roadmap

### Phase 1 (Current)
- ✅ Synchronous PDF processing
- ✅ Batch categorization (20 transactions)
- ✅ Direct Expense Service integration

### Phase 2
- ⏳ Async processing with Kafka
- ⏳ Transaction history/audit storage
- ⏳ Dead-letter queue for failed transactions

### Phase 3
- ⏳ Bank-specific PDF parsers
- ⏳ OCR for scanned PDFs
- ⏳ Receipt image storage (S3)

### Phase 4
- ⏳ Webhook notifications
- ⏳ Manual review queue
- ⏳ ML model optimization

## Debugging

### Enable Debug Logging
Edit `application.yaml`:
```yaml
logging:
  level:
    com.financemanagerai: DEBUG
    org.springframework: DEBUG
```

### Check Logs
```bash
# For file-processing-service (port 8083)
tail -f logs/file-processing-service.log
```

### Common Issues

**Issue:** "No transactions found in the PDF"
```
Solution: 
1. Verify PDF is text-based (not scanned image)
2. Check if statement format matches expected pattern
3. Add custom parser for specific bank format
```

**Issue:** "GenAI service unavailable"
```
Solution:
1. Verify genai-service is running (port 8084)
2. Check network connectivity
3. Default fallback categorization is applied
```

**Issue:** "Category not found"
```
Solution:
1. Verify categories exist in Expense Service
2. Check category name spelling (case-sensitive)
3. Ensure category is marked as active
```

## Integration Checklist

- [x] Create File Processing Service microservice
- [x] Implement PDF extraction using PDFBox
- [x] Add bulk categorization endpoint to GenAI Service
- [x] Implement Expense Service integration
- [x] Add database support (PostgreSQL)
- [x] Create REST API endpoints
- [x] Add error handling & logging
- [x] Document API specification
- [x] Create README & guides
- [ ] Add unit tests
- [ ] Add integration tests
- [ ] Add sample PDFs for testing
- [ ] Add Swagger/OpenAPI documentation
- [ ] Setup CI/CD pipeline

## Next Steps

1. **Test Locally:**
   - Generate sample bank statement PDFs
   - Test end-to-end flow
   - Verify category matching accuracy

2. **Enhance PDF Parsing:**
   - Add bank-specific parsers
   - Improve merchant extraction
   - Handle edge cases (refunds, transfers)

3. **Optimize Categorization:**
   - Fine-tune GenAI prompts
   - Implement confidence scoring
   - Add manual review workflow

4. **Scale Infrastructure:**
   - Move to async processing
   - Implement Kafka integration
   - Add transaction storage/history

## Support & Questions

Refer to:
- `file-processing-service/README.md` - Service-specific documentation
- `genaisvc/README.md` - GenAI service documentation
- Main project `README.md` - Overall architecture


