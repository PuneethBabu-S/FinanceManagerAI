# Quick Start Guide - Transaction Categorization API

## Overview

Upload a CSV/Excel file → Extract transactions → Categorize with AI → Receive results with suggested categories → Validate in UI before creating expenses.

---

## Step-by-Step Usage

### 1. Get Authentication Token

```powershell
# Get JWT token from Keycloak
$response = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/realms/finance-manager/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    client_id = "finance-client"
    username = "testuser"
    password = "password"
    grant_type = "password"
  }

$token = $response.access_token
Write-Host "Token: $token"
```

### 2. Prepare CSV/Excel File

Create a file with headers and transaction data:

**Example CSV:**
```
Date,Description,Amount,Merchant
03/01/2026,Amazon Purchase,150.50,Amazon
03/02/2026,Starbucks Coffee,5.50,Starbucks
03/03/2026,Electric Bill,85.00,PowerCorp
03/04/2026,Walmart Groceries,65.25,Walmart
03/05/2026,Uber Ride,22.00,Uber
```

### 3. Upload File for Processing

```powershell
# Set headers with Bearer token
$headers = @{ Authorization = "Bearer $token" }

# Upload file and get categorized transactions
$result = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8083/api/statements/upload" `
  -Headers $headers `
  -Form @{ file = Get-Item "transactions.csv" }

# Display results
$result | ConvertTo-Json -Depth 10
```

### 4. Response Example

```json
{
  "statementId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "CATEGORIZED",
  "totalTransactionsExtracted": 5,
  "totalTransactionsCategorized": 5,
  "totalExpensesCreated": 0,
  "message": "Statement processed and categorized successfully. Ready for UI validation before expense creation.",
  "categorizedTransactions": [
    {
      "transaction": {
        "date": "2026-03-01",
        "amount": 150.50,
        "description": "Amazon Purchase",
        "merchant": "Amazon",
        "paymentMethod": null,
        "currency": null,
        "rowValues": ["03/01/2026", "Amazon Purchase", "150.50", "Amazon"],
        "columnHeaders": ["Date", "Description", "Amount", "Merchant"],
        "isHeaderRow": false
      },
      "suggestedCategory": "Shopping",
      "confidence": "HIGH"
    },
    {
      "transaction": {
        "date": "2026-03-02",
        "amount": 5.50,
        "description": "Starbucks Coffee",
        "merchant": "Starbucks",
        "paymentMethod": null,
        "currency": null,
        "rowValues": ["03/02/2026", "Starbucks Coffee", "5.50", "Starbucks"],
        "columnHeaders": ["Date", "Description", "Amount", "Merchant"],
        "isHeaderRow": false
      },
      "suggestedCategory": "Dining",
      "confidence": "HIGH"
    },
    {
      "transaction": {
        "date": "2026-03-03",
        "amount": 85.00,
        "description": "Electric Bill",
        "merchant": "PowerCorp",
        "paymentMethod": null,
        "currency": null,
        "rowValues": ["03/03/2026", "Electric Bill", "85.00", "PowerCorp"],
        "columnHeaders": ["Date", "Description", "Amount", "Merchant"],
        "isHeaderRow": false
      },
      "suggestedCategory": "Utilities",
      "confidence": "HIGH"
    },
    {
      "transaction": {
        "date": "2026-03-04",
        "amount": 65.25,
        "description": "Walmart Groceries",
        "merchant": "Walmart",
        "paymentMethod": null,
        "currency": null,
        "rowValues": ["03/04/2026", "Walmart Groceries", "65.25", "Walmart"],
        "columnHeaders": ["Date", "Description", "Amount", "Merchant"],
        "isHeaderRow": false
      },
      "suggestedCategory": "Groceries",
      "confidence": "HIGH"
    },
    {
      "transaction": {
        "date": "2026-03-05",
        "amount": 22.00,
        "description": "Uber Ride",
        "merchant": "Uber",
        "paymentMethod": null,
        "currency": null,
        "rowValues": ["03/05/2026", "Uber Ride", "22.00", "Uber"],
        "columnHeaders": ["Date", "Description", "Amount", "Merchant"],
        "isHeaderRow": false
      },
      "suggestedCategory": "Transportation",
      "confidence": "HIGH"
    }
  ],
  "errors": []
}
```

### 5. Parse Response in UI

```javascript
// Display categorized transactions
const transactions = result.categorizedTransactions;
transactions.forEach((item, index) => {
  console.log(`${index + 1}. ${item.transaction.description}`);
  console.log(`   Amount: ${item.transaction.amount}`);
  console.log(`   Category: ${item.suggestedCategory} (${item.confidence})`);
  console.log(`   Date: ${item.transaction.date}`);
  console.log('---');
});
```

### 6. User Validates in UI

The UI should display:
- Transaction date
- Description/merchant
- Amount
- **Suggested category** (editable dropdown)
- **Confidence score** (HIGH/MEDIUM/LOW)
- Checkbox to select/deselect

User can:
- ✓ Accept suggested category
- ✓ Change category using dropdown
- ✓ Review all transactions before confirming

### 7. Submit Validated Transactions (Future)

```powershell
# After user validation, submit to create expenses
# This endpoint will be implemented next

$validatedTransactions = @{
  statementId = "550e8400-e29b-41d4-a716-446655440000"
  transactions = @(
    @{
      date = "2026-03-01"
      amount = 150.50
      description = "Amazon Purchase"
      merchant = "Amazon"
      categoryId = 3  # Shopping category ID from Expense Service
    },
    # ... more transactions
  )
}

$result = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8083/api/statements/validate" `
  -Headers $headers `
  -ContentType "application/json" `
  -Body ($validatedTransactions | ConvertTo-Json)
```

---

## Available Categories

Default categories returned from Expense Service:
- Groceries
- Utilities
- Transportation
- Entertainment
- Dining
- Shopping
- Others

---

## Error Handling

### No Token Provided
```
Error: 401 Unauthorized
Solution: Include Authorization header with Bearer token
```

### Invalid Token
```
Error: 401 Unauthorized
Solution: Get new token from Keycloak
```

### Empty File
```
Response Status: FAILED
Message: "No transactions found in the file"
Solution: Ensure file has headers and data rows
```

### GenAI Service Unavailable
```
Response Status: CATEGORIZED
Categorized with default category
Solution: Check GenAI service health
```

---

## File Format Requirements

### CSV Files
- ✓ Headers in first row
- ✓ Data starting from row 2
- ✓ UTF-8 encoding
- ✓ Standard CSV format with commas or semicolons

### Excel Files (.xlsx)
- ✓ Headers in first row of first sheet
- ✓ Data starting from row 2
- ✓ Single sheet preferred

### Required Columns
Minimum one of:
- Date column (for transaction date)
- Description or Merchant column (for categorization)
- Amount column (for transaction amount)

---

## Sample PowerShell Script

```powershell
# Complete workflow
param(
    [string]$Username = "testuser",
    [string]$Password = "password",
    [string]$FilePath = "transactions.csv"
)

# 1. Get token
Write-Host "Getting authentication token..." -ForegroundColor Cyan
$response = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/realms/finance-manager/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    client_id = "finance-client"
    username = $Username
    password = $Password
    grant_type = "password"
  }

$token = $response.access_token
Write-Host "✓ Token acquired" -ForegroundColor Green

# 2. Upload and categorize
Write-Host "`nProcessing file: $FilePath" -ForegroundColor Cyan
$headers = @{ Authorization = "Bearer $token" }

$result = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8083/api/statements/upload" `
  -Headers $headers `
  -Form @{ file = Get-Item $FilePath }

# 3. Display results
Write-Host "`n✓ Processing complete!" -ForegroundColor Green
Write-Host "Statement ID: $($result.statementId)"
Write-Host "Status: $($result.status)"
Write-Host "Extracted: $($result.totalTransactionsExtracted)"
Write-Host "Categorized: $($result.totalTransactionsCategorized)"
Write-Host "`nCategorized Transactions:"
Write-Host "─" * 80

foreach ($tx in $result.categorizedTransactions) {
    $transaction = $tx.transaction
    Write-Host "$($transaction.date) | $($transaction.description)" -ForegroundColor Yellow
    Write-Host "  Amount: $($transaction.amount) | Category: $($tx.suggestedCategory) ($($tx.confidence))"
    Write-Host "─" * 80
}

if ($result.errors.Count -gt 0) {
    Write-Host "`nErrors:" -ForegroundColor Red
    $result.errors | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
}
```

---

## Common Issues & Solutions

### Issue: "Column Headers" not showing in categorization

**Solution:** Ensure the first row of your file contains column headers. The system automatically detects headers.

### Issue: Wrong categories suggested

**Solution:** 
- Check that description/merchant field clearly identifies the transaction type
- Try more descriptive text (e.g., "Amazon.com Purchase" instead of just "Amazon")
- Verify available categories match your transaction types

### Issue: Connection timeout

**Solution:**
- Verify all services are running (Keycloak, File Processing, GenAI, Expense Service)
- Check firewall/network settings
- Verify URLs in application.yaml are correct

### Issue: 400 Bad Request on file upload

**Solution:**
- Ensure file is actual CSV/Excel format (not DOCX, PDF, etc.)
- Check file encoding is UTF-8
- Verify file is not corrupted

---

## Performance Notes

- **Single file:** ~2-5 seconds (depends on GenAI response time)
- **50 transactions:** ~3-8 seconds
- **100+ transactions:** Consider batch processing (coming soon)

---

## What's Next?

1. UI displays categorized transactions
2. User can edit categories
3. User submits validated transactions
4. Expenses are bulk created in Expense Service
5. Confirmation with created expense IDs


