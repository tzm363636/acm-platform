param(
  [string]$Frontend='https://acm-platform-web.onrender.com',
  [string]$Backend='https://acm-platform-api.onrender.com'
)
$ErrorActionPreference='Stop'
foreach ($address in @($Frontend,$Backend)) {
  $uri=[Uri]$address
  if ($uri.Scheme -ne 'https' -or $uri.UserInfo -or $uri.Query -or $uri.Fragment -or $uri.AbsolutePath -ne '/') { throw 'Use an HTTPS service origin without credentials, paths or query parameters.' }
}
$failures=0
function Check([string]$Name,[bool]$Passed) {
  if ($Passed) { Write-Output "PASS $Name" }
  else { Write-Output "FAIL $Name"; $script:failures++ }
}
function Request([string]$Url,[string]$Method='GET',$Session=$null,$Headers=@{}) {
  try {
    $requestArgs=@{Uri=$Url;Method=$Method;UseBasicParsing=$true;TimeoutSec=60;Headers=$Headers}
    if ($PSVersionTable.PSVersion.Major -ge 7) { $requestArgs.SkipHttpErrorCheck=$true }
    if ($Session) { $requestArgs.WebSession=$Session }
    if ($Method -eq 'POST') { $requestArgs.Body='{}';$requestArgs.ContentType='application/json' }
    $r=Invoke-WebRequest @requestArgs
    $finalUri=if ($r.BaseResponse.ResponseUri) { $r.BaseResponse.ResponseUri } else { $r.BaseResponse.RequestMessage.RequestUri }
    return @{Status=[int]$r.StatusCode;Body=$r.Content;Headers=$r.Headers;FinalUri=$finalUri}
  } catch {
    $r=$_.Exception.Response
    if (!$r) {
      # Distinguish a local DNS/TLS/client failure from an HTTP error returned by Render.
      # Exception messages can include request details; print the class name only.
      Write-Host ('WARN No HTTP response ('+$_.Exception.GetType().Name+'); check this computer''s DNS, TLS and network. Certificate validation remains enabled.')
      return @{Status=0;Body='';Headers=@{};FinalUri=$null}
    }
    if ($r.GetType().FullName -eq 'System.Net.Http.HttpResponseMessage') {
      $body=$_.ErrorDetails.Message
      $headers=@{}
      foreach ($header in $r.Headers) { $headers[$header.Key]=$header.Value -join ', ' }
      foreach ($header in $r.Content.Headers) { $headers[$header.Key]=$header.Value -join ', ' }
      return @{Status=[int]$r.StatusCode;Body=$body;Headers=$headers;FinalUri=$r.RequestMessage.RequestUri}
    }
    $reader=New-Object IO.StreamReader($r.GetResponseStream())
    try { $body=$reader.ReadToEnd() } finally { $reader.Dispose() }
    return @{Status=[int]$r.StatusCode;Body=$body;Headers=$r.Headers;FinalUri=$r.ResponseUri}
  }
}
function Json($Response) {
  if ([string]$Response.Headers['Content-Type'] -notmatch 'application/json') { return $null }
  try { return ($Response.Body | ConvertFrom-Json) } catch { return $null }
}
foreach ($service in @(@{Name='backend';Origin=$Backend},@{Name='frontend proxy';Origin=$Frontend})) {
  $health=Request ($service.Origin.TrimEnd('/')+'/api/health')
  Check ($service.Name+' JSON health') ($health.Status -eq 200 -and $null -ne (Json $health))
  $r=Request ($service.Origin.TrimEnd('/')+'/api/capabilities');$cap=Json $r
  Check ($service.Name+' database and authentication enabled') ($r.Status -eq 200 -and $cap.database -eq $true -and $cap.login -eq $true)
}
$session=New-Object Microsoft.PowerShell.Commands.WebRequestSession
$base=$Frontend.TrimEnd('/')
$me=Request "$base/api/auth/me" 'GET' $session
$identity=Json $me
Check 'anonymous identity JSON' ($me.Status -eq 200 -and $identity.PSObject.Properties.Name -contains 'user' -and $null -eq $identity.user)
$r=Request "$base/api/auth/csrf" 'GET' $session
$csrf=Json $r
$valid=$r.Status -eq 200 -and $csrf.headerName -eq 'X-CSRF-TOKEN' -and ![string]::IsNullOrWhiteSpace($csrf.token)
$sameOrigin=$null -ne $r.FinalUri -and $r.FinalUri.GetLeftPart([UriPartial]::Authority) -eq ([Uri]$Frontend).GetLeftPart([UriPartial]::Authority)
Check 'same-origin CSRF endpoint' ($valid -and $sameOrigin)
$cookies=[string]$r.Headers['Set-Cookie']
Check 'HttpOnly Secure SameSite=Lax session cookie' ($cookies -match 'HttpOnly' -and $cookies -match 'Secure' -and $cookies -match 'SameSite=Lax')
Check 'session cookie covers all site pages (Path=/)' ($cookies -match 'Path=/($|;)')
if ($valid -and $sameOrigin) {
  # Anonymous logout only tests POST forwarding/authorization. It cannot alter business data.
  $result=Request "$base/api/auth/logout" 'POST' $session @{'X-CSRF-TOKEN'=$csrf.token}
  Check 'POST forwarding returns anonymous JSON 401' ($result.Status -eq 401 -and $null -ne (Json $result))
} else { Check 'POST forwarding (not attempted without valid same-origin CSRF)' $false }
Write-Output 'No account password, Cookie value or CSRF token was printed. No business data was changed.'
if ($failures) { exit 1 }
