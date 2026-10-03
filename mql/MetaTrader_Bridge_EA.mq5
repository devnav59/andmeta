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

//--- Input parameters
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
//| Set Safe Order Filling Type                                      |
//+------------------------------------------------------------------+
void SetSafeFillingType()
{
   uint filling = (uint)SymbolInfoInteger(_Symbol, SYMBOL_FILLING_MODE);
   if((filling & SYMBOL_FILLING_FOK) != 0)
      trade.SetTypeFilling(ORDER_FILLING_FOK);
   else if((filling & SYMBOL_FILLING_IOC) != 0)
      trade.SetTypeFilling(ORDER_FILLING_IOC);
   else
      trade.SetTypeFilling(ORDER_FILLING_RETURN);
}

//+------------------------------------------------------------------+
//| Expert initialization function                                   |
//+------------------------------------------------------------------+
int OnInit()
{
   trade.SetExpertMagicNumber(InpMagicNumber);
   trade.SetDeviationInPoints(InpSlippage);
   SetSafeFillingType();

   EventSetTimer(InpTimerSeconds);
   Print("[+] MetaTrader 5 VPS Bridge EA initialized. Target: ", InpServerHost, ":", InpServerPort);
   return INIT_SUCCEEDED;
}

//+------------------------------------------------------------------+
//| Expert deinitialization function                                 |
//+------------------------------------------------------------------+
void OnDeinit(const int reason)
{
   EventKillTimer();
   if(g_client_socket >= 0)
   {
      SocketClose(g_client_socket);
      g_client_socket = -1;
   }
   Print("[*] MetaTrader 5 VPS Bridge EA deinitialized.");
}

//+------------------------------------------------------------------+
//| Timer event handler                                              |
//+------------------------------------------------------------------+
void OnTimer()
{
   SyncWithBridge();
}

//+------------------------------------------------------------------+
//| Tick event handler                                               |
//+------------------------------------------------------------------+
void OnTick()
{
   if(TimeCurrent() - g_last_push_time >= InpTimerSeconds)
   {
      SyncWithBridge();
   }
}

//+------------------------------------------------------------------+
//| Build JSON array of all open positions                           |
//+------------------------------------------------------------------+
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

      json += StringFormat(
         "{\"ticket\":%I64u,\"symbol\":\"%s\",\"type\":%d,\"volume\":%.2f,\"open_price\":%.*f,\"sl\":%.*f,\"tp\":%.*f,\"profit\":%.2f,\"digits\":%d,\"point_size\":%.*f}",
         ticket, symbol, typeInt, volume, digits, openPrice, digits, sl, digits, tp, profit, digits, digits, point
      );
   }

   json += "]}";
   return json;
}

//+------------------------------------------------------------------+
//| Sync positions with VPS Bridge and execute received commands     |
//+------------------------------------------------------------------+
void SyncWithBridge()
{
   g_last_push_time = TimeCurrent();
   string jsonPayload = BuildPositionsJson();

   if(InpUseWebRequest)
   {
      SyncViaWebRequest(jsonPayload);
   }
   else if(InpUseSocket)
   {
      SyncViaSocket(jsonPayload);
   }
}

//+------------------------------------------------------------------+
//| Sync via native WebRequest (HTTP POST on VPS localhost)          |
//+------------------------------------------------------------------+
void SyncViaWebRequest(string jsonPayload)
{
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
      if(StringLen(responseStr) > 0)
      {
         ProcessJsonCommands(responseStr);
      }
   }
   else if(res == -1)
   {
      int err = GetLastError();
      if(err == 4060)
      {
         Print("[-] WebRequest Error 4060: In MT5 on VPS, go to Tools -> Options -> Expert Advisors -> Allow WebRequest and add http://", InpServerHost, ":", InpServerPort);
      }
   }
}

//+------------------------------------------------------------------+
//| Sync via native MQL5 Client Socket                               |
//+------------------------------------------------------------------+
void SyncViaSocket(string jsonPayload)
{
   if(g_client_socket < 0 || !SocketIsConnected(g_client_socket))
   {
      if(g_client_socket >= 0) SocketClose(g_client_socket);
      g_client_socket = SocketCreate();
      if(g_client_socket < 0) return;

      if(!SocketConnect(g_client_socket, InpServerHost, InpServerPort, 1000))
      {
         SocketClose(g_client_socket);
         g_client_socket = -1;
         return;
      }
   }

   string request = jsonPayload + "\n";
   uchar sendData[];
   StringToCharArray(request, sendData, 0, StringLen(request), CP_UTF8);
   int sent = SocketSend(g_client_socket, sendData, ArraySize(sendData));
   if(sent <= 0)
   {
      SocketClose(g_client_socket);
      g_client_socket = -1;
      return;
   }

   uint readable = SocketIsReadable(g_client_socket);
   if(readable > 0)
   {
      uchar buffer[];
      ArrayResize(buffer, (int)readable);
      int received = SocketRead(g_client_socket, buffer, readable, 500);
      if(received > 0)
      {
         string resp = CharArrayToString(buffer, 0, received, CP_UTF8);
         ProcessJsonCommands(resp);
      }
   }
}

//+------------------------------------------------------------------+
//| Process received JSON commands from Android                      |
//+------------------------------------------------------------------+
void ProcessJsonCommands(string json)
{
   if(StringFind(json, "OPEN_ORDER") >= 0)
   {
      ExecuteOpenOrder(json);
   }
   if(StringFind(json, "MODIFY_SL_TP") >= 0)
   {
      ExecuteModifySlTp(json);
   }
   if(StringFind(json, "CLOSE_POSITION") >= 0)
   {
      ExecuteClosePosition(json);
   }
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

   SetSafeFillingType();

   if(typeStr == "BUY")
   {
      double sl = (slPips > 0) ? NormalizeDouble(ask - slPips * pip, digits) : 0;
      double tp = (tpPips > 0) ? NormalizeDouble(ask + tpPips * pip, digits) : 0;
      trade.Buy(volume, symbol, ask, sl, tp, "VPS Bubble Buy");
   }
   else if(typeStr == "SELL")
   {
      double sl = (slPips > 0) ? NormalizeDouble(bid + slPips * pip, digits) : 0;
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
   else if(typeStr == "BUY_STOP")
   {
      if(price <= 0) price = ask + 20 * pip;
      double sl = (slPips > 0) ? NormalizeDouble(price - slPips * pip, digits) : 0;
      double tp = (tpPips > 0) ? NormalizeDouble(price + tpPips * pip, digits) : 0;
      trade.BuyStop(volume, price, symbol, sl, tp, ORDER_TIME_GTC, 0, "VPS Bubble BuyStop");
   }
   else if(typeStr == "SELL_STOP")
   {
      if(price <= 0) price = bid - 20 * pip;
      double sl = (slPips > 0) ? NormalizeDouble(price - slPips * pip, digits) : 0;
      double tp = (tpPips > 0) ? NormalizeDouble(price - tpPips * pip, digits) : 0;
      trade.SellStop(volume, price, symbol, sl, tp, ORDER_TIME_GTC, 0, "VPS Bubble SellStop");
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
//| Fast JSON value extraction utilities                             |
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
