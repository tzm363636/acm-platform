param([ValidateSet('check','inspect','migrate','seed','verify','bootstrap-admin','serve')][string]$Action='check', [ValidateSet('mysql-local','aiven')][string]$Profile='mysql-local', [string]$PrivateConfig='secrets/application-private.properties', [string]$AdminConfig='secrets/admin-init.properties')
$ErrorActionPreference='Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
if (!(Test-Path -LiteralPath 'target/platform-0.1.0-SNAPSHOT.jar')) { throw 'Build first: mvn clean package' }
$privateConfigUri = if (Test-Path -LiteralPath $PrivateConfig) { (New-Object System.Uri((Resolve-Path -LiteralPath $PrivateConfig).Path)).AbsoluteUri } else { '' }
$databaseArgs = @('-jar','target/platform-0.1.0-SNAPSHOT.jar',"--spring.profiles.active=$Profile","--acm.db.action=$Action")
if ($privateConfigUri) { $databaseArgs += "--spring.config.additional-location=$privateConfigUri" }
if ($Action -eq 'bootstrap-admin' -and (Test-Path -LiteralPath $AdminConfig)) {
  $adminConfigUri = (New-Object System.Uri((Resolve-Path -LiteralPath $AdminConfig).Path)).AbsoluteUri
  $databaseArgs = $databaseArgs | Where-Object { $_ -notlike '--spring.config.additional-location=*' }
  $databaseArgs += "--spring.config.additional-location=$privateConfigUri,$adminConfigUri"
}
& java @databaseArgs
if ($LASTEXITCODE -ne 0) { throw 'Database command failed. Check local secrets, TLS and database privileges without publishing credentials.' }
} finally { Pop-Location }
