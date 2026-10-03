//+------------------------------------------------------------------+
//|                                     MetaTrader_Bridge_EA.mq4    |
//|                    Copyright 2026, MetaTrader Floating Bubble    |
//|               Production-Ready MQL4 Bridge for Windows VPS       |
//+------------------------------------------------------------------+
#property copyright "MetaTrader Floating Bubble"
#property link      "https://github.com"
#property version   "3.00"
#property strict

//--- Input parameters
input string   InpServerHost     = "127.0.0.1";     // Local Bridge IP on VPS
input int      InpServerPort     = 8080;            // Bridge Port
input int      InpTimerSeconds   = 1;               // Sync Interval (Seconds)
input int      InpMagicNumber    = 101010;          // Magic Number
input int      InpSlippage       = 20;              // Slippage Points

datetime       g_last_push_time  = 0;

//+------------------------------------------------------------------+
//| Helper: Calculate Exact Pip Value based on Digits                |
//| 5 & 3 digits: 1 Pip = 10 Points                                  |
//| 4 & 2 digits: 1 Pip = 1 Point                                   |
//+------------------------------------------------------------------+
double GetPipValue(string sym)
{
   int digits = (int)MarketInfo(sym, MODE_DIGITS);
   double point = MarketInfo(sym, MODE_POINT);
   if(digits == 3 || digits == 5)
      return point * 10.0;
   return point;
}

//+------------------------------------------------------------------+
//| Expert initialization function                                   |
//+------------------------------------------------------------------+
int OnInit()
{
   EventSetTimer(InpTimerSeconds);
   Print("[+] MetaTrader 4 VPS Bridge EA initialized. Target: ", InpServerHost, ":", InpServerPort);
   return INIT_SUCCEEDED;
}

//+------------------------------------------------------------------+
//| Expert deinitialization function                                 |
//+------------------------------------------------------------------+
void OnDeinit(const int reason)
{
   EventKillTimer();
   Print("[*] MetaTrader 4 VPS Bridge EA deinitialized.");
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
//| Build JSON array of all open positions in MT4                    |
//+------------------------------------------------------------------+
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

      json += StringFormat(
         "{\"ticket\":%d,\"symbol\":\"%s\",\"type\":%d,\"volume\":%.2f,\"open_price\":%.*f,\"sl\":%.*f,\"tp\":%.*f,\"profit\":%.2f,\"digits\":%d,\"point_size\":%.*f}",
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
         Print("[-] WebRequest Error 4060: In MT4 on VPS, go to Tools -> Options -> Expert Advisors -> Allow WebRequest and add http://", InpServerHost, ":", InpServerPort);
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
//| Handle OPEN_ORDER command in MT4                                 |
//+------------------------------------------------------------------+
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

   if(typeStr == "BUY")
   {
      cmd = OP_BUY;
      openPrice = ask;
   }
   else if(typeStr == "SELL")
   {
      cmd = OP_SELL;
      openPrice = bid;
   }
   else if(typeStr == "BUY_LIMIT")
   {
      cmd = OP_BUYLIMIT;
      openPrice = (price > 0) ? price : ask - 20 * pip;
   }
   else if(typeStr == "SELL_LIMIT")
   {
      cmd = OP_SELLLIMIT;
      openPrice = (price > 0) ? price : bid + 20 * pip;
   }

   bool isBuyType = (cmd == OP_BUY || cmd == OP_BUYLIMIT);
   double sl = 0.0;
   double tp = 0.0;

   if(slPips > 0)
      sl = NormalizeDouble(isBuyType ? openPrice - slPips * pip : openPrice + slPips * pip, digits);
   if(tpPips > 0)
      tp = NormalizeDouble(isBuyType ? openPrice + tpPips * pip : openPrice - tpPips * pip, digits);

   int ticket = OrderSend(symbol, cmd, volume, openPrice, InpSlippage, sl, tp, "VPS Bubble", InpMagicNumber, 0, (cmd == OP_BUY) ? clrGreen : clrRed);
   if(ticket > 0)
      Print("[+] VPS MT4 Order placed successfully. Ticket: ", ticket);
   else
      Print("[-] VPS MT4 OrderSend failed. Error: ", GetLastError());
}

//+------------------------------------------------------------------+
//| Handle MODIFY_SL_TP command in MT4                               |
//+------------------------------------------------------------------+
void ExecuteModifySlTp(string json)
{
   int ticket       = (int)ExtractJsonDouble(json, "ticket");
   double slDelta   = ExtractJsonDouble(json, "sl_pips_delta");
   double tpDelta   = ExtractJsonDouble(json, "tp_pips_delta");

   if(!OrderSelect(ticket, SELECT_BY_TICKET))
   {
      Print("[-] Position not found for ticket: ", ticket);
      return;
   }

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

   if(type == OP_BUY || type == OP_BUYLIMIT)
   {
      if(slDelta != 0) newSl = NormalizeDouble(baseSl + (slDelta * pip), digits);
      if(tpDelta != 0) newTp = NormalizeDouble(baseTp + (tpDelta * pip), digits);
   }
   else
   {
      if(slDelta != 0) newSl = NormalizeDouble(baseSl - (slDelta * pip), digits);
      if(tpDelta != 0) newTp = NormalizeDouble(baseTp - (tpDelta * pip), digits);
   }

   if(OrderModify(ticket, openPrice, newSl, newTp, 0, clrOrange))
      Print("[+] VPS MT4 Modified ticket #", ticket, " SL=", newSl, " TP=", newTp);
   else
      Print("[-] VPS MT4 OrderModify failed. Error: ", GetLastError());
}

//+------------------------------------------------------------------+
//| Handle CLOSE_POSITION command in MT4                             |
//+------------------------------------------------------------------+
void ExecuteClosePosition(string json)
{
   int ticket = (int)ExtractJsonDouble(json, "ticket");
   if(ticket <= 0) return;

   if(!OrderSelect(ticket, SELECT_BY_TICKET)) return;

   double closePrice = (OrderType() == OP_BUY) ? MarketInfo(OrderSymbol(), MODE_BID) : MarketInfo(OrderSymbol(), MODE_ASK);
   if(OrderClose(ticket, OrderLots(), closePrice, InpSlippage, clrWhite))
      Print("[+] VPS MT4 Position closed ticket #", ticket);
   else
      Print("[-] VPS MT4 OrderClose failed. Error: ", GetLastError());
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
