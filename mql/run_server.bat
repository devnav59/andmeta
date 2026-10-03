@echo off
title MetaTrader VPS Bridge Server
color 0A
cls
echo ==========================================================
echo        MetaTrader VPS Bridge Server for Android
echo   Zero-Installation - Runs on 100%% of Windows VPS
echo ==========================================================
echo.

:: Automatically run PowerShell with ExecutionPolicy Bypass so Windows never blocks it!
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
    "$Port = 8080; " ^
    "Write-Host '[*] Configuring Firewall for Port ' $Port '...' -ForegroundColor Cyan; " ^
    "try { netsh advfirewall firewall add rule name='MT_Bridge_8080' dir=in action=allow protocol=TCP localport=$Port | Out-Null } catch {}; " ^
    "try { " ^
    "    $Listener = New-Object System.Net.Sockets.TcpListener([System.Net.IPAddress]::Any, $Port); " ^
    "    $Listener.Start(); " ^
    "    Write-Host '[+] ==========================================================' -ForegroundColor Green; " ^
    "    Write-Host '[+] MetaTrader VPS Bridge Server is RUNNING on Port ' $Port -ForegroundColor Green; " ^
    "    Write-Host '[+] Ready for Android App and MetaTrader EA!' -ForegroundColor Green; " ^
    "    Write-Host '[+] ==========================================================' -ForegroundColor Green; " ^
    "    Write-Host 'Keep this window OPEN while trading.`n' -ForegroundColor Yellow; " ^
    "    $LatestPositions = '{\"action\":\"POSITIONS_UPDATE\",\"data\":[]}'; " ^
    "    $PendingCommands = [System.Collections.ArrayList]::new(); " ^
    "    while ($true) { " ^
    "        $client = $Listener.AcceptTcpClient(); " ^
    "        $stream = $client.GetStream(); " ^
    "        $buffer = New-Object byte[] 65536; " ^
    "        $bytesRead = $stream.Read($buffer, 0, $buffer.Length); " ^
    "        if ($bytesRead -gt 0) { " ^
    "            $req = [System.Text.Encoding]::UTF8.GetString($buffer, 0, $bytesRead); " ^
    "            $respBody = ''; " ^
    "            if ($req.StartsWith('POST /api/positions')) { " ^
    "                $idx = $req.IndexOf(\"`r`n`r`n\"); " ^
    "                if ($idx -ge 0) { $LatestPositions = $req.Substring($idx + 4).Trim() }; " ^
    "                $cmds = if ($PendingCommands.Count -gt 0) { $a = $PendingCommands.ToArray(); $PendingCommands.Clear(); '[' + ($a -join ',') + ']' } else { '[]' }; " ^
    "                $respBody = '{\"status\":\"ok\",\"commands\":' + $cmds + '}'; " ^
    "            } elseif ($req.StartsWith('POST /api/command') -or $req.StartsWith('POST /api/order')) { " ^
    "                $idx = $req.IndexOf(\"`r`n`r`n\"); " ^
    "                if ($idx -ge 0) { [void]$PendingCommands.Add($req.Substring($idx + 4).Trim()) }; " ^
    "                $respBody = '{\"status\":\"queued\"}'; " ^
    "            } else { " ^
    "                $respBody = $LatestPositions; " ^
    "            }; " ^
    "            $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($respBody); " ^
    "            $header = \"HTTP/1.1 200 OK`r`nContent-Type: application/json; charset=utf-8`r`nAccess-Control-Allow-Origin: *`r`nContent-Length: \" + $bodyBytes.Length + \"`r`nConnection: close`r`n`r`n\"; " ^
    "            $headerBytes = [System.Text.Encoding]::UTF8.GetBytes($header); " ^
    "            $stream.Write($headerBytes, 0, $headerBytes.Length); " ^
    "            $stream.Write($bodyBytes, 0, $bodyBytes.Length); " ^
    "            $stream.Flush(); " ^
    "        }; " ^
    "        $client.Close(); " ^
    "    } " ^
    "} catch { " ^
    "    Write-Host '[-] Server Error: ' $_ -ForegroundColor Red; " ^
    "} "

echo.
echo ==========================================================
echo [!] Server stopped.
echo ==========================================================
pause
