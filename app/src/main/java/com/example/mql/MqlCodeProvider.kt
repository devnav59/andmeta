package com.example.mql

object MqlCodeProvider {

    val universalFirewallCommand: String =
        "netsh advfirewall firewall add rule name=\"MT_Bridge_8080\" dir=in action=allow protocol=TCP localport=8080"

    val oneLineVpsCommand: String =
        "powershell -ExecutionPolicy Bypass -NoExit -Command \"VAR_P=8080; try{netsh advfirewall firewall add rule name='MT_Bridge_8080' dir=in action=allow protocol=TCP localport=VAR_P|Out-Null}catch{}; VAR_L=New-Object System.Net.Sockets.TcpListener([System.Net.IPAddress]::Any,VAR_P); VAR_L.Start(); Write-Host '[+] MetaTrader VPS Bridge Server is RUNNING on Port 8080!' -F Green; Write-Host 'Ready for Android and MetaTrader EA. Keep window open.' -F Yellow; VAR_D='{\\\"action\\\":\\\"POSITIONS_UPDATE\\\",\\\"data\\\":[]}'; VAR_C=[System.Collections.ArrayList]::new(); while(1){VAR_c=VAR_L.AcceptTcpClient(); VAR_s=VAR_c.GetStream(); VAR_b=New-Object byte[] 65536; VAR_r=VAR_s.Read(VAR_b,0,VAR_b.Length); if(VAR_r -gt 0){VAR_t=[System.Text.Encoding]::UTF8.GetString(VAR_b,0,VAR_r); VAR_i=VAR_t.IndexOf(\\\"`r`n`r`n\\\"); if(VAR_t.StartsWith('POST /api/positions')){VAR_D=VAR_t.Substring(VAR_i+4).Trim(); VAR_k=if(VAR_C.Count -gt 0){VAR_a=VAR_C.ToArray(); VAR_C.Clear(); '['+(VAR_a -join ',')+']'}else{'[]'}; VAR_o='{\\\"status\\\":\\\"ok\\\",\\\"commands\\\":'+VAR_k+'}'}elseif(VAR_t.StartsWith('POST /api/command')){VAR_C.Add(VAR_t.Substring(VAR_i+4).Trim()); VAR_o='{\\\"status\\\":\\\"queued\\\"}'}else{VAR_o=VAR_D}; VAR_ob=[System.Text.Encoding]::UTF8.GetBytes(VAR_o); VAR_h=[System.Text.Encoding]::UTF8.GetBytes(\\\"HTTP/1.1 200 OK`r`nContent-Type: application/json`r`nAccess-Control-Allow-Origin: *`r`nContent-Length: \\\"+VAR_ob.Length+\\\"`r`nConnection: close`r`n`r`n\\\"); VAR_s.Write(VAR_h,0,VAR_h.Length); VAR_s.Write(VAR_ob,0,VAR_ob.Length); VAR_s.Flush()}; VAR_c.Close()}\""
            .replace("VAR_", "$")

    val guideTextFa: String = """
راهنمای حل مشکل بسته شدن پنجره و راه‌اندازی سریع سرور:

علت بسته شدن فایل bridge.ps1:
در ویندوز سرور، به دلایل امنیتی سیاست اجرای اسکریپت‌ها (ExecutionPolicy) بسته است و با اجرای فایل .ps1، پاورشل ارور قرمز داده و در کسری از ثانیه پنجره را می‌بندد.

★ روش ۱: اجرای فوری ۱-خطی در ترمینال سرور مجازی (پیشنهادی - بدون نیاز به ساخت فایل)
۱. در سرور مجازی، پنجره Command Prompt (یا PowerShell) را با Run as Administrator باز کنید.
۲. دکمه "کپی دستور ۱-خطی سرور" را در بالای همین صفحه بزنید.
۳. دستور کپی‌شده را در ترمینال Paste کرده و Enter بزنید.
۴. سرور بلافاصله روی پورت ۸۰۸۰ اجرا شده، فایروال را باز کرده و پنجره باز باقی می‌ماند!

★ روش ۲: ساخت فایل run_server.bat (دابل‌کلیک بدون بسته شدن)
۱. روی دسکتاپ سرور مجازی، یک فایل به نام run_server.bat بسازید.
۲. کدهای تب "run_server.bat" را داخل آن کپی و ذخیره کنید.
۳. روی فایل run_server.bat دابل‌کلیک کنید. این فایل قفل امنیتی ویندوز را دور می‌زند و هرگز بسته نمی‌شود.

★ مرحله بعد در متاتریدر روی VPS:
۱. اکسپرت MetaTrader_Bridge_EA را در متاتریدر با کلید F7 کامپایل کنید و روی یک چارت بیندازید.
۲. در متاتریدر کلیدهای Ctrl + O را بزنید و در تب Expert Advisors تیک‌های Allow Algo Trading و Allow WebRequest را فعال کرده و آدرس http://127.0.0.1:8080 را اضافه کنید.

★ مرحله نهایی در موبایل:
در تب اتصال اپلیکیشن، IP سرور مجازی را وارد کرده و دکمه "CONNECT TO VPS" را بزنید. معاملات فوراً ظاهر می‌شوند!
    """.trimIndent()

    val runServerBatCode: String = """
@echo off
title MetaTrader VPS Bridge Server
color 0A
cls
echo ==========================================================
echo        MetaTrader VPS Bridge Server for Android
echo   Zero-Installation - Runs on 100%% of Windows VPS
echo ==========================================================
echo.
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
    "VAR_Port = 8080; " ^
    "Write-Host '[*] Configuring Firewall for Port ' VAR_Port '...' -ForegroundColor Cyan; " ^
    "try { netsh advfirewall firewall add rule name='MT_Bridge_8080' dir=in action=allow protocol=TCP localport=VAR_Port | Out-Null } catch {}; " ^
    "try { " ^
    "    VAR_Listener = New-Object System.Net.Sockets.TcpListener([System.Net.IPAddress]::Any, VAR_Port); " ^
    "    VAR_Listener.Start(); " ^
    "    Write-Host '[+] ==========================================================' -ForegroundColor Green; " ^
    "    Write-Host '[+] MetaTrader VPS Bridge Server is RUNNING on Port ' VAR_Port -ForegroundColor Green; " ^
    "    Write-Host '[+] Ready for Android App and MetaTrader EA!' -ForegroundColor Green; " ^
    "    Write-Host '[+] ==========================================================' -ForegroundColor Green; " ^
    "    Write-Host 'Keep this window OPEN while trading.`n' -ForegroundColor Yellow; " ^
    "    VAR_LatestPositions = '{\"action\":\"POSITIONS_UPDATE\",\"data\":[]}'; " ^
    "    VAR_PendingCommands = [System.Collections.ArrayList]::new(); " ^
    "    while (VAR_true) { " ^
    "        VAR_client = VAR_Listener.AcceptTcpClient(); " ^
    "        VAR_stream = VAR_client.GetStream(); " ^
    "        VAR_buffer = New-Object byte[] 65536; " ^
    "        VAR_bytesRead = VAR_stream.Read(VAR_buffer, 0, VAR_buffer.Length); " ^
    "        if (VAR_bytesRead -gt 0) { " ^
    "            VAR_req = [System.Text.Encoding]::UTF8.GetString(VAR_buffer, 0, VAR_bytesRead); " ^
    "            VAR_respBody = ''; " ^
    "            if (VAR_req.StartsWith('POST /api/positions')) { " ^
    "                VAR_idx = VAR_req.IndexOf(\"`r`n`r`n\"); " ^
    "                if (VAR_idx -ge 0) { VAR_LatestPositions = VAR_req.Substring(VAR_idx + 4).Trim() }; " ^
    "                VAR_cmds = if (VAR_PendingCommands.Count -gt 0) { VAR_a = VAR_PendingCommands.ToArray(); VAR_PendingCommands.Clear(); '[' + (VAR_a -join ',') + ']' } else { '[]' }; " ^
    "                VAR_respBody = '{\"status\":\"ok\",\"commands\":' + VAR_cmds + '}'; " ^
    "            } elseif (VAR_req.StartsWith('POST /api/command') -or VAR_req.StartsWith('POST /api/order')) { " ^
    "                VAR_idx = VAR_req.IndexOf(\"`r`n`r`n\"); " ^
    "                if (VAR_idx -ge 0) { [void]VAR_PendingCommands.Add(VAR_req.Substring(VAR_idx + 4).Trim()) }; " ^
    "                VAR_respBody = '{\"status\":\"queued\"}'; " ^
    "            } else { " ^
    "                VAR_respBody = VAR_LatestPositions; " ^
    "            }; " ^
    "            VAR_bodyBytes = [System.Text.Encoding]::UTF8.GetBytes(VAR_respBody); " ^
    "            VAR_header = \"HTTP/1.1 200 OK`r`nContent-Type: application/json; charset=utf-8`r`nAccess-Control-Allow-Origin: *`r`nContent-Length: \" + VAR_bodyBytes.Length + \"`r`nConnection: close`r`n`r`n\"; " ^
    "            VAR_headerBytes = [System.Text.Encoding]::UTF8.GetBytes(VAR_header); " ^
    "            VAR_stream.Write(VAR_headerBytes, 0, VAR_headerBytes.Length); " ^
    "            VAR_stream.Write(VAR_bodyBytes, 0, VAR_bodyBytes.Length); " ^
    "            VAR_stream.Flush(); " ^
    "        }; " ^
    "        VAR_client.Close(); " ^
    "    } " ^
    "} catch { " ^
    "    Write-Host '[-] Server Error: ' VAR_Err -ForegroundColor Red; " ^
    "} "
echo.
echo ==========================================================
echo [!] Server stopped.
echo ==========================================================
pause
    """.trimIndent().replace("VAR_Err", "$" + "_").replace("VAR_", "$")

    val mql5Code: String = """
//+------------------------------------------------------------------+
//|                                     MetaTrader_Bridge_EA.mq5    |
//|                    Copyright 2026, MetaTrader Floating Bubble    |
//|               Production-Ready MQL5 Bridge for Windows VPS       |
//+------------------------------------------------------------------+
#property copyright "MetaTrader Floating Bubble"
#property link      "https://github.com"
#property version   "3.00"

#include <Trade\Trade.mqh>
#include <Trade\PositionInfo.mqh>
#include <Trade\SymbolInfo.mqh>

input string   InpServerHost     = "127.0.0.1";     // Local Bridge IP on VPS
input int      InpServerPort     = 8080;            // Bridge Port
input int      InpTimerSeconds   = 1;               // Sync Interval (Seconds)
input ulong    InpMagicNumber    = 101010;          // Magic Number
input int      InpSlippage       = 20;              // Slippage Points
input bool     InpUseWebRequest  = true;            // True = WebRequest (Recommended for VPS)
input bool     InpUseSocket      = false;           // True = Raw Socket

CTrade         trade;
CPositionInfo  posInfo;
CSymbolInfo    symInfo;

datetime       g_last_push_time  = 0;
int            g_client_socket   = -1;

double GetPipValue(string sym)
{
   int digits = (int)SymbolInfoInteger(sym, SYMBOL_DIGITS);
   double point = SymbolInfoDouble(sym, SYMBOL_POINT);
   if(digits == 3 || digits == 5) return point * 10.0;
   return point;
}

void SetSafeFillingType()
{
   uint filling = (uint)SymbolInfoInteger(_Symbol, SYMBOL_FILLING_MODE);
   if((filling & SYMBOL_FILLING_FOK) != 0) trade.SetTypeFilling(ORDER_FILLING_FOK);
   else if((filling & SYMBOL_FILLING_IOC) != 0) trade.SetTypeFilling(ORDER_FILLING_IOC);
   else trade.SetTypeFilling(ORDER_FILLING_RETURN);
}

int OnInit()
{
   trade.SetExpertMagicNumber(InpMagicNumber);
   trade.SetDeviationInPoints(InpSlippage);
   SetSafeFillingType();
   EventSetTimer(InpTimerSeconds);
   Print("[+] MT5 VPS Bridge EA initialized. Target: ", InpServerHost, ":", InpServerPort);
   return INIT_SUCCEEDED;
}

void OnDeinit(const int reason)
{
   EventKillTimer();
   if(g_client_socket >= 0) { SocketClose(g_client_socket); g_client_socket = -1; }
}

void OnTimer() { SyncWithBridge(); }

void OnTick()
{
   if(TimeCurrent() - g_last_push_time >= InpTimerSeconds) SyncWithBridge();
}

string BuildPositionsJson()
{
   int total = PositionsTotal();
   string json = "{\"action\":\"POSITIONS_UPDATE\",\"data\":[";
   bool first = true;
   for(int i = 0; i < total; i++)
   {
      ulong ticket = PositionGetTicket(i);
      if(ticket == 0) continue;
      string symbol    = PositionGetString(POSITION_SYMBOL);
      ENUM_POSITION_TYPE type = (ENUM_POSITION_TYPE)PositionGetInteger(POSITION_TYPE);
      double volume    = PositionGetDouble(POSITION_VOLUME);
      double openPrice = PositionGetDouble(POSITION_PRICE_OPEN);
      double sl        = PositionGetDouble(POSITION_SL);
      double tp        = PositionGetDouble(POSITION_TP);
      double profit    = PositionGetDouble(POSITION_PROFIT) + PositionGetDouble(POSITION_SWAP);
      int digits       = (int)SymbolInfoInteger(symbol, SYMBOL_DIGITS);
      double point     = SymbolInfoDouble(symbol, SYMBOL_POINT);
      int typeInt = (type == POSITION_TYPE_BUY) ? 0 : 1;
      if(!first) json += ",";
      first = false;
      json += StringFormat("{\"ticket\":%I64u,\"symbol\":\"%s\",\"type\":%d,\"volume\":%.2f,\"open_price\":%.*f,\"sl\":%.*f,\"tp\":%.*f,\"profit\":%.2f,\"digits\":%d,\"point_size\":%.*f}",
         ticket, symbol, typeInt, volume, digits, openPrice, digits, sl, digits, tp, profit, digits, digits, point);
   }
   json += "]}";
   return json;
}

void SyncWithBridge()
{
   g_last_push_time = TimeCurrent();
   string jsonPayload = BuildPositionsJson();
   string url = "http://" + InpServerHost + ":" + IntegerToString(InpServerPort) + "/api/positions";
   string headers = "Content-Type: application/json\r\nAccept: application/json\r\n";
   char postData[];
   StringToCharArray(jsonPayload, postData, 0, StringLen(jsonPayload), CP_UTF8);
   char result[];
   string resultHeaders;
   ResetLastError();
   int res = WebRequest("POST", url, headers, 1000, postData, result, resultHeaders);
   if(res == 200)
   {
      string responseStr = CharArrayToString(result, 0, ArraySize(result), CP_UTF8);
      if(StringLen(responseStr) > 0) ProcessJsonCommands(responseStr);
   }
}

void ProcessJsonCommands(string json)
{
   if(StringFind(json, "OPEN_ORDER") >= 0) ExecuteOpenOrder(json);
   if(StringFind(json, "MODIFY_SL_TP") >= 0) ExecuteModifySlTp(json);
   if(StringFind(json, "CLOSE_POSITION") >= 0) ExecuteClosePosition(json);
}

void ExecuteOpenOrder(string json)
{
   string symbol   = ExtractJsonString(json, "symbol");
   string typeStr  = ExtractJsonString(json, "type");
   double volume   = ExtractJsonDouble(json, "volume");
   double price    = ExtractJsonDouble(json, "price");
   double slPips   = ExtractJsonDouble(json, "sl_pips");
   double tpPips   = ExtractJsonDouble(json, "tp_pips");
   if(symbol == "") symbol = _Symbol;
   if(volume <= 0.0) volume = 0.01;
   symInfo.Name(symbol);
   symInfo.RefreshRates();
   double pip = GetPipValue(symbol);
   int digits = (int)SymbolInfoInteger(symbol, SYMBOL_DIGITS);
   double ask = symInfo.Ask();
   double bid = symInfo.Bid();
   SetSafeFillingType();

   if(typeStr == "BUY")
   {
      double sl = (slPips > 0) ? NormalizeDouble(ask - slPips * pip, digits) : 0;
      double tp = (tpPips > 0) ? NormalizeDouble(ask + tpPips * pip, digits) : 0;
      trade.Buy(volume, symbol, ask, sl, tp, "VPS Bubble Buy");
   }
   else if(typeStr == "SELL")
   {
      double sl = (slPips > 0) ? NormalizeDouble(bid - slPips * pip, digits) : 0;
      double tp = (tpPips > 0) ? NormalizeDouble(bid - tpPips * pip, digits) : 0;
      trade.Sell(volume, symbol, bid, sl, tp, "VPS Bubble Sell");
   }
   else if(typeStr == "BUY_LIMIT")
   {
      if(price <= 0) price = ask - 20 * pip;
      double sl = (slPips > 0) ? NormalizeDouble(price - slPips * pip, digits) : 0;
      double tp = (tpPips > 0) ? NormalizeDouble(price + tpPips * pip, digits) : 0;
      trade.BuyLimit(volume, price, symbol, sl, tp, ORDER_TIME_GTC, 0, "VPS Bubble BuyLimit");
   }
   else if(typeStr == "SELL_LIMIT")
   {
      if(price <= 0) price = bid + 20 * pip;
      double sl = (slPips > 0) ? NormalizeDouble(price - slPips * pip, digits) : 0;
      double tp = (tpPips > 0) ? NormalizeDouble(price - tpPips * pip, digits) : 0;
      trade.SellLimit(volume, price, symbol, sl, tp, ORDER_TIME_GTC, 0, "VPS Bubble SellLimit");
   }
}

void ExecuteModifySlTp(string json)
{
   ulong ticket     = (ulong)ExtractJsonDouble(json, "ticket");
   double slDelta   = ExtractJsonDouble(json, "sl_pips_delta");
   double tpDelta   = ExtractJsonDouble(json, "tp_pips_delta");
   if(!PositionSelectByTicket(ticket)) return;
   string symbol = PositionGetString(POSITION_SYMBOL);
   ENUM_POSITION_TYPE type = (ENUM_POSITION_TYPE)PositionGetInteger(POSITION_TYPE);
   double currentSl = PositionGetDouble(POSITION_SL);
   double currentTp = PositionGetDouble(POSITION_TP);
   double openPrice = PositionGetDouble(POSITION_PRICE_OPEN);
   int digits = (int)SymbolInfoInteger(symbol, SYMBOL_DIGITS);
   double pip = GetPipValue(symbol);
   double baseSl = (currentSl > 0) ? currentSl : openPrice;
   double baseTp = (currentTp > 0) ? currentTp : openPrice;
   double newSl = currentSl;
   double newTp = currentTp;
   if(type == POSITION_TYPE_BUY)
   {
      if(slDelta != 0) newSl = NormalizeDouble(baseSl + (slDelta * pip), digits);
      if(tpDelta != 0) newTp = NormalizeDouble(baseTp + (tpDelta * pip), digits);
   }
   else
   {
      if(slDelta != 0) newSl = NormalizeDouble(baseSl - (slDelta * pip), digits);
      if(tpDelta != 0) newTp = NormalizeDouble(baseTp - (tpDelta * pip), digits);
   }
   trade.PositionModify(ticket, newSl, newTp);
}

void ExecuteClosePosition(string json)
{
   ulong ticket = (ulong)ExtractJsonDouble(json, "ticket");
   if(ticket > 0) trade.PositionClose(ticket);
}

string ExtractJsonString(string json, string key)
{
   string pattern = "\"" + key + "\":\"";
   int pos = StringFind(json, pattern);
   if(pos < 0) return "";
   pos += StringLen(pattern);
   int end = StringFind(json, "\"", pos);
   if(end < 0) return "";
   return StringSubstr(json, pos, end - pos);
}

double ExtractJsonDouble(string json, string key)
{
   string pattern = "\"" + key + "\":";
   int pos = StringFind(json, pattern);
   if(pos < 0) return 0.0;
   pos += StringLen(pattern);
   while(pos < StringLen(json) && (StringGetCharacter(json, pos) == ' ' || StringGetCharacter(json, pos) == '\"')) pos++;
   int end = pos;
   while(end < StringLen(json))
   {
      ushort ch = StringGetCharacter(json, end);
      if((ch >= '0' && ch <= '9') || ch == '.' || ch == '-' || ch == '+') end++;
      else break;
   }
   if(end <= pos) return 0.0;
   return StringToDouble(StringSubstr(json, pos, end - pos));
}
    """.trimIndent()

    val mql4Code: String = """
//+------------------------------------------------------------------+
//|                                     MetaTrader_Bridge_EA.mq4    |
//|                    Copyright 2026, MetaTrader Floating Bubble    |
//|               Production-Ready MQL4 Bridge for Windows VPS       |
//+------------------------------------------------------------------+
#property copyright "MetaTrader Floating Bubble"
#property link      "https://github.com"
#property version   "3.00"
#property strict

input string   InpServerHost     = "127.0.0.1";     // Local Bridge IP on VPS
input int      InpServerPort     = 8080;            // Bridge Port
input int      InpTimerSeconds   = 1;               // Sync Interval (Seconds)
input int      InpMagicNumber    = 101010;          // Magic Number
input int      InpSlippage       = 20;              // Slippage Points

datetime       g_last_push_time  = 0;

double GetPipValue(string sym)
{
   int digits = (int)MarketInfo(sym, MODE_DIGITS);
   double point = MarketInfo(sym, MODE_POINT);
   if(digits == 3 || digits == 5) return point * 10.0;
   return point;
}

int OnInit()
{
   EventSetTimer(InpTimerSeconds);
   Print("[+] MT4 VPS Bridge EA initialized. Target: ", InpServerHost, ":", InpServerPort);
   return INIT_SUCCEEDED;
}

void OnDeinit(const int reason)
{
   EventKillTimer();
}

void OnTimer() { SyncWithBridge(); }

void OnTick()
{
   if(TimeCurrent() - g_last_push_time >= InpTimerSeconds) SyncWithBridge();
}

string BuildPositionsJson()
{
   int total = OrdersTotal();
   string json = "{\"action\":\"POSITIONS_UPDATE\",\"data\":[";
   bool first = true;
   for(int i = 0; i < total; i++)
   {
      if(!OrderSelect(i, SELECT_BY_POS, MODE_TRADES)) continue;
      int type = OrderType();
      if(type != OP_BUY && type != OP_SELL) continue;
      int ticket       = OrderTicket();
      string symbol    = OrderSymbol();
      double volume    = OrderLots();
      double openPrice = OrderOpenPrice();
      double sl        = OrderStopLoss();
      double tp        = OrderTakeProfit();
      double profit    = OrderProfit() + OrderSwap();
      int digits       = (int)MarketInfo(symbol, MODE_DIGITS);
      double point     = MarketInfo(symbol, MODE_POINT);
      int typeInt = (type == OP_BUY) ? 0 : 1;
      if(!first) json += ",";
      first = false;
      json += StringFormat("{\"ticket\":%d,\"symbol\":\"%s\",\"type\":%d,\"volume\":%.2f,\"open_price\":%.*f,\"sl\":%.*f,\"tp\":%.*f,\"profit\":%.2f,\"digits\":%d,\"point_size\":%.*f}",
         ticket, symbol, typeInt, volume, digits, openPrice, digits, sl, digits, tp, profit, digits, digits, point);
   }
   json += "]}";
   return json;
}

void SyncWithBridge()
{
   g_last_push_time = TimeCurrent();
   string jsonPayload = BuildPositionsJson();
   string url = "http://" + InpServerHost + ":" + IntegerToString(InpServerPort) + "/api/positions";
   string headers = "Content-Type: application/json\r\nAccept: application/json\r\n";
   char postData[];
   StringToCharArray(jsonPayload, postData, 0, StringLen(jsonPayload), CP_UTF8);
   char result[];
   string resultHeaders;
   ResetLastError();
   int res = WebRequest("POST", url, headers, 1000, postData, result, resultHeaders);
   if(res == 200)
   {
      string responseStr = CharArrayToString(result, 0, ArraySize(result), CP_UTF8);
      if(StringLen(responseStr) > 0) ProcessJsonCommands(responseStr);
   }
}

void ProcessJsonCommands(string json)
{
   if(StringFind(json, "OPEN_ORDER") >= 0) ExecuteOpenOrder(json);
   if(StringFind(json, "MODIFY_SL_TP") >= 0) ExecuteModifySlTp(json);
   if(StringFind(json, "CLOSE_POSITION") >= 0) ExecuteClosePosition(json);
}

void ExecuteOpenOrder(string json)
{
   string symbol   = ExtractJsonString(json, "symbol");
   string typeStr  = ExtractJsonString(json, "type");
   double volume   = ExtractJsonDouble(json, "volume");
   double price    = ExtractJsonDouble(json, "price");
   double slPips   = ExtractJsonDouble(json, "sl_pips");
   double tpPips   = ExtractJsonDouble(json, "tp_pips");
   if(symbol == "") symbol = Symbol();
   if(volume <= 0.0) volume = 0.01;
   int digits = (int)MarketInfo(symbol, MODE_DIGITS);
   double pip = GetPipValue(symbol);
   double ask = MarketInfo(symbol, MODE_ASK);
   double bid = MarketInfo(symbol, MODE_BID);
   int cmd = OP_BUY;
   double openPrice = ask;
   if(typeStr == "BUY") { cmd = OP_BUY; openPrice = ask; }
   else if(typeStr == "SELL") { cmd = OP_SELL; openPrice = bid; }
   else if(typeStr == "BUY_LIMIT") { cmd = OP_BUYLIMIT; openPrice = (price > 0) ? price : ask - 20 * pip; }
   else if(typeStr == "SELL_LIMIT") { cmd = OP_SELLLIMIT; openPrice = (price > 0) ? price : bid + 20 * pip; }

   bool isBuyType = (cmd == OP_BUY || cmd == OP_BUYLIMIT);
   double sl = 0.0;
   double tp = 0.0;
   if(slPips > 0) sl = NormalizeDouble(isBuyType ? openPrice - slPips * pip : openPrice + slPips * pip, digits);
   if(tpPips > 0) tp = NormalizeDouble(isBuyType ? openPrice + tpPips * pip : openPrice - tpPips * pip, digits);
   OrderSend(symbol, cmd, volume, openPrice, InpSlippage, sl, tp, "VPS Bubble", InpMagicNumber, 0, (cmd == OP_BUY) ? clrGreen : clrRed);
}

void ExecuteModifySlTp(string json)
{
   int ticket       = (int)ExtractJsonDouble(json, "ticket");
   double slDelta   = ExtractJsonDouble(json, "sl_pips_delta");
   double tpDelta   = ExtractJsonDouble(json, "tp_pips_delta");
   if(!OrderSelect(ticket, SELECT_BY_TICKET)) return;
   string symbol    = OrderSymbol();
   int digits       = (int)MarketInfo(symbol, MODE_DIGITS);
   double pip       = GetPipValue(symbol);
   int type         = OrderType();
   double currentSl = OrderStopLoss();
   double currentTp = OrderTakeProfit();
   double openPrice = OrderOpenPrice();
   double baseSl = (currentSl > 0) ? currentSl : openPrice;
   double baseTp = (currentTp > 0) ? currentTp : openPrice;
   double newSl = currentSl;
   double newTp = currentTp;
   if(type == OP_BUY)
   {
      if(slDelta != 0) newSl = NormalizeDouble(baseSl + (slDelta * pip), digits);
      if(tpDelta != 0) newTp = NormalizeDouble(baseTp + (tpDelta * pip), digits);
   }
   else
   {
      if(slDelta != 0) newSl = NormalizeDouble(baseSl - (slDelta * pip), digits);
      if(tpDelta != 0) newTp = NormalizeDouble(baseTp - (tpDelta * pip), digits);
   }
   OrderModify(ticket, openPrice, newSl, newTp, 0, clrOrange);
}

void ExecuteClosePosition(string json)
{
   int ticket = (int)ExtractJsonDouble(json, "ticket");
   if(ticket <= 0 || !OrderSelect(ticket, SELECT_BY_TICKET)) return;
   double closePrice = (OrderType() == OP_BUY) ? MarketInfo(OrderSymbol(), MODE_BID) : MarketInfo(OrderSymbol(), MODE_ASK);
   OrderClose(ticket, OrderLots(), closePrice, InpSlippage, clrWhite);
}

string ExtractJsonString(string json, string key)
{
   string pattern = "\"" + key + "\":\"";
   int pos = StringFind(json, pattern);
   if(pos < 0) return "";
   pos += StringLen(pattern);
   int end = StringFind(json, "\"", pos);
   if(end < 0) return "";
   return StringSubstr(json, pos, end - pos);
}

double ExtractJsonDouble(string json, string key)
{
   string pattern = "\"" + key + "\":";
   int pos = StringFind(json, pattern);
   if(pos < 0) return 0.0;
   pos += StringLen(pattern);
   while(pos < StringLen(json) && (StringGetCharacter(json, pos) == ' ' || StringGetCharacter(json, pos) == '\"')) pos++;
   int end = pos;
   while(end < StringLen(json))
   {
      ushort ch = StringGetCharacter(json, end);
      if((ch >= '0' && ch <= '9') || ch == '.' || ch == '-' || ch == '+') end++;
      else break;
   }
   if(end <= pos) return 0.0;
   return StringToDouble(StringSubstr(json, pos, end - pos));
}
    """.trimIndent()
}
