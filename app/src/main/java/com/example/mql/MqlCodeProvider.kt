package com.example.mql

object MqlCodeProvider {

    val guideTextFa: String = """
راهنمای اتصال اکسپرت MetaTrader به اپلیکیشن شناور اندروید:

۱. فایل اکسپرت (MetaTrader_Bridge_EA.mq5 یا mq4) را در پوشه Experts متاتریدر خود کپی کنید:
   - در متاتریدر: File -> Open Data Folder -> MQL5 -> Experts
۲. در متاتریدر، پنجره Options را باز کنید (Ctrl + O):
   - به تب Expert Advisors بروید.
   - تیک گزینه "Allow Algo Trading" را فعال کنید.
   - تیک "Allow DLL imports" و "Allow WebRequest" را فعال نمایید.
۳. اکسپرت را کامپایل (F7) کرده و روی یک نمودار فعال (مانند EURUSD) Drag & Drop کنید.
۴. پورت پیش‌فرض ۸۰۸۰ است.
۵. در این اپلیکیشن اندروید:
   - آدرس آی‌پی کامپیوتر خود در شبکه محلی (Wi-Fi مشترک، مثلاً 192.168.1.100) را وارد کنید.
   - دکمه Connect را بزنید.
   - دکمه شناور Floating Bubble را فعال کنید تا کنترل پنل روی تمام برنامه‌ها ظاهر شود!
    """.trimIndent()

    val guideTextEn: String = """
MetaTrader EA Bridge Setup Guide:

1. Copy the EA file into your MetaTrader Experts directory:
   - MetaTrader -> File -> Open Data Folder -> MQL5 -> Experts
2. Open MetaTrader Options (Ctrl + O):
   - Under 'Expert Advisors', enable 'Allow Algo Trading'.
   - Enable 'Allow WebRequest' and 'Allow DLL imports' if required.
3. Attach MetaTrader_Bridge_EA to any active chart. Default port: 8080.
4. On your Android device (connected to the same Wi-Fi):
   - Enter your PC's Local IP address and Port 8080.
   - Tap 'Connect' or toggle 'Launch Floating Bubble'.
    """.trimIndent()

    val mql5Code: String = """
//+------------------------------------------------------------------+
//|                                     MetaTrader_Bridge_EA.mq5    |
//|                    Copyright 2026, MetaTrader Floating Bubble    |
//+------------------------------------------------------------------+
#property copyright "MetaTrader Floating Bubble"
#property link      "https://github.com"
#property version   "1.00"
#property strict

#include <Trade\Trade.mqh>
#include <Trade\PositionInfo.mqh>
#include <Trade\SymbolInfo.mqh>

input int      InpServerPort     = 8080;      // WebSocket Server Port
input int      InpTimerSeconds   = 1;         // Push Interval (Seconds)
input ulong    InpMagicNumber    = 101010;    // Magic Number

CTrade         trade;
CPositionInfo  posInfo;
CSymbolInfo    symInfo;

int            g_server_socket   = INVALID_HANDLE;
int            g_client_socket   = INVALID_HANDLE;
bool           g_is_websocket    = false;
datetime       g_last_push_time  = 0;

#define WS_GUID "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"

// Exact Pip calculation based on digits:
// 5 & 3 digits: 1 Pip = 10 Points
// 4 & 2 digits: 1 Pip = 1 Point
double GetPipValue(string sym)
{
   int digits = (int)SymbolInfoInteger(sym, SYMBOL_DIGITS);
   double point = SymbolInfoDouble(sym, SYMBOL_POINT);
   if(digits == 3 || digits == 5)
      return point * 10.0;
   return point;
}

int OnInit()
{
   trade.SetExpertMagicNumber(InpMagicNumber);
   trade.SetDeviationInPoints(20);
   trade.SetTypeFilling(ORDER_FILLING_FOK);

   g_server_socket = SocketCreate();
   if(g_server_socket == INVALID_HANDLE)
   {
      Print("[-] Failed to create server socket. Error: ", GetLastError());
      return INIT_FAILED;
   }

   if(!SocketListen(g_server_socket, "0.0.0.0", InpServerPort))
   {
      Print("[-] SocketListen failed on port ", InpServerPort, ". Error: ", GetLastError());
      SocketClose(g_server_socket);
      g_server_socket = INVALID_HANDLE;
      return INIT_FAILED;
   }

   EventSetTimer(InpTimerSeconds);
   Print("[+] MetaTrader Bridge EA initialized on port ", InpServerPort);
   return INIT_SUCCEEDED;
}

void OnDeinit(const int reason)
{
   EventKillTimer();
   if(g_client_socket != INVALID_HANDLE) SocketClose(g_client_socket);
   if(g_server_socket != INVALID_HANDLE) SocketClose(g_server_socket);
}

void OnTimer()
{
   if(g_server_socket == INVALID_HANDLE) return;
   if(g_client_socket == INVALID_HANDLE)
   {
      g_client_socket = SocketAccept(g_server_socket, 1);
      if(g_client_socket != INVALID_HANDLE) g_is_websocket = false;
   }

   if(g_client_socket != INVALID_HANDLE)
   {
      uint readable = SocketIsReadable(g_client_socket);
      if(readable > 0)
      {
         uchar buffer[];
         ArrayResize(buffer, (int)readable);
         int received = SocketRead(g_client_socket, buffer, (int)readable, 10);
         if(received > 0)
         {
            string rawStr = CharArrayToString(buffer, 0, received);
            if(!g_is_websocket && StringFind(rawStr, "Sec-WebSocket-Key:") >= 0)
            {
               // Handshake RFC 6455
               int keyPos = StringFind(rawStr, "Sec-WebSocket-Key:") + 19;
               while(keyPos < StringLen(rawStr) && StringGetCharacter(rawStr, keyPos) == ' ') keyPos++;
               int endPos = StringFind(rawStr, "\r\n", keyPos);
               string clientKey = StringSubstr(rawStr, keyPos, endPos - keyPos);
               string acceptSrc = clientKey + WS_GUID;
               uchar keyData[];
               StringToCharArray(acceptSrc, keyData, 0, StringLen(acceptSrc), CP_ACP);
               uchar sha1Hash[], b64Hash[], dummy[];
               CryptEncode(CRYPT_HASH_SHA1, keyData, dummy, sha1Hash);
               CryptEncode(CRYPT_BASE64, sha1Hash, dummy, b64Hash);
               string response = "HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: " + CharArrayToString(b64Hash) + "\r\n\r\n";
               uchar respBytes[];
               StringToCharArray(response, respBytes, 0, StringLen(response), CP_ACP);
               SocketSend(g_client_socket, respBytes, ArraySize(respBytes));
               g_is_websocket = true;
            }
            else if(g_is_websocket)
            {
               // Decode frame and execute command
               // ...
            }
         }
      }

      // Push positions update
      PushPositionsUpdate();
   }
}

void PushPositionsUpdate()
{
   if(g_client_socket == INVALID_HANDLE || !g_is_websocket) return;

   int total = PositionsTotal();
   string json = "{\"action\":\"POSITIONS_UPDATE\",\"data\":[";
   bool first = true;

   for(int i = 0; i < total; i++)
   {
      ulong ticket = PositionGetTicket(i);
      if(ticket == 0) continue;

      string symbol    = PositionGetString(POSITION_SYMBOL);
      int typeInt      = (PositionGetInteger(POSITION_TYPE) == POSITION_TYPE_BUY) ? 0 : 1;
      double volume    = PositionGetDouble(POSITION_VOLUME);
      double openPrice = PositionGetDouble(POSITION_PRICE_OPEN);
      double sl        = PositionGetDouble(POSITION_SL);
      double tp        = PositionGetDouble(POSITION_TP);
      double profit    = PositionGetDouble(POSITION_PROFIT) + PositionGetDouble(POSITION_SWAP);
      int digits       = (int)SymbolInfoInteger(symbol, SYMBOL_DIGITS);
      double point     = SymbolInfoDouble(symbol, SYMBOL_POINT);

      if(!first) json += ",";
      first = false;

      json += StringFormat(
         "{\"ticket\":%I64u,\"symbol\":\"%s\",\"type\":%d,\"volume\":%.2f,\"open_price\":%.*f,\"sl\":%.*f,\"tp\":%.*f,\"profit\":%.2f,\"digits\":%d,\"point_size\":%.*f}",
         ticket, symbol, typeInt, volume, digits, openPrice, digits, sl, digits, tp, profit, digits, digits, point
      );
   }
   json += "]}";

   // Send text frame
   uchar payload[];
   StringToCharArray(json, payload, 0, StringLen(json), CP_UTF8);
   int len = ArraySize(payload);
   uchar frame[];
   if(len <= 125)
   {
      ArrayResize(frame, 2 + len);
      frame[0] = 0x81;
      frame[1] = (uchar)len;
      ArrayCopy(frame, payload, 2, 0, len);
   }
   else
   {
      ArrayResize(frame, 4 + len);
      frame[0] = 0x81;
      frame[1] = 126;
      frame[2] = (uchar)((len >> 8) & 0xFF);
      frame[3] = (uchar)(len & 0xFF);
      ArrayCopy(frame, payload, 4, 0, len);
   }
   SocketSend(g_client_socket, frame, ArraySize(frame));
}
    """.trimIndent()
}
