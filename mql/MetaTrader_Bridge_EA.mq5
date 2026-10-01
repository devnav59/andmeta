//+------------------------------------------------------------------+
//|                                     MetaTrader_Bridge_EA.mq5    |
//|                    Copyright 2026, MetaTrader Floating Bubble    |
//|                        Native WebSocket Server for Android UI    |
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

//+------------------------------------------------------------------+
//| Helper: Calculate Exact Pip Value based on Digits                |
//| 5 & 3 digits: 1 Pip = 10 Points                                  |
//| 4 & 2 digits: 1 Pip = 1 Point                                   |
//+------------------------------------------------------------------+
double GetPipValue(string sym)
{
   int digits = (int)SymbolInfoInteger(sym, SYMBOL_DIGITS);
   double point = SymbolInfoDouble(sym, SYMBOL_POINT);
   if(digits == 3 || digits == 5)
      return point * 10.0;
   return point;
}

//+------------------------------------------------------------------+
//| Expert initialization function                                   |
//+------------------------------------------------------------------+
int OnInit()
{
   trade.SetExpertMagicNumber(InpMagicNumber);
   trade.SetDeviationInPoints(20);
   trade.SetTypeFilling(ORDER_FILLING_FOK);

   // Create and bind TCP socket on port
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
   Print("[+] MetaTrader Bridge EA initialized. Listening on port ", InpServerPort);
   return INIT_SUCCEEDED;
}

//+------------------------------------------------------------------+
//| Expert deinitialization function                                 |
//+------------------------------------------------------------------+
void OnDeinit(const int reason)
{
   EventKillTimer();
   if(g_client_socket != INVALID_HANDLE)
   {
      SocketClose(g_client_socket);
      g_client_socket = INVALID_HANDLE;
   }
   if(g_server_socket != INVALID_HANDLE)
   {
      SocketClose(g_server_socket);
      g_server_socket = INVALID_HANDLE;
   }
   Print("[*] MetaTrader Bridge EA deinitialized.");
}

//+------------------------------------------------------------------+
//| Timer event handler                                              |
//+------------------------------------------------------------------+
void OnTimer()
{
   AcceptClient();
   ReadClientData();
   PushPositionsUpdate();
}

//+------------------------------------------------------------------+
//| Tick event handler                                               |
//+------------------------------------------------------------------+
void OnTick()
{
   AcceptClient();
   ReadClientData();
   if(TimeCurrent() - g_last_push_time >= 1)
   {
      PushPositionsUpdate();
   }
}

//+------------------------------------------------------------------+
//| Accept incoming connection if none is active                     |
//+------------------------------------------------------------------+
void AcceptClient()
{
   if(g_server_socket == INVALID_HANDLE) return;

   if(g_client_socket == INVALID_HANDLE)
   {
      g_client_socket = SocketAccept(g_server_socket, 1);
      if(g_client_socket != INVALID_HANDLE)
      {
         Print("[+] Android Client connected to socket: ", g_client_socket);
         g_is_websocket = false;
      }
   }
}

//+------------------------------------------------------------------+
//| Read incoming client data and process WebSocket frames           |
//+------------------------------------------------------------------+
void ReadClientData()
{
   if(g_client_socket == INVALID_HANDLE) return;

   uint readable = SocketIsReadable(g_client_socket);
   if(readable == 0) return;

   uchar buffer[];
   ArrayResize(buffer, (int)readable);
   int received = SocketRead(g_client_socket, buffer, (int)readable, 10);
   if(received <= 0)
   {
      int err = GetLastError();
      if(err != 0)
      {
         Print("[-] SocketRead closed or error: ", err);
         SocketClose(g_client_socket);
         g_client_socket = INVALID_HANDLE;
         g_is_websocket = false;
      }
      return;
   }

   string rawStr = CharArrayToString(buffer, 0, received);

   // Check if this is an HTTP WebSocket Upgrade Request
   if(!g_is_websocket && StringFind(rawStr, "Sec-WebSocket-Key:") >= 0)
   {
      PerformWebSocketHandshake(rawStr);
      return;
   }

   // If already upgraded to WebSocket, decode frame
   if(g_is_websocket)
   {
      string payload = DecodeWebSocketFrame(buffer, received);
      if(StringLen(payload) > 0)
      {
         ProcessJsonCommand(payload);
      }
   }
   else
   {
      // Fallback plain JSON over TCP
      ProcessJsonCommand(rawStr);
   }
}

//+------------------------------------------------------------------+
//| Perform RFC 6455 WebSocket Handshake                             |
//+------------------------------------------------------------------+
void PerformWebSocketHandshake(string request)
{
   int keyPos = StringFind(request, "Sec-WebSocket-Key:");
   if(keyPos < 0) return;

   keyPos += 19;
   while(keyPos < StringLen(request) && StringGetCharacter(request, keyPos) == ' ') keyPos++;
   int endPos = StringFind(request, "\r\n", keyPos);
   if(endPos < 0) endPos = StringFind(request, "\n", keyPos);
   if(endPos < 0) return;

   string clientKey = StringSubstr(request, keyPos, endPos - keyPos);
   string acceptSrc = clientKey + WS_GUID;

   uchar keyData[];
   StringToCharArray(acceptSrc, keyData, 0, StringLen(acceptSrc), CP_ACP);

   uchar sha1Hash[];
   uchar dummyKey[];
   CryptEncode(CRYPT_HASH_SHA1, keyData, dummyKey, sha1Hash);

   uchar b64Hash[];
   CryptEncode(CRYPT_BASE64, sha1Hash, dummyKey, b64Hash);
   string acceptKey = CharArrayToString(b64Hash);

   string response = "HTTP/1.1 101 Switching Protocols\r\n" +
                     "Upgrade: websocket\r\n" +
                     "Connection: Upgrade\r\n" +
                     "Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n";

   uchar respBytes[];
   StringToCharArray(response, respBytes, 0, StringLen(response), CP_ACP);
   SocketSend(g_client_socket, respBytes, ArraySize(respBytes));

   g_is_websocket = true;
   Print("[+] WebSocket handshake completed successfully with Android Client.");
   PushPositionsUpdate();
}

//+------------------------------------------------------------------+
//| Decode masked WebSocket frame from Android client                |
//+------------------------------------------------------------------+
string DecodeWebSocketFrame(uchar &data[], int length)
{
   if(length < 2) return "";
   uchar b1 = data[0];
   uchar b2 = data[1];

   int opcode = b1 & 0x0F;
   if(opcode == 0x08) // Close frame
   {
      SocketClose(g_client_socket);
      g_client_socket = INVALID_HANDLE;
      g_is_websocket = false;
      return "";
   }

   bool masked = (b2 & 0x80) != 0;
   ulong payloadLen = b2 & 0x7F;
   int offset = 2;

   if(payloadLen == 126)
   {
      if(length < 4) return "";
      payloadLen = (data[2] << 8) | data[3];
      offset = 4;
   }
   else if(payloadLen == 127)
   {
      if(length < 10) return "";
      offset = 10;
   }

   uchar maskKey[4];
   if(masked)
   {
      if(length < offset + 4) return "";
      for(int i = 0; i < 4; i++) maskKey[i] = data[offset + i];
      offset += 4;
   }

   if(length < offset + (int)payloadLen) return "";

   uchar unmasked[];
   ArrayResize(unmasked, (int)payloadLen);
   for(ulong i = 0; i < payloadLen; i++)
   {
      if(masked)
         unmasked[i] = data[offset + (int)i] ^ maskKey[i % 4];
      else
         unmasked[i] = data[offset + (int)i];
   }

   return CharArrayToString(unmasked, 0, (int)payloadLen, CP_UTF8);
}

//+------------------------------------------------------------------+
//| Send WebSocket Text Frame to connected Android Client            |
//+------------------------------------------------------------------+
void SendWebSocketText(string text)
{
   if(g_client_socket == INVALID_HANDLE) return;

   uchar payload[];
   StringToCharArray(text, payload, 0, StringLen(text), CP_UTF8);
   int len = ArraySize(payload);

   uchar frame[];
   int headerSize = 2;
   if(len <= 125)
   {
      ArrayResize(frame, 2 + len);
      frame[0] = 0x81; // FIN + text opcode
      frame[1] = (uchar)len;
      headerSize = 2;
   }
   else if(len <= 65535)
   {
      ArrayResize(frame, 4 + len);
      frame[0] = 0x81;
      frame[1] = 126;
      frame[2] = (uchar)((len >> 8) & 0xFF);
      frame[3] = (uchar)(len & 0xFF);
      headerSize = 4;
   }
   else
   {
      ArrayResize(frame, 10 + len);
      frame[0] = 0x81;
      frame[1] = 127;
      for(int i = 0; i < 8; i++) frame[2 + i] = 0;
      frame[8] = (uchar)((len >> 8) & 0xFF);
      frame[9] = (uchar)(len & 0xFF);
      headerSize = 10;
   }

   ArrayCopy(frame, payload, headerSize, 0, len);

   int sent = SocketSend(g_client_socket, frame, ArraySize(frame));
   if(sent <= 0)
   {
      SocketClose(g_client_socket);
      g_client_socket = INVALID_HANDLE;
      g_is_websocket = false;
   }
}

//+------------------------------------------------------------------+
//| Push all open positions to Android in JSON format                |
//+------------------------------------------------------------------+
void PushPositionsUpdate()
{
   if(g_client_socket == INVALID_HANDLE || !g_is_websocket) return;

   g_last_push_time = TimeCurrent();
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

      json += StringFormat(
         "{\"ticket\":%I64u,\"symbol\":\"%s\",\"type\":%d,\"volume\":%.2f,\"open_price\":%.*f,\"sl\":%.*f,\"tp\":%.*f,\"profit\":%.2f,\"digits\":%d,\"point_size\":%.*f}",
         ticket, symbol, typeInt, volume, digits, openPrice, digits, sl, digits, tp, profit, digits, digits, point
      );
   }

   json += "]}";

   SendWebSocketText(json);
}

//+------------------------------------------------------------------+
//| Process incoming JSON command from Android                       |
//+------------------------------------------------------------------+
void ProcessJsonCommand(string json)
{
   Print("[*] Received JSON: ", json);

   string action = ExtractJsonString(json, "action");

   if(action == "OPEN_ORDER")
   {
      ExecuteOpenOrder(json);
   }
   else if(action == "MODIFY_SL_TP")
   {
      ExecuteModifySlTp(json);
   }
   else if(action == "CLOSE_POSITION")
   {
      ExecuteClosePosition(json);
   }
   else
   {
      Print("[!] Unknown action: ", action);
   }

   // Push immediate update after order execution
   PushPositionsUpdate();
}

//+------------------------------------------------------------------+
//| Handle OPEN_ORDER command                                        |
//+------------------------------------------------------------------+
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

   if(typeStr == "BUY")
   {
      double sl = (slPips > 0) ? NormalizeDouble(ask - slPips * pip, digits) : 0;
      double tp = (tpPips > 0) ? NormalizeDouble(ask + tpPips * pip, digits) : 0;
      trade.Buy(volume, symbol, ask, sl, tp, "MT Bubble Buy");
   }
   else if(typeStr == "SELL")
   {
      double sl = (slPips > 0) ? NormalizeDouble(bid + slPips * pip, digits) : 0;
      double tp = (tpPips > 0) ? NormalizeDouble(bid - tpPips * pip, digits) : 0;
      trade.Sell(volume, symbol, bid, sl, tp, "MT Bubble Sell");
   }
   else if(typeStr == "BUY_LIMIT")
   {
      if(price <= 0) price = ask - 20 * pip;
      double sl = (slPips > 0) ? NormalizeDouble(price - slPips * pip, digits) : 0;
      double tp = (tpPips > 0) ? NormalizeDouble(price + tpPips * pip, digits) : 0;
      trade.BuyLimit(volume, price, symbol, sl, tp, ORDER_TIME_GTC, 0, "MT Bubble BuyLimit");
   }
   else if(typeStr == "SELL_LIMIT")
   {
      if(price <= 0) price = bid + 20 * pip;
      double sl = (slPips > 0) ? NormalizeDouble(price + slPips * pip, digits) : 0;
      double tp = (tpPips > 0) ? NormalizeDouble(price - tpPips * pip, digits) : 0;
      trade.SellLimit(volume, price, symbol, sl, tp, ORDER_TIME_GTC, 0, "MT Bubble SellLimit");
   }
   else if(typeStr == "BUY_STOP")
   {
      if(price <= 0) price = ask + 20 * pip;
      double sl = (slPips > 0) ? NormalizeDouble(price - slPips * pip, digits) : 0;
      double tp = (tpPips > 0) ? NormalizeDouble(price + tpPips * pip, digits) : 0;
      trade.BuyStop(volume, price, symbol, sl, tp, ORDER_TIME_GTC, 0, "MT Bubble BuyStop");
   }
   else if(typeStr == "SELL_STOP")
   {
      if(price <= 0) price = bid - 20 * pip;
      double sl = (slPips > 0) ? NormalizeDouble(price + slPips * pip, digits) : 0;
      double tp = (tpPips > 0) ? NormalizeDouble(price - tpPips * pip, digits) : 0;
      trade.SellStop(volume, price, symbol, sl, tp, ORDER_TIME_GTC, 0, "MT Bubble SellStop");
   }
}

//+------------------------------------------------------------------+
//| Handle MODIFY_SL_TP command                                      |
//+------------------------------------------------------------------+
void ExecuteModifySlTp(string json)
{
   ulong ticket     = (ulong)ExtractJsonDouble(json, "ticket");
   double slDelta   = ExtractJsonDouble(json, "sl_pips_delta");
   double tpDelta   = ExtractJsonDouble(json, "tp_pips_delta");

   if(!PositionSelectByTicket(ticket))
   {
      Print("[-] Position not found for ticket: ", ticket);
      return;
   }

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
   else // SELL
   {
      if(slDelta != 0) newSl = NormalizeDouble(baseSl - (slDelta * pip), digits);
      if(tpDelta != 0) newTp = NormalizeDouble(baseTp - (tpDelta * pip), digits);
   }

   trade.PositionModify(ticket, newSl, newTp);
   Print("[+] Modified Position #", ticket, " newSL=", newSl, " newTP=", newTp);
}

//+------------------------------------------------------------------+
//| Handle CLOSE_POSITION command                                    |
//+------------------------------------------------------------------+
void ExecuteClosePosition(string json)
{
   ulong ticket = (ulong)ExtractJsonDouble(json, "ticket");
   if(ticket <= 0) return;

   if(trade.PositionClose(ticket))
   {
      Print("[+] Successfully closed position #", ticket);
   }
   else
   {
      Print("[-] Failed to close position #", ticket, " Error: ", trade.ResultRetcode());
   }
}

//+------------------------------------------------------------------+
//| Quick JSON extraction utilities                                  |
//+------------------------------------------------------------------+
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
//+------------------------------------------------------------------+
