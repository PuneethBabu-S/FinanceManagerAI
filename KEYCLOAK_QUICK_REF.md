# 🔐 Keycloak Quick Reference

## Essential Information

### Keycloak Access
- **Admin Console**: http://localhost:8080
- **Admin User**: admin
- **Admin Password**: admin
- **Realm**: finance-manager

### Pre-configured Test Users

| Username   | Password  | Role        | Email                  |
|------------|-----------|-------------|------------------------|
| testuser   | password  | USER        | testuser@example.com   |
| adminuser  | adminpass | ADMIN, USER | admin@example.com      |

## Get JWT Token (PowerShell)

```powershell
# Test User
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
$env:AUTH_TOKEN = $token
Write-Host $token
```

## Use Token in API Calls

```powershell
# Set headers
$headers = @{ Authorization = "Bearer $env:AUTH_TOKEN" }

# File Processing Service (Port 8083)
Invoke-RestMethod -Uri "http://localhost:8083/api/statements/upload" `
  -Headers $headers -Method Post `
  -Form @{ file = Get-Item "statement.pdf" }

# Expense Service (Port 8082)
Invoke-RestMethod -Uri "http://localhost:8082/api/expenses" `
  -Headers $headers -Method Get

# User Service (Port 8081)
Invoke-RestMethod -Uri "http://localhost:8081/api/users/profile" `
  -Headers $headers -Method Get
```

## Run Automated Test

```powershell
.\test-keycloak-auth.ps1
```

This will:
- ✅ Check Keycloak connectivity
- ✅ Verify realm configuration
- ✅ Get tokens for both users
- ✅ Decode and display JWT claims
- ✅ Set environment variables for immediate use

## Service Configuration

All services are configured with:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/finance-manager
```

## Client Configurations

### finance-client (Public Client)
- **Type**: Public Client
- **Purpose**: User authentication
- **Grants**: Password, Authorization Code
- **Returns**: JWT tokens with user claims

### Service Clients (Bearer Only)
- file-processing-service
- expense-service  
- user-service
- **Type**: Resource Servers
- **Purpose**: Validate JWT tokens
- **Access**: Bearer token in Authorization header

## JWT Token Structure

```json
{
  "header": {
    "alg": "RS256",
    "typ": "JWT",
    "kid": "..."
  },
  "payload": {
    "iss": "http://localhost:8080/realms/finance-manager",
    "sub": "user-uuid",
    "preferred_username": "testuser",
    "email": "testuser@example.com",
    "role": "USER",
    "exp": 1234567890,
    "iat": 1234564290
  }
}
```

## Common Issues

### ❌ 401 Unauthorized
- Token expired (get new token)
- Wrong token format (must be: `Bearer <token>`)
- Service can't reach Keycloak

### ❌ 403 Forbidden
- Token valid but user lacks required role
- Check role mapping in Keycloak

### ❌ Connection Refused
- Keycloak not running: `docker-compose up keycloak`
- Wrong port (should be 8080)

## Useful Commands

```powershell
# Start Keycloak
docker-compose up keycloak -d

# Check Keycloak logs
docker-compose logs keycloak -f

# Stop Keycloak
docker-compose stop keycloak

# Restart with fresh import
docker-compose down keycloak
docker-compose up keycloak -d

# Verify realm file
Get-Content .\finance-manager-realm.json | ConvertFrom-Json | ConvertTo-Json -Depth 10

# Decode JWT token (basic)
$token = "eyJ..."
$parts = $token.Split('.')
$payload = $parts[1]
while ($payload.Length % 4 -ne 0) { $payload += "=" }
[System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($payload)) | ConvertFrom-Json | ConvertTo-Json
```

## Resources

- 📖 Full Documentation: `KEYCLOAK_AUTH_SETUP.md`
- 🧪 Test Script: `test-keycloak-auth.ps1`
- 🔍 JWT Decoder: https://jwt.io
- 📚 Keycloak Docs: https://www.keycloak.org/documentation

