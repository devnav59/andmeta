# ==========================================================
# MetaTrader VPS Bridge in Pure PowerShell (No Python Needed!)
# Compatible with Windows Server 2016, 2019, 2022, Windows 10/11
# ==========================================================

$Port = 8080
$Host.UI.RawUI.WindowTitle = "MetaTrader VPS Bridge Server (PowerShell Native)"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   MetaTrader VPS Bridge Server (Pure PowerShell)        " -ForegroundColor Green
Write-Host "   NO Python Installation Required - 100% Built-in       " -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Open Windows Firewall for Port 8080
Write-Host "[*] Configuring Windows Firewall for Port $Port..." -ForegroundColor Gray
try {
    netsh advfirewall firewall add rule name="MT_Bridge_8080" dir=in action=allow protocol=TCP localport=$Port | Out-Null
    Write-Host "[+] Port $Port successfully allowed in Firewall." -ForegroundColor Green
} catch {
    Write-Host "[!] Note: Run PowerShell as Administrator to allow Firewall." -ForegroundColor Yellow
}

# 2. Start HTTP Listener on 0.0.0.0:8080
$Listener = New-Object System.Net.HttpListener
$Listener.Prefixes.Add("http://*:$Port/")

try {
    $Listener.Start()
    Write-Host "[+] Server is RUNNING and listening on http://*:$Port/" -ForegroundColor Green
    Write-Host "[*] Android App target: http://<YOUR_VPS_PUBLIC_IP>:$Port" -ForegroundColor Cyan
    Write-Host "[*] MetaTrader EA target: http://127.0.0.1:$Port/api/positions" -ForegroundColor Cyan
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host "Press Ctrl+C to stop the server.`n" -ForegroundColor Gray
} catch {
    Write-Host "[-] Failed to bind to port $Port. Error: $_" -ForegroundColor Red
    Write-Host "[!] Make sure PowerShell is Run as Administrator or port $Port is free." -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit
}

# In-memory storage for positions and commands
$LatestPositionsJson = '{"action":"POSITIONS_UPDATE","data":[]}'
$PendingCommands = [System.Collections.ArrayList]::new()
$Lock = [System.Object]::new()

while ($Listener.IsListening) {
    try {
        $Context = $Listener.GetContext()
        $Request = $Context.Request
        $Response = $Context.Response

        # CORS Headers for Mobile / Web clients
        $Response.Headers.Add("Access-Control-Allow-Origin", "*")
        $Response.Headers.Add("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        $Response.Headers.Add("Access-Control-Allow-Headers", "Content-Type")

        if ($Request.HttpMethod -eq "OPTIONS") {
            $Response.StatusCode = 200
            $Response.Close()
            continue
        }

        $Path = $Request.Url.AbsolutePath
        $ResponseString = ""

        if ($Request.HttpMethod -eq "GET") {
            if ($Path -eq "/api/positions" -or $Path -eq "/" -or $Path -eq "/status") {
                [System.Threading.Monitor]::Enter($Lock)
                try {
                    $ResponseString = $LatestPositionsJson
                } finally {
                    [System.Threading.Monitor]::Exit($Lock)
                }
                $Response.ContentType = "application/json; charset=utf-8"
                $Response.StatusCode = 200
            } else {
                $Response.StatusCode = 404
                $ResponseString = '{"error":"Not Found"}'
            }
        }
        elseif ($Request.HttpMethod -eq "POST") {
            $Reader = New-Object System.IO.StreamReader($Request.InputStream, $Request.ContentEncoding)
            $Body = $Reader.ReadToEnd()
            $Reader.Close()

            if ($Path -eq "/api/positions") {
                # Received from MetaTrader EA
                [System.Threading.Monitor]::Enter($Lock)
                try {
                    $LatestPositionsJson = $Body
                    # Pop queued commands to return to EA
                    $CmdsJson = if ($PendingCommands.Count -gt 0) {
                        $arr = $PendingCommands.ToArray()
                        $PendingCommands.Clear()
                        "[" + ($arr -join ",") + "]"
                    } else {
                        "[]"
                    }
                    $ResponseString = "{`"status`":`"ok`",`"commands`":$CmdsJson}"
                } finally {
                    [System.Threading.Monitor]::Exit($Lock)
                }

                $Response.ContentType = "application/json; charset=utf-8"
                $Response.StatusCode = 200
            }
            elseif ($Path -eq "/api/command" -or $Path -eq "/api/order") {
                # Received from Android Mobile App
                [System.Threading.Monitor]::Enter($Lock)
                try {
                    [void]$PendingCommands.Add($Body)
                    Write-Host "[+] Command received from Android: $Body" -ForegroundColor Green
                } finally {
                    [System.Threading.Monitor]::Exit($Lock)
                }

                $ResponseString = '{"status":"queued"}'
                $Response.ContentType = "application/json; charset=utf-8"
                $Response.StatusCode = 200
            }
            else {
                $Response.StatusCode = 404
                $ResponseString = '{"error":"Invalid Endpoint"}'
            }
        }

        $Buffer = [System.Text.Encoding]::UTF8.GetBytes($ResponseString)
        $Response.ContentLength64 = $Buffer.Length
        $Response.OutputStream.Write($Buffer, 0, $Buffer.Length)
        $Response.OutputStream.Close()

    } catch {
        # Continue loop on transient error
    }
}
