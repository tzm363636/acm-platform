param([ValidateSet('check','migrate','seed','verify','serve')][string]$Action='check', [ValidateSet('mysql-local','aiven')][string]$Profile='mysql-local', [string]$PrivateConfig='secrets/application-private.properties')
$ErrorActionPreference='Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
if (!(Test-Path -LiteralPath 'target/platform-0.1.0-SNAPSHOT.jar')) { throw 'Build first: mvn clean package' }
$privateConfigUri = if (Test-Path -LiteralPath $PrivateConfig) { (New-Object System.Uri((Resolve-Path -LiteralPath $PrivateConfig).Path)).AbsoluteUri } else { '' }
$databaseArgs = @('-jar','target/platform-0.1.0-SNAPSHOT.jar',"--spring.profiles.active=$Profile","--acm.db.action=$Action")
if ($privateConfigUri) { $databaseArgs += "--spring.config.additional-location=$privateConfigUri" }
& java @databaseArgs
if ($LASTEXITCODE -ne 0) { throw 'Database command failed. Check local secrets, TLS and database privileges without publishing credentials.' }
} finally { Pop-Location }
