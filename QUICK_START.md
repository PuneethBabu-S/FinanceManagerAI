# Quick Start Guide: PDF Statement Processing

## 5-Minute Setup

### Prerequisites
- Docker & Docker Compose
- Java 17+
- Gradle

### Step 1: Start Infrastructure (2 min)
```bash
cd D:\Projects\FinanceManagerAI
docker-compose up -d

# Verify containers running
docker ps | grep -E "(postgres|kafka|elasticsearch|keycloak)"
```

### Step 2: Start Services (1 min per service)
Open 4 separate PowerShell terminals in `D:\Projects\FinanceManagerAI`:

**Terminal 1:**
```bash
cd expense-service && ./gradlew bootRun
# Wait for "Started ExpenseServiceApplication"
```

**Terminal 2:**
```bash
cd genaisvc && ./gradlew bootRun
# Wait for "Started GenaisvcApplication"
```

**Terminal 3:**
```bash
cd file-processing-service && ./gradlew bootRun
# Wait for "Started FileProcessingServiceApplication"
```

### Step 3: Test the System (2 min)

#### 3a. Check Health
```bash
# All should return: {"status":"UP"}
curl http://localhost:8082/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8083/actuator/health
```

#### 3b. Get JWT Token (from Keycloak)
1. Go to http://localhost:8080/
2. Login with admin/admin
3. Navigate to Realm Settings → Export Realm
4. Create a test user (or use via API)

Or use a test token (configure in Keycloak first):
```bash
# Replace values with your Keycloak setup
TOKEN=$(curl -s -X POST \
  http://localhost:8080/realms/finance-manager/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=finance-client&username=testuser&password=testpass&grant_type=password" \
  | jq -r '.access_token')

echo $TOKEN
```

#### 3c. Upload PDF Statement
```bash
# Create a sample PDF first (see Sample PDF Format below)
# Or use an actual bank statement in text format

curl -X POST http://localhost:8083/api/statements/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@statement.pdf"
```

#### 3d. Expected Response
```json
{
  "statementId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "status": "COMPLETED",
  "totalTransactionsExtracted": 5,
  "totalTransactionsCategorized": 5,
  "totalExpensesCreated": 5,
  "message": "Statement processed successfully",
  "errors": []
}
```

#### 3e. Verify Expenses Created
```bash
curl -X GET http://localhost:8082/api/expenses \
  -H "Authorization: Bearer $TOKEN"
```

## Sample PDF for Testing

### Using Python to Create Sample PDF
```python
from reportlab.lib.pagesizes import letter
from reportlab.pdfgen import canvas

c = canvas.Canvas("sample_statement.pdf", pagesize=letter)
c.setFont("Helvetica", 12)

# Title
c.drawString(50, 750, "Bank Statement - March 2026")

# Transactions
y = 700
transactions = [
    "03/03/2026 Amazon Purchase                    1,250.50",
    "04/03/2026 Grocery Store                        450.25",
    "05/03/2026 Fuel Station                       2,000.00",
    "06/03/2026 Restaurant Dining                    650.75",
    "07/03/2026 Electric Utility Bill              1,500.00",
]

for tx in transactions:
    c.drawString(50, y, tx)
    y -= 30

c.save()
```

### Or Use SimpleText
Create `statement.txt`:
```
03/03/2026 Amazon Purchase                    1250.50
04/03/2026 Grocery Store                      450.25
05/03/2026 Fuel Station                      2000.00
06/03/2026 Restaurant Dining                  650.75
07/03/2026 Electric Utility Bill             1500.00
```

Then convert to PDF using any text-to-PDF tool.

## Troubleshooting

### "Connection refused" on port 8083
- Verify file-processing-service is running
- Check logs in Terminal 3
- Ensure Spring Boot started successfully

### "401 Unauthorized"
- Check JWT token is valid
- Token should start with "Bearer "
- Token must not be expired

### "Category not found"
- Create categories in Expense Service first:
```bash
curl -X POST http://localhost:8082/api/categories \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Groceries",
    "description": "Grocery shopping",
    "isGlobal": true
  }'
```

### "No transactions found in the PDF"
- Ensure PDF is text-based (not scanned image)
- Check transaction format matches expected pattern
- Verify date format is dd/MM/yyyy

### GenAI Service Returns Errors
- Check GEMINI_API_KEY is configured
- Verify internet connection
- Check GenAI Service logs

## Configuration Files

### If Using Different Ports

Edit `file-processing-service/src/main/resources/application.yaml`:
```yaml
expense-service:
  url: http://localhost:YOUR_PORT  # Change if needed
  
genai-service:
  url: http://localhost:YOUR_PORT  # Change if needed
```

## Useful Commands

### Check Service Logs
```bash
# Follow logs in real-time
# In service terminal, use Ctrl+C to stop

# Or check Spring Boot console output
```

### Stop All Services
```bash
# In each terminal, press Ctrl+C
# Then stop Docker containers:
docker-compose down
```

### View Database
```bash
# Connect to PostgreSQL
psql -h localhost -p 5433 -U postgres -d file_processing_db

# Or use pgAdmin if running
```

### Clear Data & Restart
```bash
# Stop everything
docker-compose down -v  # Remove volumes

# Restart
docker-compose up -d

# Rebuild services
cd file-processing-service && ./gradlew clean build
```

## What's Happening Behind the Scenes

```
1. PDF Upload
   ↓
2. PDFBox extracts text
   ↓
3. Regex parses transactions (date, amount, description)
   ↓
4. Fetch available categories from Expense Service
   ↓
5. Batch transactions (20 per batch)
   ↓
6. Send to GenAI for categorization
   ↓
7. Map category names to IDs
   ↓
8. Create expenses in Expense Service
   ↓
9. Return summary with counts
```

## Next Steps

### Once Basic Setup Works:
1. **Test with Real Bank Statement** (redact sensitive info)
2. **Experiment with Different Prompts** in CategorizationService
3. **Add Custom Categories** for your use case
4. **Configure Email Notifications** (future feature)
5. **Integrate with UI** (future feature)

### For Development:
- Add unit tests: `src/test/java`
- Enable debug logging in `application.yaml`
- Add custom bank parsers in PDFExtractionService
- Implement manual review for low-confidence categories

## Support

If you encounter issues:
1. Check `IMPLEMENTATION_SUMMARY.md` for detailed architecture
2. See `file-processing-service/README.md` for service docs
3. Review logs in service terminals
4. Check Docker containers: `docker ps -a`
5. Verify network: `docker network ls`

---

**Happy Processing! 🎉**

