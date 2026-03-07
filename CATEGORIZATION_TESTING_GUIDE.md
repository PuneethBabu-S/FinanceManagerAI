# Transaction Categorization Testing Guide

## Prerequisites

1. **All Services Running**:
   - Keycloak (port 8080)
   - expense-service (port 8082)
   - file-processing-service (port 8083)
   - genaisvc (port 8089)

2. **Environment Variables**:
   - `GEMINI_API_KEY` set for genaisvc

3. **Test Data**: Create a CSV file with transactions

## Test CSV File Example

Create `test-transactions.csv`:

```csv
Date,Description,Category,Amount,Currency,Payment Method,Tags
2026-03-06,Starbucks Coffee,,25.50,INR,DEBIT_CARD,
2026-03-05,Uber Ride,,150.00,INR,UPI,
2026-03-04,Amazon Shopping,,1200.00,INR,CREDIT_CARD,
2026-03-03,Dominos Pizza,,450.00,INR,CASH,
```

## Testing Steps

### 1. Get Authentication Token

```powershell
$tokenResponse = Invoke-RestMethod -Uri "http://localhost:8080/realms/finance-manager/protocol/openid-connect/token" `
  -Method POST `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    grant_type = "password"
    client_id = "finance-app"
    username = "testuser"
    password = "password"
  }

$token = $tokenResponse.access_token
Write-Host "Token: $token"
```

### 2. Upload Statement for Categorization

```powershell
$headers = @{
    "Authorization" = "Bearer $token"
}

$fileBytes = [System.IO.File]::ReadAllBytes("D:\test-transactions.csv")
$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"

$bodyLines = @(
    "--$boundary",
    "Content-Disposition: form-data; name=`"file`"; filename=`"test-transactions.csv`"",
    "Content-Type: text/csv$LF",
    [System.Text.Encoding]::UTF8.GetString($fileBytes),
    "--$boundary--$LF"
) -join $LF

$response = Invoke-RestMethod -Uri "http://localhost:8083/api/statements/upload" `
  -Method POST `
  -Headers $headers `
  -ContentType "multipart/form-data; boundary=$boundary" `
  -Body $bodyLines

$response | ConvertTo-Json -Depth 10
```

## Expected Response

```json
{
  "statementId": "uuid-here",
  "status": "CATEGORIZED",
  "totalTransactionsExtracted": 4,
  "totalTransactionsCategorized": 4,
  "totalExpensesCreated": 0,
  "errors": [],
  "message": "Statement processed and categorized successfully. Ready for UI validation before expense creation.",
  "categorizedTransactions": [
    {
      "transaction": {
        "date": "2026-03-06",
        "amount": 25.50,
        "description": "Starbucks Coffee",
        "merchant": null,
        "paymentMethod": "DEBIT_CARD",
        "currency": "INR",
        "rowValues": ["2026-03-06", "Starbucks Coffee", "", "25.50", "INR", "DEBIT_CARD", ""],
        "headerRow": false
      },
      "suggestedCategory": "Dining",
      "confidence": "HIGH"
    },
    {
      "transaction": {
        "date": "2026-03-05",
        "amount": 150.00,
        "description": "Uber Ride",
        "merchant": null,
        "paymentMethod": "UPI",
        "currency": "INR",
        "rowValues": ["2026-03-05", "Uber Ride", "", "150.00", "INR", "UPI", ""],
        "headerRow": false
      },
      "suggestedCategory": "Transportation",
      "confidence": "HIGH"
    },
    {
      "transaction": {
        "date": "2026-03-04",
        "amount": 1200.00,
        "description": "Amazon Shopping",
        "merchant": null,
        "paymentMethod": "CREDIT_CARD",
        "currency": "INR",
        "rowValues": ["2026-03-04", "Amazon Shopping", "", "1200.00", "INR", "CREDIT_CARD", ""],
        "headerRow": false
      },
      "suggestedCategory": "Shopping",
      "confidence": "MEDIUM"
    },
    {
      "transaction": {
        "date": "2026-03-03",
        "amount": 450.00,
        "description": "Dominos Pizza",
        "merchant": null,
        "paymentMethod": "CASH",
        "currency": "INR",
        "rowValues": ["2026-03-03", "Dominos Pizza", "", "450.00", "INR", "CASH", ""],
        "headerRow": false
      },
      "suggestedCategory": "Dining",
      "confidence": "HIGH"
    }
  ]
}
```

## Validation Checklist

- [ ] No header row in categorizedTransactions
- [ ] totalTransactionsExtracted matches actual data rows (not including header)
- [ ] Each transaction has a valid suggestedCategory
- [ ] Each transaction has a confidence score (HIGH/MEDIUM/LOW)
- [ ] totalExpensesCreated is 0 (expenses not created yet)
- [ ] status is "CATEGORIZED"
- [ ] No errors in errors array

## Common Issues & Solutions

### Issue 1: "Header row included in categorization"
**Solution**: Check that FileExtractionService correctly marks header rows with `isHeaderRow = true`

### Issue 2: "Categories have extra whitespace or newlines"
**Solution**: Verify CategorizationService.parseCategorizationResponse() properly trims responses

### Issue 3: "Authentication failed"
**Solution**: 
- Check token is valid and not expired
- Verify all services have correct Keycloak configuration
- Ensure token is passed as "Bearer <token>"

### Issue 4: "GenAI service returns plain text instead of JSON"
**Solution**: 
- Check GenAI prompt requests JSON format
- Verify GenAI service has GEMINI_API_KEY configured
- Review GenAI response in logs

### Issue 5: "Wrong number of categorized transactions"
**Solution**: 
- Check that response parsing correctly matches lines to transactions
- Verify empty lines are filtered out
- Review logs for parsing details

## Debugging

Enable debug logging in application.yaml:

```yaml
logging:
  level:
    com.financemanagerai: DEBUG
```

Check logs for:
- "Starting categorization for X transactions"
- "Filtered X data rows (excluded Y header rows)"
- "Parsed X valid response lines for Y transactions"
- "Parsed JSON for transaction N: category=X, confidence=Y"

## Next Steps

After successful categorization:
1. UI validates suggested categories
2. User can modify categories if needed
3. UI calls expense-service to create expenses
4. File-processing-service does NOT create expenses automatically

