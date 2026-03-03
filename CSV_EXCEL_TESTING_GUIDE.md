# CSV/Excel Processing - Testing Guide

## Quick Start

### 1. Create a Sample CSV File

**File: `sample_transactions.csv`**
```csv
Transaction Date,Description,Amount,Merchant,Type
2026-03-03,Amazon Purchase,1250.50,Amazon,Online Shopping
2026-03-04,Grocery Store,450.25,Walmart,Groceries
2026-03-05,Fuel Station,2000.00,Shell,Transportation
2026-03-06,Coffee Shop,250.75,Starbucks,Dining
2026-03-07,Electric Bill,1500.00,Power Corp,Utilities
```

### 2. Create a Sample Excel File

**File: `sample_transactions.xlsx`**

| Transaction Date | Description | Amount | Merchant | Type |
|---|---|---|---|---|
| 2026-03-03 | Amazon Purchase | 1250.50 | Amazon | Online Shopping |
| 2026-03-04 | Grocery Store | 450.25 | Walmart | Groceries |
| 2026-03-05 | Fuel Station | 2000.00 | Shell | Transportation |
| 2026-03-06 | Coffee Shop | 250.75 | Starbucks | Dining |
| 2026-03-07 | Electric Bill | 1500.00 | Power Corp | Utilities |

## Build the Service

```bash
cd D:\Projects\FinanceManagerAI\file-processing-service
./gradlew clean build
```

## Run the Service

```bash
./gradlew bootRun
```

You should see:
```
Started FileProcessingServiceApplication in X seconds
```

## Test the API

### Option 1: Using cURL

#### Test with CSV:
```bash
curl -X POST http://localhost:8083/api/statements/upload \
  -F "file=@sample_transactions.csv"
```

#### Test with Excel:
```bash
curl -X POST http://localhost:8083/api/statements/upload \
  -F "file=@sample_transactions.xlsx"
```

### Option 2: Using Postman

1. **Create New Request**
   - Method: POST
   - URL: `http://localhost:8083/api/statements/upload`

2. **Body**
   - Select: `form-data`
   - Key: `file`
   - Value: Select your CSV or Excel file

3. **Send**

### Expected Response

```json
{
  "statementId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "totalTransactionsExtracted": 5,
  "totalTransactionsCategorized": 5,
  "totalExpensesCreated": 0,
  "message": "Statement processed successfully",
  "errors": []
}
```

## Verify Header Extraction

### Check Logs

Watch the service logs during upload. You should see:
```
Extracted 6 rows from file (including headers).
[HEADERS] [Transaction Date, Description, Amount, Merchant, Type]
[ROW 1] [2026-03-03, Amazon Purchase, 1250.50, Amazon, Online Shopping]
[ROW 2] [2026-03-04, Grocery Store, 450.25, Walmart, Groceries]
...
```

### Verify AI Prompt Includes Headers

The CategorizationService should build a prompt like:
```
Column Headers: [Transaction Date, Description, Amount, Merchant, Type]

[HEADER ROW] [Transaction Date, Description, Amount, Merchant, Type]
1. [2026-03-03, Amazon Purchase, 1250.50, Amazon, Online Shopping]
2. [2026-03-04, Grocery Store, 450.25, Walmart, Groceries]
...
```

## Test File Validation

### Valid Files ✅
- `transactions.csv`
- `statements.CSV`
- `data.xlsx`
- `export.XLSX`
- `report.xls`
- `statement.XLS`

### Invalid Files ❌
- `statement.pdf` → Error: "File must be in CSV or Excel format"
- `data.txt` → Error: "File must be in CSV or Excel format"
- `empty.csv` → Error: "No transactions found in the file"

## CSV Format Requirements

### Must Have:
- ✅ Headers in first row (any column names allowed)
- ✅ Data in subsequent rows
- ✅ Same number of columns in all rows

### Example Valid Formats:
```csv
Date,Description,Amount
2026-03-03,Purchase,100.00
2026-03-04,Shopping,200.00
```

```csv
Transaction Date,Description,Amount,Merchant,Category,Type
2026-03-03,Amazon,1250.50,Amazon,Shopping,Online
2026-03-04,Walmart,450.25,Walmart,Groceries,Retail
```

## Excel Format Requirements

### Must Have:
- ✅ Headers in first row
- ✅ Data in subsequent rows
- ✅ Only use first sheet (Sheet1)

### Example:
Sheet1:
| Date | Description | Amount |
|------|---|---|
| 2026-03-03 | Purchase | 100.00 |
| 2026-03-04 | Shopping | 200.00 |

## Test Different Scenarios

### Scenario 1: Basic CSV
```bash
curl -X POST http://localhost:8083/api/statements/upload \
  -F "file=@basic.csv"
```

### Scenario 2: Excel with Special Characters
Create Excel with special characters in merchant names and verify extraction

### Scenario 3: Large File
Create CSV with 100+ rows and test batch processing

### Scenario 4: Empty Columns
Test with some empty cells in data rows

## Debugging

### Enable Debug Logging

Edit `application.yaml`:
```yaml
logging:
  level:
    com.financemanagerai: DEBUG
    org.springframework: DEBUG
```

### Monitor Extraction

Look for these logs:
```
Extracted 6 rows from file (including headers).
[HEADERS] [...]
[ROW 1] [...]
[ROW 2] [...]
```

### Check Error Messages

If extraction fails, check for:
- File is empty
- Unsupported file format
- Malformed CSV/Excel structure

## Integration Testing

### Test with Expense Service

1. Start Expense Service on port 8082
2. Configure categories in Expense Service
3. Upload CSV/Excel to File Processing Service
4. Verify expenses are created in Expense Service

### Test with GenAI Service

1. Start GenAI Service on port 8084
2. Configure Gemini API key
3. Upload CSV/Excel
4. Verify categorization works with headers context

## Performance Testing

### Small File (10 rows)
```bash
time curl -X POST http://localhost:8083/api/statements/upload \
  -F "file=@small.csv"
```
Expected: <1 second

### Medium File (100 rows)
```bash
time curl -X POST http://localhost:8083/api/statements/upload \
  -F "file=@medium.csv"
```
Expected: 1-5 seconds (depends on GenAI latency)

### Large File (1000 rows)
```bash
time curl -X POST http://localhost:8083/api/statements/upload \
  -F "file=@large.csv"
```
Expected: 5-30 seconds (batch processing kicks in at 20 rows)

## Troubleshooting

### Issue: "File must be in CSV or Excel format"
- ✅ Check file extension is .csv, .xlsx, or .xls
- ✅ Ensure filename doesn't have spaces or special chars
- ✅ Verify case-insensitive matching works

### Issue: "No transactions found in the file"
- ✅ Verify file has at least 2 rows (header + 1 data row)
- ✅ Check CSV format is valid (proper delimiters)
- ✅ Ensure Excel has data in Sheet1

### Issue: Headers not included in AI prompt
- ✅ Check logs for [HEADERS] line
- ✅ Verify isHeaderRow flag is set correctly
- ✅ Check columnHeaders field is populated

### Issue: Rows parsed incorrectly
- ✅ Verify CSV delimiter is comma (not semicolon)
- ✅ Check for quoted fields with commas inside
- ✅ Ensure Excel cells are formatted as Text

## Next Steps After Testing

1. ✅ Test with real bank statement exports
2. ✅ Adjust AI prompt for better categorization
3. ✅ Add column mapping for flexible headers
4. ✅ Implement transaction field extraction from raw rows
5. ✅ Add more file format support if needed

---

**Testing Status:** Ready for Testing  
**Files Provided:** Sample CSV and Excel templates above  
**Expected Result:** Successful extraction with headers included

