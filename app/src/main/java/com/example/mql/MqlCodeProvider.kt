package com.example.mql

object MqlCodeProvider {

    val universalFirewallCommand: String =
        "netsh advfirewall firewall add rule name=\"MT_Bridge_8080\" dir=in action=allow protocol=TCP localport=8080"

    val guideTextFa: String = """
راهنمای اتصال به متاتریدر روی سرور مجازی (Windows VPS):

★ ساده‌ترین روش (پیشنهادی): بدون نیاز به نصب پایتون (با PowerShell داخلی ویندوز)
روی سرور مجازی ویندوز، هیچ نیازی به نصب پایتون ندارید! ویندوز به صورت پیش‌فرض PowerShell دارد.

۱. باز کردن پورت فایروال:
   روی VPS، پنجره Command Prompt یا PowerShell را با Run as Administrator باز کنید و این دستور را وارد و اینتر کنید:
   netsh advfirewall firewall add rule name="MT_Bridge_8080" dir=in action=allow protocol=TCP localport=8080

۲. اجرای سرور بریج (بدون نیاز به پایتون):
   - روی دسکتاپ VPS یک فایل متنی به نام bridge.ps1 بسازید.
   - کدهای تب "PowerShell (بدون پایتون)" را داخل آن کپی و ذخیره کنید.
   - روی فایل راست‌کلیک کرده و گزینه "Run with PowerShell" را انتخاب کنید.
   - پنجره آبی‌رنگ باز شده و پیام سبز رنگ "Server is RUNNING on Port 8080" نمایان می‌شود!

۳. اجرای اکسپرت در متاتریدر روی VPS:
   - اکسپرت MetaTrader_Bridge_EA را در متاتریدر با F7 کامپایل کنید و روی یک چارت بیندازید.
   - در متاتریدر کلیدهای Ctrl + O را بزنید و در تب Expert Advisors تیک Allow Algo Trading و تیک Allow WebRequest را زده و آدرس http://127.0.0.1:8080 را اضافه کنید.

۴. اتصال در اپلیکیشن موبایل:
   - در تب اتصال اپلیکیشن، در کادر IP، آدرس IP عمومی سرور مجازی خود (Public IP) را وارد کنید.
   - دکمه بزرگ "CONNECT TO VPS" را بزنید؛ وضعیت فوراً سبز (CONNECTED TO VPS LIVE) شده و تمام معاملات روی گوشی شما ظاهر می‌شوند!

----------------------------------------------------
★ اگر مایلید از پایتون استفاده کنید:
۱. پایتون ۳ را از python.org نصب کنید (تیک Add Python to PATH حتماً باید خورده باشد).
۲. کدهای تب "start_bridge.bat" و "bridge.py" را در یک پوشه ذخیره و فایل bat را اجرا کنید.
    """.trimIndent()

    val powerShellBridgeCode: String = """
# MetaTrader VPS Bridge Server in Pure PowerShell (No Python Needed!)
VAR_Port = 8080
VAR_Host.UI.RawUI.WindowTitle = "MetaTrader VPS Bridge Server (PowerShell Native)"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   MetaTrader VPS Bridge Server (Pure PowerShell)        " -ForegroundColor Green
Write-Host "   NO Python Installation Required - 100% Built-in       " -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Open Windows Firewall for Port 8080
try {
    netsh advfirewall firewall add rule name="MT_Bridge_8080" dir=in action=allow protocol=TCP localport=VAR_Port | Out-Null
    Write-Host "[+] Port VAR_Port successfully allowed in Firewall." -ForegroundColor Green
} catch {
    Write-Host "[!] Note: Run as Administrator to allow Firewall." -ForegroundColor Yellow
}

# 2. Start HTTP Listener on 0.0.0.0:8080
VAR_Listener = New-Object System.Net.HttpListener
VAR_Listener.Prefixes.Add("http://*:VAR_Port/")

try {
    VAR_Listener.Start()
    Write-Host "[+] Server is RUNNING on http://*:VAR_Port/" -ForegroundColor Green
    Write-Host "[*] Android App target: http://<YOUR_VPS_PUBLIC_IP>:VAR_Port" -ForegroundColor Cyan
    Write-Host "[*] MetaTrader EA target: http://127.0.0.1:VAR_Port/api/positions" -ForegroundColor Cyan
    Write-Host "==========================================================" -ForegroundColor Cyan
} catch {
    Write-Host "[-] Failed to bind port VAR_Port. Error: VAR_Err" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit
}

VAR_LatestPositionsJson = '{"action":"POSITIONS_UPDATE","data":[]}'
VAR_PendingCommands = [System.Collections.ArrayList]::new()
VAR_Lock = [System.Object]::new()

while (VAR_Listener.IsListening) {
    try {
        VAR_Context = VAR_Listener.GetContext()
        VAR_Request = VAR_Context.Request
        VAR_Response = VAR_Context.Response

        VAR_Response.Headers.Add("Access-Control-Allow-Origin", "*")
        VAR_Response.Headers.Add("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        VAR_Response.Headers.Add("Access-Control-Allow-Headers", "Content-Type")

        if (VAR_Request.HttpMethod -eq "OPTIONS") {
            VAR_Response.StatusCode = 200
            VAR_Response.Close()
            continue
        }

        VAR_Path = VAR_Request.Url.AbsolutePath
        VAR_ResponseString = ""

        if (VAR_Request.HttpMethod -eq "GET") {
            if (VAR_Path -eq "/api/positions" -or VAR_Path -eq "/" -or VAR_Path -eq "/status") {
                [System.Threading.Monitor]::Enter(VAR_Lock)
                try { VAR_ResponseString = VAR_LatestPositionsJson } finally { [System.Threading.Monitor]::Exit(VAR_Lock) }
                VAR_Response.ContentType = "application/json; charset=utf-8"
                VAR_Response.StatusCode = 200
            } else {
                VAR_Response.StatusCode = 404
                VAR_ResponseString = '{"error":"Not Found"}'
            }
        }
        elseif (VAR_Request.HttpMethod -eq "POST") {
            VAR_Reader = New-Object System.IO.StreamReader(VAR_Request.InputStream, VAR_Request.ContentEncoding)
            VAR_Body = VAR_Reader.ReadToEnd()
            VAR_Reader.Close()

            if (VAR_Path -eq "/api/positions") {
                [System.Threading.Monitor]::Enter(VAR_Lock)
                try {
                    VAR_LatestPositionsJson = VAR_Body
                    VAR_CmdsJson = if (VAR_PendingCommands.Count -gt 0) {
                        VAR_arr = VAR_PendingCommands.ToArray()
                        VAR_PendingCommands.Clear()
                        "[" + (VAR_arr -join ",") + "]"
                    } else { "[]" }
                    VAR_ResponseString = "{\"status\":\"ok\",\"commands\":" + VAR_CmdsJson + "}"
                } finally { [System.Threading.Monitor]::Exit(VAR_Lock) }
                VAR_Response.ContentType = "application/json; charset=utf-8"
                VAR_Response.StatusCode = 200
            }
            elseif (VAR_Path -eq "/api/command" -or VAR_Path -eq "/api/order") {
                [System.Threading.Monitor]::Enter(VAR_Lock)
                try {
                    [void]VAR_PendingCommands.Add(VAR_Body)
                    Write-Host "[+] Command received from Android: VAR_Body" -ForegroundColor Green
                } finally { [System.Threading.Monitor]::Exit(VAR_Lock) }
                VAR_ResponseString = '{"status":"queued"}'
                VAR_Response.ContentType = "application/json; charset=utf-8"
                VAR_Response.StatusCode = 200
            }
        }

        VAR_Buffer = [System.Text.Encoding]::UTF8.GetBytes(VAR_ResponseString)
        VAR_Response.ContentLength64 = VAR_Buffer.Length
        VAR_Response.OutputStream.Write(VAR_Buffer, 0, VAR_Buffer.Length)
        VAR_Response.OutputStream.Close()
    } catch {}
}
    """.trimIndent().replace("VAR_Err", "$" + "_").replace("VAR_", "$")

    val batchScriptCode: String = """
@echo off
title MetaTrader VPS Bridge Server
color 0B
cls
echo ==========================================================
echo        MetaTrader VPS Bridge Server for Android
echo ==========================================================
echo.
netsh advfirewall firewall add rule name="MT_Bridge_8080" dir=in action=allow protocol=TCP localport=8080 >nul 2>&1
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Python 3 is NOT installed on this VPS!
    echo Download: https://www.python.org/downloads/
    echo (Make sure to check "Add Python to PATH")
    pause
    exit /b 1
)
python bridge.py 8080
if %errorlevel% neq 0 (
    echo [!] Server stopped with error code %errorlevel%.
)
pause
    """.trimIndent()

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

    val pythonBridgeCode: String = """
#!/usr/bin/env python3
# MetaTrader VPS Bridge Server (bridge.py)
import sys, json, threading, hashlib, base64, struct
from http.server import HTTPServer, BaseHTTPRequestHandler
from socketserver import ThreadingMixIn

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
lock = threading.Lock()
latest_positions = []
pending_commands = []
connected_ws_clients = []

def ws_handshake_key(key):
    guid = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
    return base64.b64encode(hashlib.sha1((key + guid).encode('utf-8')).digest()).decode('utf-8')

def broadcast_positions(positions_data):
    msg = json.dumps({"action": "POSITIONS_UPDATE", "data": positions_data}).encode('utf-8')
    length = len(msg)
    header = bytes([0x81, length]) if length <= 125 else struct.pack('!BBH', 0x81, 126, length) if length <= 65535 else struct.pack('!BBQ', 0x81, 127, length)
    frame = header + msg
    with lock:
        dead = []
        for client in connected_ws_clients:
            try: client.sendall(frame)
            except: dead.append(client)
        for client in dead:
            if client in connected_ws_clients: connected_ws_clients.remove(client)

class ThreadedHTTPServer(ThreadingMixIn, HTTPServer):
    daemon_threads = True

class BridgeHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.headers.get('Upgrade', '').lower() == 'websocket':
            key = self.headers.get('Sec-WebSocket-Key', '')
            if key:
                resp = "HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: " + ws_handshake_key(key) + "\r\n\r\n"
                self.wfile.write(resp.encode('utf-8'))
                sock = self.connection
                sock.setblocking(True)
                with lock: connected_ws_clients.append(sock)
                print(f"[+] Android connected from {self.client_address[0]}")
                with lock: cur = list(latest_positions)
                if cur: broadcast_positions(cur)
                self.handle_ws(sock)
                return
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()
        with lock:
            self.wfile.write(json.dumps({"status": "online", "positions": len(latest_positions), "clients": len(connected_ws_clients)}).encode('utf-8'))

    def do_POST(self):
        clen = int(self.headers.get('Content-Length', 0))
        body = self.rfile.read(clen).decode('utf-8', errors='ignore')
        try:
            d = json.loads(body)
            act = d.get('action')
            if act == 'POSITIONS_UPDATE':
                global latest_positions
                pos = d.get('data', [])
                with lock: latest_positions = pos
                broadcast_positions(pos)
                with lock:
                    cmds = list(pending_commands)
                    pending_commands.clear()
                self.send_response(200)
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                self.wfile.write(json.dumps({"status": "ok", "commands": cmds}).encode('utf-8'))
                return
            elif act in ['OPEN_ORDER', 'MODIFY_SL_TP', 'CLOSE_POSITION']:
                with lock: pending_commands.append(d)
                self.send_response(200)
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                self.wfile.write(json.dumps({"status": "queued"}).encode('utf-8'))
                return
        except Exception as e:
            print("Error:", e)
        self.send_response(400)
        self.end_headers()

    def handle_ws(self, sock):
        while True:
            try:
                head = sock.recv(2)
                if not head or len(head) < 2 or (head[0] & 0x0F == 0x08): break
                is_mask = (head[1] & 0x80) != 0
                plen = head[1] & 0x7F
                if plen == 126: plen = struct.unpack('!H', sock.recv(2))[0]
                elif plen == 127: plen = struct.unpack('!Q', sock.recv(8))[0]
                mask = sock.recv(4) if is_mask else b''
                data = bytearray()
                while len(data) < plen:
                    chunk = sock.recv(plen - len(data))
                    if not chunk: break
                    data.extend(chunk)
                if is_mask:
                    for i in range(len(data)): data[i] ^= mask[i % 4]
                cmd = json.loads(data.decode('utf-8', errors='ignore'))
                if cmd.get('action') in ['OPEN_ORDER', 'MODIFY_SL_TP', 'CLOSE_POSITION']:
                    with lock: pending_commands.append(cmd)
            except: break
        with lock:
            if sock in connected_ws_clients: connected_ws_clients.remove(sock)
        print("[-] Android disconnected")

    def log_message(self, format, *args): return

if __name__ == '__main__':
    print(f"[*] MetaTrader VPS Bridge Server running on port {PORT}")
    ThreadedHTTPServer(('0.0.0.0', PORT), BridgeHandler).serve_forever()
    """.trimIndent()
}
