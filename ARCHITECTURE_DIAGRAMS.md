# Architecture & Data Flow Diagrams

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENT LAYER                               │
│  (UI/PowerShell Script)                                            │
│                                                                     │
│  1. User authenticates to Keycloak                                 │
│  2. Gets JWT token                                                 │
│  3. Uploads CSV/Excel file with token                              │
└────────┬──────────────────────────────────────────────────────────┘
         │
         │ POST /api/statements/upload
         │ Authorization: Bearer <TOKEN>
         │ Content-Type: multipart/form-data
         │
         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   FILE PROCESSING SERVICE                          │
│                      (Port 8083)                                   │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │ StatementProcessingService (Orchestrator)                   │  │
│  │  - Coordinates entire workflow                              │  │
│  │  - Passes token to all services                             │  │
│  └──────┬──────────────────────┬─────────────────────┬────────┘  │
│         │                      │                     │             │
│    ┌────▼───────────┐  ┌──────▼──────────┐  ┌─────▼────────────┐ │
│    │ FileExtraction │  │ ExpenseCreation │  │ CategorizationSvc│ │
│    │ Service        │  │ Service         │  │                  │ │
│    │                │  │                 │  │ ┌──────────────┐ │ │
│    │ Extracts CSV/  │  │ • Calls Expense │  │ │ With Token:  │ │ │
│    │ Excel files    │  │   Service       │  │ │ • Calls GenAI│ │ │
│    │                │  │ • Gets categories│ │ │   Service   │ │ │
│    │ Returns:       │  │ • With Bearer   │  │ │ • Includes  │ │ │
│    │ • List of      │  │   token         │  │ │   headers   │ │ │
│    │   transactions │  │                 │  │ │   in prompt │ │ │
│    │   (w/ headers) │  │ Returns:        │  │ │             │ │ │
│    │   marked as    │  │ • List of       │  │ │ Returns:    │ │ │
│    │   isHeaderRow  │  │   categories    │  │ │ • Categorized│ │ │
│    └────────────────┘  └─────────────────┘  │ │   transactions│ │ │
│                                             │ │ • Confidence  │ │ │
│                                             │ │   scores      │ │ │
│                                             └──────────────────┘ │ │
└──────┬───────────────────────────────────────────────────────────┘
       │
       │ Response with categorizedTransactions
       │
       ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    INTER-SERVICE CALLS                              │
│                                                                     │
│  ┌──────────────────────────┐      ┌────────────────────────────┐ │
│  │ Expense Service          │      │ GenAI Service              │ │
│  │ (Port 8082)              │      │ (Port 8084)                │ │
│  │                          │      │                            │ │
│  │ GET /api/categories      │      │ POST /genai/query          │ │
│  │ Authorization: Bearer... │      │ Authorization: Bearer...   │ │
│  │                          │      │                            │ │
│  │ Returns:                 │      │ Request:                   │ │
│  │ [                        │      │ {                          │ │
│  │   {id, name, active}     │      │   "query": "Headers:...\n  │ │
│  │   ...                    │      │   Categorize these...\n    │ │
│  │ ]                        │      │   Transactions:..."        │ │
│  │                          │      │ }                          │ │
│  │                          │      │                            │ │
│  │                          │      │ Response:                  │ │
│  │                          │      │ {                          │ │
│  │                          │      │   "query": "...",          │ │
│  │                          │      │   "response": "Shopping\n  │ │
│  │                          │      │   Dining\n..."             │ │
│  │                          │      │ }                          │ │
│  └──────────────────────────┘      └────────────────────────────┘ │
│          ▲                                    ▲                     │
│          │ With Bearer Token                  │ With Bearer Token  │
│          │ (Propagated from Client)           │ (Propagated)       │
│          └────────────────────────────────────┘                     │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
       │
       │ Status: CATEGORIZED
       │ categorizedTransactions: [...]
       │
       ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      RESPONSE TO CLIENT                             │
│                                                                     │
│  {                                                                  │
│    "statementId": "uuid",                                          │
│    "status": "CATEGORIZED",                                        │
│    "totalTransactionsExtracted": 4,                                │
│    "totalTransactionsCategorized": 4,                              │
│    "totalExpensesCreated": 0,                                      │
│    "message": "Ready for UI validation...",                        │
│    "categorizedTransactions": [                                    │
│      {                                                             │
│        "transaction": { date, amount, description, ... },         │
│        "suggestedCategory": "Shopping",                            │
│        "confidence": "HIGH"                                        │
│      },                                                            │
│      ...                                                           │
│    ]                                                               │
│  }                                                                 │
│                                                                     │
│  ► UI displays categorized transactions for user validation        │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Data Flow Sequence

```
Timeline →

1. CLIENT SIDE
   ├─ User authenticates to Keycloak
   ├─ Receives JWT token (valid for ~5 minutes)
   └─ Prepares CSV file for upload

2. FILE UPLOAD
   ├─ User submits POST /api/statements/upload
   ├─ Includes Authorization: Bearer <TOKEN>
   └─ Includes file: <CSV_CONTENT>

3. FILE EXTRACTION (File Processing Service)
   ├─ StatementProcessingService receives request
   ├─ Validates token (implicitly via Spring Security)
   ├─ Calls FileExtractionService.extractTransactions()
   ├─ Returns: List<ExtractedTransactionDTO>
   │  ├─ isHeaderRow = true for first row
   │  ├─ isHeaderRow = false for data rows
   │  └─ Each row has: date, amount, description, merchant, rowValues, columnHeaders
   └─ Logs extracted data

4. GET CATEGORIES (with Token)
   ├─ StatementProcessingService calls:
   │  └─ expenseCreationService.getAvailableCategories(username, token)
   ├─ ExpenseCreationService creates HttpHeaders
   │  └─ headers.set("Authorization", "Bearer " + token)
   ├─ Calls Expense Service: GET /api/categories
   ├─ Expense Service validates token with Keycloak
   ├─ Returns: List<String> = ["Groceries", "Utilities", "Shopping", ...]
   └─ Logs categories retrieved

5. CATEGORIZATION (with Token & Headers)
   ├─ StatementProcessingService calls:
   │  └─ categorizationService.categorizeBatch(transactions, categories, token)
   ├─ CategorizationService builds prompt:
   │  ├─ Includes: "Column Headers: [Date, Description, Amount, Merchant]"
   │  ├─ Includes: All transaction rows (headers + data)
   │  └─ Instructions: "For each row, respond with category name"
   ├─ Creates HttpHeaders with Bearer token
   ├─ Calls GenAI Service: POST /genai/query
   ├─ GenAI Service validates token with Keycloak
   ├─ Gemini AI processes prompt
   ├─ Returns: "Shopping\nDining\nUtilities\nGroceries"
   ├─ Parses response line-by-line:
   │  ├─ "Shopping" → matches available category → HIGH confidence
   │  ├─ "Dining" → matches available category → HIGH confidence
   │  └─ etc.
   └─ Returns: List<CategorizedTransactionDTO>

6. BUILD RESPONSE
   ├─ StatementProcessingService builds StatementProcessingResponseDTO:
   │  ├─ statementId = UUID
   │  ├─ status = "CATEGORIZED"
   │  ├─ totalTransactionsExtracted = 4
   │  ├─ totalTransactionsCategorized = 4
   │  ├─ totalExpensesCreated = 0
   │  ├─ categorizedTransactions = [4 CategorizedTransactionDTO objects]
   │  └─ errors = []
   └─ Returns to client

7. UI DISPLAYS RESULTS
   ├─ Client receives response with HTTP 200
   ├─ Parses categorizedTransactions array
   ├─ Displays in table:
   │  ├─ Date | Description | Amount | Suggested Category | Confidence | Edit
   │  ├─ 03/01 | Amazon | 150.50 | Shopping | HIGH | [Change]
   │  ├─ 03/02 | Starbucks | 5.50 | Dining | HIGH | [Change]
   │  └─ etc.
   └─ User can approve or change categories

8. FUTURE: USER VALIDATION & EXPENSE CREATION
   ├─ User clicks "Confirm & Create Expenses"
   ├─ POST /api/statements/validate
   │  ├─ statementId: "uuid"
   │  └─ transactions: [validated categorized transactions]
   ├─ File Processing Service:
   │  ├─ Creates ExpenseRequestDTO for each transaction
   │  ├─ Calls Expense Service: POST /api/expenses/add (with token)
   │  └─ Returns: List of created expense IDs
   └─ Client receives confirmation with expense IDs
```

---

## Token Authentication Flow

```
┌──────────────────────────────────────────────────────────────┐
│                      KEYCLOAK (Port 8080)                   │
│                  Identity Provider (Realm)                  │
└──────────────────────────────────────────────────────────────┘
         ▲
         │
         │ 1. POST /protocol/openid-connect/token
         │    client_id=finance-client
         │    username=testuser
         │    password=password
         │
┌────────┴──────────────────────────────────────────────────────┐
│                        CLIENT                                 │
│  (PowerShell / JavaScript)                                    │
│                                                               │
│  Receives: JWT Token                                          │
│  eyJhbGc....[PAYLOAD]....[SIGNATURE]                         │
│                                                               │
│  Payload contains:                                            │
│  {                                                            │
│    "preferred_username": "testuser",                         │
│    "roles": ["user"],                                        │
│    "exp": 1741269...,  (expiration time)                     │
│    "iat": 1741268...,  (issued at)                           │
│    ...                                                        │
│  }                                                            │
│                                                               │
│  2. Stores token: $token = response.access_token             │
│                                                               │
│  3. Includes in all requests:                                │
│     $headers = @{Authorization="Bearer " + $token}           │
└────────┬──────────────────────────────────────────────────────┘
         │
         │ Authorization: Bearer eyJhbGc...
         │
    ┌────▼──────────────────────┐
    │ File Processing Service    │
    │ (Port 8083)                │
    │                            │
    │ Spring Security:           │
    │ 1. Extracts token          │
    │ 2. Validates signature     │
    │ 3. Checks expiration       │
    │ 4. Sets Authentication     │
    │    context                 │
    │                            │
    │ Extracts: username="testuser"
    │           roles=["user"]
    │                            │
    │ Passes token to:           │
    │ • ExpenseCreationService   │
    │ • CategorizationService    │
    └────┬──────────────────┬────┘
         │                  │
         │ Token            │ Token
         │                  │
    ┌────▼──────────────┐ ┌▼─────────────────┐
    │ Expense Service   │ │ GenAI Service    │
    │ (Port 8082)       │ │ (Port 8084)      │
    │                  │ │                  │
    │ Validates:       │ │ Validates:       │
    │ 1. Token present │ │ 1. Token present │
    │ 2. Signature OK  │ │ 2. Signature OK  │
    │ 3. Not expired   │ │ 3. Not expired   │
    │ 4. Rights match  │ │ 4. Rights match  │
    │                  │ │                  │
    │ Returns 200 OK   │ │ Returns 200 OK   │
    │ with data        │ │ with categorized │
    │                  │ │ transactions     │
    └──────────────────┘ └──────────────────┘
         │                  │
         └────────┬─────────┘
                  │
                  │ Response with categorized transactions
                  │
                  ▼
            ┌───────────────┐
            │   CLIENT      │
            │  Displays     │
            │  Results      │
            └───────────────┘
```

---

## Transaction Object Transformation

```
CSV/EXCEL FILE
├─ Date,Description,Amount,Merchant
├─ 03/01/2026,Amazon Purchase,150.50,Amazon
├─ 03/02/2026,Starbucks Coffee,5.50,Starbucks
└─ ...

                    │
                    │ FileExtractionService.extractTransactions()
                    ▼

List<ExtractedTransactionDTO>
├─ [0] ExtractedTransactionDTO
│  ├─ rowValues: ["Date", "Description", "Amount", "Merchant"]
│  ├─ isHeaderRow: true
│  ├─ date: null
│  ├─ amount: null
│  └─ ...
├─ [1] ExtractedTransactionDTO
│  ├─ rowValues: ["03/01/2026", "Amazon Purchase", "150.50", "Amazon"]
│  ├─ isHeaderRow: false
│  ├─ date: 2026-03-01
│  ├─ amount: 150.50
│  ├─ description: "Amazon Purchase"
│  ├─ merchant: "Amazon"
│  ├─ columnHeaders: ["Date", "Description", "Amount", "Merchant"]
│  └─ ...
├─ [2] ExtractedTransactionDTO (Starbucks)
│  └─ ...
└─ ...

                    │
                    │ CategorizationService.categorizeBatch()
                    │ + ExpenseCreationService.getAvailableCategories()
                    │ + GenAI categorization
                    ▼

List<CategorizedTransactionDTO>
├─ [0] CategorizedTransactionDTO (same as [1] from above + categorization)
│  ├─ transaction: ExtractedTransactionDTO (rowValues, date, amount, etc.)
│  ├─ suggestedCategory: "Shopping"
│  └─ confidence: "HIGH"
├─ [1] CategorizedTransactionDTO (Starbucks)
│  ├─ transaction: ExtractedTransactionDTO
│  ├─ suggestedCategory: "Dining"
│  └─ confidence: "HIGH"
└─ ...

                    │
                    │ StatementProcessingService.processStatement()
                    ▼

StatementProcessingResponseDTO
├─ statementId: "550e8400-e29b-41d4-a716-446655440000"
├─ status: "CATEGORIZED"
├─ totalTransactionsExtracted: 2
├─ totalTransactionsCategorized: 2
├─ totalExpensesCreated: 0
├─ message: "Statement processed and categorized successfully..."
├─ categorizedTransactions: [List<CategorizedTransactionDTO>]
│  ├─ [0] CategorizedTransactionDTO (Amazon)
│  ├─ [1] CategorizedTransactionDTO (Starbucks)
│  └─ ...
├─ errors: []
└─ ...

                    │
                    │ Return to Client
                    ▼

JSON RESPONSE
{
  "statementId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "CATEGORIZED",
  "totalTransactionsExtracted": 2,
  "totalTransactionsCategorized": 2,
  "totalExpensesCreated": 0,
  "message": "...",
  "categorizedTransactions": [
    {
      "transaction": {
        "date": "2026-03-01",
        "amount": 150.50,
        "description": "Amazon Purchase",
        "merchant": "Amazon",
        "rowValues": ["03/01/2026", "Amazon Purchase", "150.50", "Amazon"],
        "columnHeaders": ["Date", "Description", "Amount", "Merchant"],
        "isHeaderRow": false
      },
      "suggestedCategory": "Shopping",
      "confidence": "HIGH"
    },
    ...
  ]
}

                    │
                    │ UI Displays Results
                    ▼

┌─────────────────────────────────────────────────────────────┐
│ Transaction Categorization Result                          │
├─────────────────────────────────────────────────────────────┤
│ Date        Description          Amount   Category  Conf   │
├─────────────────────────────────────────────────────────────┤
│ 03/01/2026  Amazon Purchase      150.50   Shopping  HIGH   │
│ 03/02/2026  Starbucks Coffee       5.50   Dining    HIGH   │
├─────────────────────────────────────────────────────────────┤
│                [Confirm & Create Expenses]                 │
└─────────────────────────────────────────────────────────────┘
```

---

## Error Handling Flow

```
Exception during processing:
         │
    ┌────▼─────────────────────┐
    │ CategorizationService    │
    │ categorizeBatch()        │
    │                          │
    │ try {                    │
    │   callGenAIService()     │──► GenAI Service down
    │ } catch (Exception e) {  │
    │   ┌─────────────────────┐│
    │   │ Logs error message  ││
    │   │ Returns default     ││
    │   │ categorization      ││
    │   │ (uses first         ││
    │   │  category for all)  ││
    │   └─────────────────────┘│
    │ }                        │
    └────┬────────────────────┘
         │
         ▼
    Returns categorized transactions
    with LOW confidence scores
         │
         ▼
    Response status: CATEGORIZED
    (User informed via message)
         │
         ▼
    UI displays results
    User can manually fix categories
    before creating expenses
```

---

## Summary

- **Security:** Bearer token propagated through all service calls
- **Context:** Headers included in GenAI prompt for better categorization
- **Flexibility:** Support for both plain text and JSON responses
- **User Control:** Categorized results returned for UI validation
- **Resilience:** Fallback mechanisms if services fail


