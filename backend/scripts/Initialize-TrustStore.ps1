param([string]$CaFile = 'secrets/ca.pem', [string]$StoreFile = 'secrets/aiven-truststore.p12', [string]$PrivateConfig = 'secrets/application-private.properties')
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
if (!(Test-Path -LiteralPath $CaFile)) { throw 'CA certificate file is missing. Download it from the Aiven service overview first.' }
if (Test-Path -LiteralPath $StoreFile) { throw 'Truststore already exists; preserve it or select a new output path.' }
if (!(Test-Path -LiteralPath $PrivateConfig)) { throw 'Local private configuration file is missing.' }
$trustBytes = New-Object byte[] 32
$trustRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$trustRng.GetBytes($trustBytes)
$trustRng.Dispose()
$env:ACM_TRUSTSTORE_PASS = [Convert]::ToBase64String($trustBytes)
    & keytool -importcert -noprompt -alias aiven-service-ca -file $CaFile -keystore $StoreFile -storetype PKCS12 -storepass:env ACM_TRUSTSTORE_PASS 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'CA import failed; no database connection was attempted.' }
    $trustConfigLines = [System.IO.File]::ReadAllLines((Resolve-Path -LiteralPath $PrivateConfig)) | Where-Object { $_ -notmatch '^acm\.db\.truststore-(password|path)=' }
    $trustConfigLines += 'acm.db.truststore-path=' + $StoreFile.Replace('\','/')
    $trustConfigLines += 'acm.db.truststore-password=' + $env:ACM_TRUSTSTORE_PASS
    [System.IO.File]::WriteAllLines((Resolve-Path -LiteralPath $PrivateConfig), $trustConfigLines, (New-Object System.Text.UTF8Encoding($false)))
    Write-Output 'CA imported into a private PKCS12 truststore. Password stored only in the Git-ignored private configuration.'
} finally { Remove-Item Env:ACM_TRUSTSTORE_PASS -ErrorAction SilentlyContinue; Pop-Location }
