# ✅ Keycloak Authentication - Getting Started Checklist

## 🚀 Quick Start (5 Minutes)

Follow these steps to get authentication working in your Finance Manager application.

### Step 1: Start Keycloak ⏱️ ~2 minutes

```powershell
# Navigate to project directory
cd D:\Projects\FinanceManagerAI

# Start Keycloak (it will auto-import the realm configuration)
docker-compose up keycloak -d

# Wait for Keycloak to be ready (check logs)
docker-compose logs keycloak -f
# Wait until you see: "Running the server in development mode. DO NOT use this configuration in production."
# Press Ctrl+C to exit logs
```

**✅ Verify**: Visit http://localhost:8080 - You should see Keycloak login page

### Step 2: Run Authentication Test ⏱️ ~1 minute

```powershell
# Run the automated test script
.\test-keycloak-auth.ps1
```

**Expected Output**:
```
🔐 Keycloak Authentication Test Script
==================================================

[Test 1] Checking if Keycloak is running...
✅ Keycloak is running and ready

[Test 2] Verifying realm configuration...
✅ Realm 'finance-manager' is configured

[Test 3] Authenticating as 'testuser'...
✅ Successfully obtained JWT token for 'testuser'

[Test 4] Authenticating as 'adminuser'...
✅ Successfully obtained JWT token for 'adminuser'

[Test 5] Validating token...
✅ OpenID Connect configuration retrieved

==================================================
✅ All tests passed!
```

### Step 3: Use Tokens in API Calls ⏱️ ~2 minutes

The test script sets environment variables with tokens. Use them immediately:

```powershell
# Example: Upload a PDF statement
$headers = @{ Authorization = "Bearer $env:TEST_USER_TOKEN" }

Invoke-RestMethod -Uri "http://localhost:8083/api/statements/upload" `
  -Headers $headers `
  -Method Post `
  -Form @{ file = Get-Item "path\to\your\statement.pdf" }
```

**✅ Done!** You're now using JWT authentication!

---

## 📚 What You Get

### 1. Pre-configured Users

| Username   | Password  | Role  | Use Case           |
|------------|-----------|-------|--------------------|
| testuser   | password  | USER  | Testing user flows |
| adminuser  | adminpass | ADMIN | Testing admin APIs |

### 2. Ready-to-Use Tokens

After running the test script:
- `$env:TEST_USER_TOKEN` - USER role token
- `$env:ADMIN_USER_TOKEN` - ADMIN role token

### 3. Protected Services

All these services now require authentication:
- **User Service** (8081): User management
- **Expense Service** (8082): Expense operations  
- **File Processing Service** (8083): PDF processing
- **GenAI Service** (8084): AI categorization

---

## 🔄 Common Workflows

### Get Fresh Token (Token Expired)

```powershell
# Just re-run the test script
.\test-keycloak-auth.ps1

# Or manually:
$response = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/realms/finance-manager/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    client_id = "finance-client"
    username = "testuser"
    password = "password"
    grant_type = "password"
  }

$env:TEST_USER_TOKEN = $response.access_token
```

### View Token Contents

```powershell
# Copy token
Write-Host $env:TEST_USER_TOKEN

# Paste at https://jwt.io to see decoded claims
```

### Add New User

```powershell
# Option 1: Via Keycloak Admin UI
# 1. Go to http://localhost:8080
# 2. Login with admin/admin
# 3. Select "finance-manager" realm
# 4. Users → Add User

# Option 2: Edit finance-manager-realm.json and restart
```

### Test Different Roles

```powershell
# Use testuser token (USER role)
$userHeaders = @{ Authorization = "Bearer $env:TEST_USER_TOKEN" }
Invoke-RestMethod -Uri "http://localhost:8082/api/expenses" -Headers $userHeaders

# Use adminuser token (ADMIN role)
$adminHeaders = @{ Authorization = "Bearer $env:ADMIN_USER_TOKEN" }
Invoke-RestMethod -Uri "http://localhost:8082/admin/expenses" -Headers $adminHeaders
```

---

## 🎯 Understanding the Setup

### What Happens When You Start Keycloak?

1. **Docker Compose** starts Keycloak container
2. **Auto-import** reads `finance-manager-realm.json`
3. **Creates** realm "finance-manager"
4. **Configures** 4 clients (finance-client + 3 services)
5. **Adds** 2 roles (USER, ADMIN)
6. **Creates** 2 users (testuser, adminuser)
7. **Sets up** protocol mappers for JWT claims

### What Happens When You Get a Token?

```
You → Keycloak Token Endpoint
      POST: username + password + client_id

Keycloak → Validates credentials
        → Looks up user roles
        → Creates JWT with claims
        → Signs with private key
        → Returns access_token

You → Use token in API calls
      Authorization: Bearer <token>

Service → Validates signature with Keycloak public key
        → Extracts username, email, role from JWT
        → Checks expiration
        → Grants/denies access
```

### JWT Token Contains

```json
{
  "iss": "http://localhost:8080/realms/finance-manager",  // Who issued it
  "sub": "user-uuid",                                      // User ID
  "preferred_username": "testuser",                        // Username
  "email": "testuser@example.com",                         // Email
  "role": "USER",                                          // User role
  "exp": 1709845200,                                       // Expiration (1 hour)
  "iat": 1709841600                                        // Issued at
}
```

---

## 📖 Documentation Reference

- **Quick Start**: This file (KEYCLOAK_GETTING_STARTED.md)
- **Quick Reference**: [KEYCLOAK_QUICK_REF.md](./KEYCLOAK_QUICK_REF.md)
- **Complete Guide**: [KEYCLOAK_AUTH_SETUP.md](./KEYCLOAK_AUTH_SETUP.md)
- **Setup Summary**: [KEYCLOAK_SETUP_SUMMARY.md](./KEYCLOAK_SETUP_SUMMARY.md)

---

## ❓ Troubleshooting

### "Connection refused" when running test

**Problem**: Keycloak not running

**Solution**:
```powershell
docker-compose up keycloak -d
# Wait 30 seconds for startup
.\test-keycloak-auth.ps1
```

### "Realm not found"

**Problem**: Realm import failed

**Solution**:
```powershell
# Check if realm file exists and is valid
Get-Content .\finance-manager-realm.json | ConvertFrom-Json

# Restart with fresh import
docker-compose down keycloak
docker-compose up keycloak -d
```

### "Invalid credentials"

**Problem**: Wrong username/password

**Solution**: Use exact credentials:
- testuser / password
- adminuser / adminpass

### "401 Unauthorized" on API call

**Problem**: Token expired or invalid

**Solution**:
```powershell
# Get fresh token
.\test-keycloak-auth.ps1

# Verify token is set
Write-Host $env:TEST_USER_TOKEN
```

---

## 🎓 Next Steps

### Learn More
1. Read [KEYCLOAK_AUTH_SETUP.md](./KEYCLOAK_AUTH_SETUP.md) for advanced topics
2. Explore Keycloak Admin Console at http://localhost:8080
3. Try role-based access control with admin endpoints

### Integrate with Your Code
1. Check service configurations in `application.yaml` files
2. See how JWT validation works in Spring Security
3. Add `@PreAuthorize` annotations for role-based access

### Production Preparation
1. Review security checklist in [KEYCLOAK_AUTH_SETUP.md](./KEYCLOAK_AUTH_SETUP.md)
2. Configure SSL/TLS
3. Use environment variables for secrets
4. Shorten token lifetime

---

## ✨ Tips

💡 **Save time**: Keep the test script output open - tokens are displayed there

💡 **Decode tokens**: Visit https://jwt.io and paste your token to see all claims

💡 **Monitor logs**: Use `docker-compose logs keycloak -f` to see authentication attempts

💡 **Admin console**: http://localhost:8080 (admin/admin) for visual configuration

💡 **Environment variables persist**: Once set by test script, use tokens until PowerShell session ends

---

**Ready to start? Run this now:**

```powershell
# 1. Start Keycloak
docker-compose up keycloak -d

# 2. Test authentication
.\test-keycloak-auth.ps1

# 3. Use tokens!
```

🎉 **Happy coding!**

