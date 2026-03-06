# 🔐 Keycloak Authentication Setup Guide

## Overview

This Finance Manager application uses Keycloak for centralized OAuth2/JWT authentication. All services are configured to validate JWT tokens issued by Keycloak.

## Architecture

### Services & Ports
- **Keycloak**: `http://localhost:8080` - Identity Provider
- **User Service**: `http://localhost:8081` - User management
- **Expense Service**: `http://localhost:8082` - Expense operations
- **File Processing Service**: `http://localhost:8083` - PDF processing
- **GenAI Service**: `http://localhost:8084` - AI categorization

### Authentication Flow
```
User → Login → Keycloak → JWT Token → Service (validates token) → Response
```

## Configuration Details

### Realm: `finance-manager`

#### Clients

1. **finance-client** (Public Client)
   - Used for user authentication
   - Direct Access Grants enabled (password grant type)
   - Returns JWT tokens with user roles

2. **file-processing-service** (Bearer Only)
   - Resource server
   - Validates incoming JWT tokens

3. **expense-service** (Bearer Only)
   - Resource server
   - Validates incoming JWT tokens

4. **user-service** (Bearer Only)
   - Resource server
   - Validates incoming JWT tokens

#### Roles

- **USER**: Standard user access
- **ADMIN**: Administrative privileges

#### Pre-configured Users

| Username    | Password  | Email                  | Roles       |
|-------------|-----------|------------------------|-------------|
| testuser    | password  | testuser@example.com   | USER        |
| adminuser   | adminpass | admin@example.com      | ADMIN, USER |

## Getting Started

### 1. Start Keycloak

```powershell
# Start Keycloak with Docker Compose
docker-compose up keycloak
```

The realm configuration will be automatically imported from `finance-manager-realm.json`.

### 2. Access Keycloak Admin Console

- URL: http://localhost:8080
- Username: `admin`
- Password: `admin`

### 3. Verify Realm Import

1. Login to Keycloak admin console
2. Select "finance-manager" realm from dropdown (top-left)
3. Verify:
   - Clients: finance-client, file-processing-service, expense-service, user-service
   - Roles: USER, ADMIN
   - Users: testuser, adminuser

## Testing Authentication

### Get JWT Token (Password Grant)

#### Using PowerShell

```powershell
# Get token for testuser
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
Write-Host "Access Token: $token"

# Save token for later use
$env:AUTH_TOKEN = $token
```

#### Using cURL (Git Bash/WSL)

```bash
# Get token for testuser
curl -X POST http://localhost:8080/realms/finance-manager/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=finance-client" \
  -d "username=testuser" \
  -d "password=password" \
  -d "grant_type=password"

# Extract token (requires jq)
TOKEN=$(curl -s -X POST http://localhost:8080/realms/finance-manager/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=finance-client&username=testuser&password=password&grant_type=password" \
  | jq -r '.access_token')

echo $TOKEN
```

### Decode JWT Token

Visit https://jwt.io and paste your token to see the decoded payload:

```json
{
  "exp": 1234567890,
  "iat": 1234564290,
  "iss": "http://localhost:8080/realms/finance-manager",
  "sub": "user-uuid",
  "preferred_username": "testuser",
  "email": "testuser@example.com",
  "role": "USER"
}
```

### Use Token in API Requests

#### PowerShell Example

```powershell
# Upload PDF to file-processing-service
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8083/api/statements/upload" `
  -Headers @{ Authorization = "Bearer $env:AUTH_TOKEN" } `
  -Form @{ file = Get-Item ".\bank_statement.pdf" }
```

#### cURL Example

```bash
# Upload PDF
curl -X POST http://localhost:8083/api/statements/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@bank_statement.pdf"
```

## Token Validation

Each service validates JWT tokens using Spring Security OAuth2 Resource Server:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/finance-manager
```

Spring Security automatically:
1. Fetches the public key from Keycloak
2. Validates token signature
3. Checks token expiration
4. Extracts user claims (username, email, roles)

## Role-Based Access Control

### Expense Service Example

```java
@GetMapping("/admin/expenses")
@PreAuthorize("hasRole('ADMIN')")
public List<Expense> getAllExpenses() {
    // Only accessible with ADMIN role
}
```

### File Processing Service Example

```java
@PostMapping("/api/statements/upload")
public ResponseEntity<?> uploadStatement(
    @RequestParam("file") MultipartFile file,
    Authentication authentication) {
    
    String username = authentication.getName(); // From JWT token
    // Process statement for this user only
}
```

## Managing Users

### Add New User via Keycloak Admin UI

1. Go to http://localhost:8080
2. Login as admin
3. Select "finance-manager" realm
4. Click "Users" → "Add user"
5. Fill in details and click "Save"
6. Go to "Credentials" tab → Set password
7. Go to "Role Mappings" tab → Assign roles

### Add User via Realm JSON

Edit `finance-manager-realm.json`:

```json
{
  "users": [
    {
      "username": "newuser",
      "enabled": true,
      "email": "newuser@example.com",
      "emailVerified": true,
      "firstName": "New",
      "lastName": "User",
      "realmRoles": ["USER"],
      "credentials": [
        {
          "type": "password",
          "value": "newpassword",
          "temporary": false
        }
      ]
    }
  ]
}
```

Then restart Keycloak:
```powershell
docker-compose restart keycloak
```

## Troubleshooting

### 401 Unauthorized

**Cause**: Invalid or expired token

**Solution**:
- Get a fresh token from Keycloak
- Verify token hasn't expired (default: 1 hour)
- Check Authorization header format: `Bearer <token>`

### 403 Forbidden

**Cause**: Valid token but insufficient permissions

**Solution**:
- Verify user has required role (USER or ADMIN)
- Check role mappings in Keycloak admin console

### Token Validation Failed

**Cause**: Service can't reach Keycloak or wrong issuer

**Solution**:
- Ensure Keycloak is running on port 8080
- Verify `issuer-uri` in application.yaml matches realm
- Check network connectivity to Keycloak

### Realm Not Found

**Cause**: Realm import failed

**Solution**:
```powershell
# Check Keycloak logs
docker-compose logs keycloak

# Verify realm file is mounted
docker exec keycloak ls -la /opt/keycloak/data/import/
```

## Security Best Practices

### Production Checklist

- [ ] Change default admin password
- [ ] Use HTTPS (set `sslRequired: external`)
- [ ] Disable direct access grants in production
- [ ] Implement refresh token rotation
- [ ] Enable brute force protection (already configured)
- [ ] Use shorter token lifespans
- [ ] Store client secrets securely
- [ ] Enable audit logging
- [ ] Use environment variables for sensitive data
- [ ] Configure proper CORS policies

### Environment-Specific Configuration

**Development** (current):
- Public client with password grants
- Long token lifetime (1 hour)
- SSL not required

**Production** (recommended):
```json
{
  "sslRequired": "external",
  "accessTokenLifespan": 300,
  "clients": [{
    "clientId": "finance-client",
    "publicClient": false,
    "clientAuthenticatorType": "client-secret",
    "secret": "use-strong-random-secret"
  }]
}
```

## Advanced Topics

### Custom Claims

Add custom attributes to JWT tokens by configuring protocol mappers in Keycloak.

### Refresh Tokens

```powershell
# Get refresh token
$response = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/realms/finance-manager/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    client_id = "finance-client"
    grant_type = "refresh_token"
    refresh_token = $response.refresh_token
  }
```

### Service-to-Service Communication

For inter-service calls, use service accounts with client credentials grant:

1. Enable service account in client settings
2. Assign required roles to service account
3. Use client credentials grant to get token

## References

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/)
- [JWT.io Token Debugger](https://jwt.io)
- [OpenID Connect Specification](https://openid.net/connect/)

## Support

For issues or questions:
1. Check Keycloak logs: `docker-compose logs keycloak`
2. Verify service logs for JWT validation errors
3. Test token at https://jwt.io
4. Review this documentation

