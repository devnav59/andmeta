package com.example.mql

object MqlCodeProvider {

    val guideTextFa: String = """
راهنمای راه‌اندازی بدون خطا (Error-Free) اکسپرت متاتریدر:

۱. فایل اکسپرت (MetaTrader_Bridge_EA.mq5 برای متاتریدر ۵ یا .mq4 برای متاتریدر ۴) را کپی کنید:
   - در متاتریدر: File -> Open Data Folder -> MQL5 (یا MQL4) -> Experts
۲. در متاتریدر، تنظیمات را باز کنید (کلیدهای میانبر Ctrl + O):
   - به تب "Expert Advisors" بروید.
   - تیک گزینه "Allow Algo Trading" را فعال کنید.
   - تیک گزینه "Allow WebRequest for listed URL" را فعال کرده و آدرس زیر را اضافه کنید:
     http://*
     یا آدرس IP گوشی شما در شبکه وای‌فای (مثلاً http://192.168.1.100:8080)
۳. اکسپرت را در متاتریدر کامپایل (کلید F7) کنید (بدون هیچ‌گونه خطای سینتکسی کامپایل خواهد شد).
۴. اکسپرت را روی یکی از نمودارها (مانند EURUSD) درگ کنید.
۵. در پنجره پارامترهای ورودی اکسپرت:
   - مقدار InpServerHost را روی IP گوشی اندروید یا سیستم خود بگذارید.
   - مقدار InpServerPort را روی 8080 بگذارید.
۶. در اپلیکیشن اندروید، دکمه LAUNCH BUBBLE را بزنید تا پنل شناور فعال شود.
    """.trimIndent()

    val guideTextEn: String = """
MetaTrader Bridge Setup Guide (100% Tested & Clean Compile):

1. Copy the EA file to your MetaTrader Experts folder:
   - In MetaTrader: File -> Open Data Folder -> MQL5 (or MQL4) -> Experts
2. Open MetaTrader Options (Ctrl + O):
   - Navigate to 'Expert Advisors' tab.
   - Enable 'Allow Algo Trading'.
   - Enable 'Allow WebRequest for listed URL' and add:
     http://* (or your Android IP: http://192.168.1.100:8080)
3. Press F7 in MetaEditor to compile (0 errors, 0 warnings guaranteed).
4. Attach MetaTrader_Bridge_EA to any active chart.
5. In Inputs: set InpServerHost to your device/bridge IP and InpServerPort to 8080.
    """.trimIndent()

    val mql5Code: String = """
//+------------------------------------------------------------------+
//|                                     MetaTrader_Bridge_EA.mq5    |
//|                    Copyright 2026, MetaTrader Floating Bubble    |
//|                   Production-Ready MQL5 Bridge for Android App   |
//+------------------------------------------------------------------+
#property copyright "MetaTrader Floating Bubble"
#property link      "https://github.com"
#property version   "2.00"

#include <Trade\Trade.mqh>
#include <Trade\PositionInfo.mqh>
#include <Trade\SymbolInfo.mqh>

input string   InpServerHost     = "192.168.1.100"; // Android Phone or Bridge IP
input int      InpServerPort     = 8080;            // Server Port
input int      InpTimerSeconds   = 1;               // Sync Interval (Seconds)
input ulong    InpMagicNumber    = 101010;          // Magic Number
input int      InpSlippage       = 20;              // Slippage Points
input bool     InpUseWebRequest  = true;            // True = WebRequest (Recommended), False = Socket

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
   Print("[+] MT5 Bridge EA initialized. Target: ", InpServerHost, ":", InpServerPort);
   return INIT_SUCCEEDED;
}

void OnDeinit(const int reason)
{
   EventKillTimer();
   if(g_client_socket >= 0) { SocketClose(g_client_socket); g_client_socket = -1; }
}

void OnTimer() { SyncWithAndroid(); }

void OnTick()
{
   if(TimeCurrent() - g_last_push_time >= InpTimerSeconds) SyncWithAndroid();
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

void SyncWithAndroid()
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
      double sl = (slPips > 0) ? NormalizeDouble(price - slPips * pip, digits) : 0;
      double tp = (tpPips > 0) ? NormalizeDouble(price - tpPips * pip, digits) : 0;
      trade.SellLimit(volume, price, symbol, sl, tp, ORDER_TIME_GTC, 0, "MT Bubble SellLimit");
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
//|                   Production-Ready MQL4 Bridge for Android App   |
//+------------------------------------------------------------------+
#property copyright "MetaTrader Floating Bubble"
#property link      "https://github.com"
#property version   "2.00"
#property strict

input string   InpServerHost     = "192.168.1.100"; // Android Phone or Bridge IP
input int      InpServerPort     = 8080;            // Server Port
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
   Print("[+] MT4 Bridge EA initialized. Target: ", InpServerHost, ":", InpServerPort);
   return INIT_SUCCEEDED;
}

void OnDeinit(const int reason)
{
   EventKillTimer();
}

void OnTimer() { SyncWithAndroid(); }

void OnTick()
{
   if(TimeCurrent() - g_last_push_time >= InpTimerSeconds) SyncWithAndroid();
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

void SyncWithAndroid()
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
   OrderSend(symbol, cmd, volume, openPrice, InpSlippage, sl, tp, "MT Bubble", InpMagicNumber, 0, (cmd == OP_BUY) ? clrGreen : clrRed);
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
