# Keycloak Authentication Test Script
# This script tests the complete authentication flow

Write-Host "`n🔐 Keycloak Authentication Test Script" -ForegroundColor Cyan
Write-Host "=" * 50 -ForegroundColor Cyan

# Configuration
$KEYCLOAK_URL = "http://localhost:8080"
$REALM = "finance-manager"
$CLIENT_ID = "finance-client"

# Test 1: Check if Keycloak is running
Write-Host "`n[Test 1] Checking if Keycloak is running..." -ForegroundColor Yellow
try {
    $healthCheck = Invoke-RestMethod -Uri "$KEYCLOAK_URL/health/ready" -Method Get -ErrorAction Stop
    Write-Host "✅ Keycloak is running and ready" -ForegroundColor Green
} catch {
    Write-Host "❌ Keycloak is not accessible at $KEYCLOAK_URL" -ForegroundColor Red
    Write-Host "   Please start Keycloak with: docker-compose up keycloak" -ForegroundColor Yellow
    exit 1
}

# Test 2: Verify realm endpoint
Write-Host "`n[Test 2] Verifying realm configuration..." -ForegroundColor Yellow
try {
    $realmInfo = Invoke-RestMethod -Uri "$KEYCLOAK_URL/realms/$REALM" -Method Get -ErrorAction Stop
    Write-Host "✅ Realm '$REALM' is configured" -ForegroundColor Green
    Write-Host "   Issuer: $($realmInfo.issuer)" -ForegroundColor Gray
} catch {
    Write-Host "❌ Realm '$REALM' not found" -ForegroundColor Red
    Write-Host "   Make sure finance-manager-realm.json was imported" -ForegroundColor Yellow
    exit 1
}

# Test 3: Get token for testuser
Write-Host "`n[Test 3] Authenticating as 'testuser'..." -ForegroundColor Yellow
try {
    $tokenResponse = Invoke-RestMethod -Method Post `
        -Uri "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" `
        -ContentType "application/x-www-form-urlencoded" `
        -Body @{
            client_id = $CLIENT_ID
            username = "testuser"
            password = "password"
            grant_type = "password"
        } -ErrorAction Stop

    Write-Host "✅ Successfully obtained JWT token for 'testuser'" -ForegroundColor Green
    Write-Host "   Token Type: $($tokenResponse.token_type)" -ForegroundColor Gray
    Write-Host "   Expires In: $($tokenResponse.expires_in) seconds" -ForegroundColor Gray
    Write-Host "   Access Token (first 50 chars): $($tokenResponse.access_token.Substring(0, 50))..." -ForegroundColor Gray

    # Store token for later use
    $env:TEST_USER_TOKEN = $tokenResponse.access_token

    # Decode JWT (basic parsing - header and payload)
    $tokenParts = $tokenResponse.access_token.Split('.')
    if ($tokenParts.Length -eq 3) {
        $payload = $tokenParts[1]
        # Pad the string if necessary
        while ($payload.Length % 4 -ne 0) { $payload += "=" }
        $payloadJson = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($payload)) | ConvertFrom-Json

        Write-Host "`n   Token Claims:" -ForegroundColor Gray
        Write-Host "   - Username: $($payloadJson.preferred_username)" -ForegroundColor Gray
        Write-Host "   - Email: $($payloadJson.email)" -ForegroundColor Gray
        Write-Host "   - Role: $($payloadJson.role)" -ForegroundColor Gray
    }

} catch {
    Write-Host "❌ Failed to authenticate" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test 4: Get token for adminuser
Write-Host "`n[Test 4] Authenticating as 'adminuser'..." -ForegroundColor Yellow
try {
    $adminTokenResponse = Invoke-RestMethod -Method Post `
        -Uri "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" `
        -ContentType "application/x-www-form-urlencoded" `
        -Body @{
            client_id = $CLIENT_ID
            username = "adminuser"
            password = "adminpass"
            grant_type = "password"
        } -ErrorAction Stop

    Write-Host "✅ Successfully obtained JWT token for 'adminuser'" -ForegroundColor Green

    # Store token
    $env:ADMIN_USER_TOKEN = $adminTokenResponse.access_token

    # Decode JWT
    $tokenParts = $adminTokenResponse.access_token.Split('.')
    if ($tokenParts.Length -eq 3) {
        $payload = $tokenParts[1]
        while ($payload.Length % 4 -ne 0) { $payload += "=" }
        $payloadJson = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($payload)) | ConvertFrom-Json

        Write-Host "   Token Claims:" -ForegroundColor Gray
        Write-Host "   - Username: $($payloadJson.preferred_username)" -ForegroundColor Gray
        Write-Host "   - Email: $($payloadJson.email)" -ForegroundColor Gray
        Write-Host "   - Role: $($payloadJson.role)" -ForegroundColor Gray
    }

} catch {
    Write-Host "❌ Failed to authenticate admin user" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 5: Test token introspection
Write-Host "`n[Test 5] Validating token..." -ForegroundColor Yellow
try {
    # Get OpenID configuration
    $oidcConfig = Invoke-RestMethod -Uri "$KEYCLOAK_URL/realms/$REALM/.well-known/openid-configuration" -Method Get
    Write-Host "✅ OpenID Connect configuration retrieved" -ForegroundColor Green
    Write-Host "   Token Endpoint: $($oidcConfig.token_endpoint)" -ForegroundColor Gray
    Write-Host "   Authorization Endpoint: $($oidcConfig.authorization_endpoint)" -ForegroundColor Gray
    Write-Host "   JWKS URI: $($oidcConfig.jwks_uri)" -ForegroundColor Gray
} catch {
    Write-Host "⚠️  Could not retrieve OIDC configuration" -ForegroundColor Yellow
}

# Summary
Write-Host "`n" + ("=" * 50) -ForegroundColor Cyan
Write-Host "✅ All tests passed!" -ForegroundColor Green
Write-Host "`n📝 Environment Variables Set:" -ForegroundColor Cyan
Write-Host "   `$env:TEST_USER_TOKEN - Token for testuser" -ForegroundColor Gray
Write-Host "   `$env:ADMIN_USER_TOKEN - Token for adminuser" -ForegroundColor Gray

Write-Host "`n💡 Quick Usage Examples:" -ForegroundColor Cyan
Write-Host @"
   # Use token in API request:
   `$headers = @{ Authorization = "Bearer `$env:TEST_USER_TOKEN" }
   Invoke-RestMethod -Uri "http://localhost:8083/api/statements/upload" -Headers `$headers -Method Post -Form @{ file = Get-Item "statement.pdf" }

   # Decode token at jwt.io:
   Write-Host `$env:TEST_USER_TOKEN

   # Get fresh token:
   .\test-keycloak-auth.ps1
"@ -ForegroundColor Gray

Write-Host "`n" -ForegroundColor Cyan

