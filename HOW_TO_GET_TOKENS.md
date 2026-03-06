# 🚀 How to Get Tokens and Make API Calls - Step by Step

## Prerequisites

Start Keycloak first:
```powershell
docker-compose up keycloak -d
```

Wait 30 seconds for it to start, then continue...

---

## Method 1: Automated (Easiest) ⭐

### Run the Test Script

```powershell
.\test-keycloak-auth.ps1
```

This will:
- ✅ Get tokens for both users
- ✅ Save them to environment variables
- ✅ Show token details
- ✅ Ready to use immediately!

**Result**: 
- `$env:TEST_USER_TOKEN` - Token for testuser (USER role)
- `$env:ADMIN_USER_TOKEN` - Token for adminuser (ADMIN role)

---

## Method 2: Manual (Step by Step)

### Step 1: Get Token for Test User

```powershell
# Make the token request
$response = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/realms/finance-manager/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    client_id = "finance-client"
    username = "testuser"
    password = "password"
    grant_type = "password"
  }

# Save the token
$token = $response.access_token
$env:AUTH_TOKEN = $token

# View the token
Write-Host $token
```

### Step 2: Get Token for Admin User

```powershell
$adminResponse = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/realms/finance-manager/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    client_id = "finance-client"
    username = "adminuser"
    password = "adminpass"
    grant_type = "password"
  }

$adminToken = $adminResponse.access_token
$env:ADMIN_TOKEN = $adminToken
```

---

## Making API Calls with Your Token

### Example 1: Upload PDF Statement (File Processing Service - Port 8083)

```powershell
# Set authorization header
$headers = @{ Authorization = "Bearer $env:AUTH_TOKEN" }

# Upload a PDF file
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8083/api/statements/upload" `
  -Headers $headers `
  -Form @{ file = Get-Item "C:\path\to\your\bank_statement.pdf" }
```

### Example 2: Get All Expenses (Expense Service - Port 8082)

```powershell
# Set authorization header
$headers = @{ Authorization = "Bearer $env:AUTH_TOKEN" }

# Get expenses for the authenticated user
$expenses = Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8082/api/expenses" `
  -Headers $headers

# Display results
$expenses | Format-Table
```

### Example 3: Create a New Expense

```powershell
$headers = @{ Authorization = "Bearer $env:AUTH_TOKEN" }

$newExpense = @{
  amount = 50.00
  category = "Groceries"
  description = "Weekly shopping"
  date = (Get-Date).ToString("yyyy-MM-dd")
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8082/api/expenses" `
  -Headers $headers `
  -ContentType "application/json" `
  -Body $newExpense
```

### Example 4: Get User Profile (User Service - Port 8081)

```powershell
$headers = @{ Authorization = "Bearer $env:AUTH_TOKEN" }

$profile = Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8081/api/users/profile" `
  -Headers $headers

$profile
```

### Example 5: Admin Endpoint (Requires ADMIN Role)

```powershell
# Use admin token
$adminHeaders = @{ Authorization = "Bearer $env:ADMIN_TOKEN" }

# Get all users (admin only)
$allUsers = Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8081/api/admin/users" `
  -Headers $adminHeaders

$allUsers | Format-Table
```

---

## Complete Workflow Example

Here's a complete example workflow:

```powershell
# 1. Get authentication token
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
Write-Host "✅ Got token: $($token.Substring(0,50))..." -ForegroundColor Green

# 2. Set up headers for all requests
$headers = @{ 
  Authorization = "Bearer $token"
  "Content-Type" = "application/json"
}

# 3. Upload a bank statement
Write-Host "`n📄 Uploading PDF statement..." -ForegroundColor Cyan
$uploadResult = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8083/api/statements/upload" `
  -Headers @{ Authorization = "Bearer $token" } `
  -Form @{ file = Get-Item "bank_statement.pdf" }

Write-Host "✅ Upload successful!" -ForegroundColor Green
$uploadResult

# 4. Check the expenses created from the PDF
Write-Host "`n💰 Fetching expenses..." -ForegroundColor Cyan
$expenses = Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8082/api/expenses" `
  -Headers $headers

Write-Host "✅ Found $($expenses.Count) expenses" -ForegroundColor Green
$expenses | Select-Object date, merchant, amount, category | Format-Table

# 5. Get spending summary
Write-Host "`n📊 Getting spending summary..." -ForegroundColor Cyan
$summary = Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8082/api/expenses/summary?startDate=2024-01-01&endDate=2024-12-31" `
  -Headers $headers

$summary
```

---

## Debugging: Decode Your Token

Want to see what's inside your token?

### Method 1: Using JWT.io (Online)

```powershell
# Copy token to clipboard
$env:AUTH_TOKEN | Set-Clipboard

# Or display it
Write-Host $env:AUTH_TOKEN

# Then paste at https://jwt.io
```

### Method 2: PowerShell Decode (Basic)

```powershell
# Decode JWT payload
$token = $env:AUTH_TOKEN
$parts = $token.Split('.')
$payload = $parts[1]

# Add padding if needed
while ($payload.Length % 4 -ne 0) { 
  $payload += "=" 
}

# Decode from Base64
$decoded = [System.Text.Encoding]::UTF8.GetString(
  [System.Convert]::FromBase64String($payload)
)

# Parse JSON
$claims = $decoded | ConvertFrom-Json

# Display claims
Write-Host "`nToken Claims:" -ForegroundColor Cyan
$claims | Format-List
```

**Example Output:**
```
iss           : http://localhost:8080/realms/finance-manager
sub           : 12345-67890-abcdef
preferred_username : testuser
email         : testuser@example.com
role          : USER
exp           : 1709845200
iat           : 1709841600
```

---

## Common Scenarios

### Scenario 1: Token Expired (401 Error)

```powershell
# Error: "401 Unauthorized" or "Token expired"

# Solution: Get a fresh token
$response = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/realms/finance-manager/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    client_id = "finance-client"
    username = "testuser"
    password = "password"
    grant_type = "password"
  }

$env:AUTH_TOKEN = $response.access_token
```

### Scenario 2: Insufficient Permissions (403 Error)

```powershell
# Error: "403 Forbidden"

# You're trying to access an admin endpoint with USER role
# Solution: Use admin token instead

$adminResponse = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/realms/finance-manager/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    client_id = "finance-client"
    username = "adminuser"
    password = "adminpass"
    grant_type = "password"
  }

$adminHeaders = @{ Authorization = "Bearer $($adminResponse.access_token)" }
```

### Scenario 3: Testing Multiple Users

```powershell
# Create a function for easier token management
function Get-AuthToken {
    param(
        [string]$Username,
        [string]$Password
    )
    
    $response = Invoke-RestMethod -Method Post `
      -Uri "http://localhost:8080/realms/finance-manager/protocol/openid-connect/token" `
      -ContentType "application/x-www-form-urlencoded" `
      -Body @{
        client_id = "finance-client"
        username = $Username
        password = $Password
        grant_type = "password"
      }
    
    return $response.access_token
}

# Usage
$userToken = Get-AuthToken -Username "testuser" -Password "password"
$adminToken = Get-AuthToken -Username "adminuser" -Password "adminpass"

# Make calls
$userHeaders = @{ Authorization = "Bearer $userToken" }
$adminHeaders = @{ Authorization = "Bearer $adminToken" }
```

---

## Using cURL (Alternative)

If you prefer cURL (in Git Bash or WSL):

### Get Token

```bash
curl -X POST http://localhost:8080/realms/finance-manager/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=finance-client" \
  -d "username=testuser" \
  -d "password=password" \
  -d "grant_type=password"
```

### Extract Token (with jq)

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/realms/finance-manager/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=finance-client&username=testuser&password=password&grant_type=password" \
  | jq -r '.access_token')

echo $TOKEN
```

### Make API Call

```bash
curl -X POST http://localhost:8083/api/statements/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@bank_statement.pdf"
```

---

## Quick Reference Card

### Get Token (Copy-Paste Ready)

```powershell
$response = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/realms/finance-manager/protocol/openid-connect/token" -ContentType "application/x-www-form-urlencoded" -Body @{client_id="finance-client";username="testuser";password="password";grant_type="password"}; $env:AUTH_TOKEN = $response.access_token; Write-Host "Token: $env:AUTH_TOKEN"
```

### Make API Call (Copy-Paste Ready)

```powershell
$headers = @{Authorization="Bearer $env:AUTH_TOKEN"}; Invoke-RestMethod -Method Get -Uri "http://localhost:8082/api/expenses" -Headers $headers
```

---

## Testing Checklist

- [ ] Keycloak is running (`docker-compose ps keycloak`)
- [ ] Got token successfully (no errors)
- [ ] Token saved to environment variable
- [ ] Headers include `Authorization: Bearer <token>`
- [ ] Service is running on correct port
- [ ] Token not expired (valid for 1 hour)

---

## Pro Tips 💡

1. **Save tokens in variables**: Tokens are long - store them in `$env:` variables
2. **Reuse headers**: Create `$headers` once, use in all requests
3. **Check token expiry**: Tokens last 1 hour - get fresh one if needed
4. **Use the test script**: Fastest way to get started
5. **Decode tokens**: Visit jwt.io to see what's inside
6. **Check service logs**: Use `docker-compose logs <service>` if errors occur

---

## Next Steps

1. ✅ Start Keycloak: `docker-compose up keycloak -d`
2. ✅ Run test script: `.\test-keycloak-auth.ps1`
3. ✅ Try the examples above
4. ✅ Build your own workflows

**Need help?** Check the other documentation files:
- `KEYCLOAK_QUICK_REF.md` - Quick reference
- `KEYCLOAK_AUTH_SETUP.md` - Complete guide
- `KEYCLOAK_GETTING_STARTED.md` - Getting started

---

**Ready to start?**

```powershell
# Copy and run this complete example:

# 1. Get token
$r = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/realms/finance-manager/protocol/openid-connect/token" -ContentType "application/x-www-form-urlencoded" -Body @{client_id="finance-client";username="testuser";password="password";grant_type="password"}

# 2. Save token
$env:TOKEN = $r.access_token

# 3. Test it
$h = @{Authorization="Bearer $env:TOKEN"}
Invoke-RestMethod -Method Get -Uri "http://localhost:8082/api/expenses" -Headers $h
```

🎉 **You're all set!**

