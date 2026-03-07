# Transaction Categorization - Technical Reference

## Architecture Overview

```
┌─────────────┐
│   Browser   │
│     UI      │
└──────┬──────┘
       │ JWT Token
       ▼
┌──────────────────────────────┐
│  file-processing-service     │
│  (Port 8083)                 │
│                              │
│  1. Extract transactions     │
│  2. Filter headers           │
│  3. Get categories ────────┐ │
│  4. Call GenAI ────────┐   │ │
│  5. Parse & return     │   │ │
└────────────────────────┼───┼─┘
                         │   │
         ┌───────────────┘   └──────────────┐
         │                                   │
         ▼                                   ▼
┌────────────────────┐            ┌──────────────────┐
│   genaisvc         │            │  expense-service │
│   (Port 8089)      │            │  (Port 8082)     │
│                    │            │                  │
│  - Gemini AI       │            │  - Get categories│
│  - Categorization  │            │  - User mgmt     │
└────────────────────┘            └──────────────────┘
```

## Service Endpoints

### file-processing-service

#### POST /api/statements/upload
**Headers**:
```
Authorization: Bearer <jwt_token>
Content-Type: multipart/form-data
```

**Request**:
```
file: <CSV/Excel file>
```

**Response**:
```json
{
  "statementId": "string",
  "status": "CATEGORIZED",
  "totalTransactionsExtracted": 0,
  "totalTransactionsCategorized": 0,
  "totalExpensesCreated": 0,
  "errors": [],
  "message": "string",
  "categorizedTransactions": [...]
}
```

### genaisvc

#### POST /genai/query
**Headers**:
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request**:
```json
{
  "query": "categorization prompt with transactions"
}
```

**Response**:
```json
{
  "query": "...",
  "response": "JSON lines with categories and confidence"
}
```

### expense-service

#### GET /api/categories
**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response**:
```json
[
  {
    "id": 1,
    "name": "Groceries",
    "active": true
  },
  ...
]
```

## Data Flow

### 1. File Upload
```java
MultipartFile → FileExtractionService
  → List<ExtractedTransactionDTO>
    - date, amount, description, merchant, paymentMethod, currency
    - rowValues (raw CSV row)
    - isHeaderRow (boolean)
```

### 2. Header Filtering
```java
CategorizationService.categorizeBatch()
  → Filter transactions where isHeaderRow = false
  → Keep header for context in prompt
  → Send only data rows to GenAI
```

### 3. Category Fetching
```java
ExpenseCreationService.getAvailableCategories(username, token)
  → HTTP GET to expense-service/api/categories
  → Extract active category names
  → Return List<String>
```

### 4. Prompt Building
```java
buildCategorizationPrompt(dataRows, headerRow, categories)
  → Include: available categories
  → Include: column headers from CSV
  → Include: transaction rows
  → Request: JSON format {"category": "...", "confidence": "..."}
```

Example prompt:
```
You are a financial transaction categorizer. Categorize the following transactions into one of these categories: Groceries, Utilities, Transportation, Entertainment, Dining, Shopping, Others

Column Headers: [Date, Description, Category, Amount, Currency, Payment Method, Tags]

For each transaction, respond with ONLY a JSON object on a single line with this format:
{"category": "<category_name>", "confidence": "<HIGH|MEDIUM|LOW>"}

Rules:
- Match based on transaction description, comments, vendor, amount patterns, or any other relevant details
- Use confidence scores: HIGH for certain matches, MEDIUM for reasonable matches, LOW for uncertain matches
- Respond with exactly one JSON object per transaction line
- Use ONLY the available categories listed above
- Do NOT use any other categories

Transaction Data:
1. [2026-03-06, Starbucks Coffee, , 25.50, INR, DEBIT_CARD, ]
2. [2026-03-05, Uber Ride, , 150.00, INR, UPI, ]

Respond with one JSON object per line, in the same order as the rows above.
```

### 5. GenAI Call
```java
callGenAIService(prompt, token)
  → POST to genaisvc/genai/query
  → Headers: Authorization: Bearer <token>
  → Body: {"query": "<prompt>"}
  → Extract response.response field
```

Expected GenAI response:
```
{"category": "Dining", "confidence": "HIGH"}
{"category": "Transportation", "confidence": "HIGH"}
```

### 6. Response Parsing
```java
parseCategorizationResponse(transactions, response, categories)
  → Split response by newlines
  → Filter empty/whitespace lines
  → For each transaction:
    - Try parse line as JSON
    - Extract category and confidence
    - Match category to available categories
    - Fallback to default if no match
  → Return List<CategorizedTransactionDTO>
```

### 7. Response Assembly
```java
StatementProcessingResponseDTO
  - statementId: UUID
  - status: "CATEGORIZED"
  - totalTransactionsExtracted: count of data rows (no headers)
  - totalTransactionsCategorized: count of results
  - totalExpensesCreated: 0 (not creating yet)
  - categorizedTransactions: List<CategorizedTransactionDTO>
    - transaction: ExtractedTransactionDTO (original data)
    - suggestedCategory: String (from GenAI)
    - confidence: String (HIGH/MEDIUM/LOW)
```

## Key Classes

### ExtractedTransactionDTO
```java
public class ExtractedTransactionDTO {
    private LocalDate date;
    private Double amount;
    private String description;
    private String merchant;
    private String paymentMethod;
    private String currency;
    private List<String> rowValues;
    private List<String> columnHeaders;
    private boolean isHeaderRow;
}
```

### CategorizedTransactionDTO
```java
public class CategorizedTransactionDTO {
    private ExtractedTransactionDTO transaction;
    private String suggestedCategory;
    private String confidence; // HIGH, MEDIUM, LOW
}
```

### StatementProcessingResponseDTO
```java
public class StatementProcessingResponseDTO {
    private String statementId;
    private String status;
    private Long totalTransactionsExtracted;
    private Long totalTransactionsCategorized;
    private Long totalExpensesCreated;
    private List<String> errors;
    private String message;
    private List<CategorizedTransactionDTO> categorizedTransactions;
}
```

## Configuration

### file-processing-service/application.yaml
```yaml
server:
  port: 8083

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/finance-manager

expense-service:
  url: http://localhost:8082

genai-service:
  url: http://localhost:8089
```

### genaisvc/application.properties
```properties
server.port=8089

spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/finance-manager

gemini.api.key=${GEMINI_API_KEY}
gemini.model=models/gemini-2.5-flash
gemini.api.url=https://generativelanguage.googleapis.com/v1beta
```

## Security

### Token Extraction
```java
private String getAuthToken(Authentication authentication) {
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
        return jwtAuth.getToken().getTokenValue();
    }
    return authentication.getCredentials() != null ? 
           authentication.getCredentials().toString() : "";
}
```

### Token Propagation
```java
HttpHeaders headers = new HttpHeaders();
headers.set("Authorization", "Bearer " + token);
headers.set("Content-Type", "application/json");

HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
restTemplate.postForObject(endpoint, entity, String.class);
```

## Error Handling

### GenAI Service Failure
- Catches exceptions in categorizeBatch()
- Logs error with stack trace
- Returns default categorization (first available category, LOW confidence)

### Response Parsing Failure
- Tries JSON parsing first
- Falls back to plain text parsing
- Normalizes whitespace
- Uses default category if no valid response

### Token Issues
- 401 Unauthorized from downstream services
- Logged and returned in errors array
- Client receives error details in response

## Logging

Key log points:
```
INFO  - Starting categorization for X transactions
DEBUG - Found header row: [...]
INFO  - Filtered X data rows (excluded Y header rows)
DEBUG - Built categorization prompt with X available categories
DEBUG - Received GenAI response: {...}
DEBUG - Parsed X valid response lines for Y transactions
DEBUG - Parsed JSON for transaction N: category=X, confidence=Y
INFO  - Successfully categorized X transactions
ERROR - Error calling GenAI service: {...}
```

## Performance Considerations

- **Batch Size**: Currently processes all transactions in one GenAI call
- **Token Usage**: Filtered headers reduce GenAI token consumption
- **Response Time**: Depends on GenAI latency (~1-3 seconds for Gemini)
- **Caching**: Categories cached per request (not persistent)

## Future Improvements

1. Implement chunking for large files (>100 transactions)
2. Add retry logic for GenAI failures
3. Cache category mappings (Redis)
4. Support async processing for large batches
5. Add user feedback loop for improving categorization
6. Support multiple GenAI providers (OpenAI, Claude, etc.)

