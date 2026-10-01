//+------------------------------------------------------------------+
//|                                     MetaTrader_Bridge_EA.mq4    |
//|                    Copyright 2026, MetaTrader Floating Bubble    |
//|                    Native WebSocket / TCP Bridge for Android     |
//+------------------------------------------------------------------+
#property copyright "MetaTrader Floating Bubble"
#property link      "https://github.com"
#property version   "1.00"
#property strict

input int      InpServerPort     = 8080;      // WebSocket/TCP Server Port
input int      InpTimerSeconds   = 1;         // Push Interval (Seconds)
input int      InpMagicNumber    = 101010;    // Magic Number

int            g_server_socket   = INVALID_HANDLE;
int            g_client_socket   = INVALID_HANDLE;
bool           g_is_websocket    = false;
datetime       g_last_push_time  = 0;

#define WS_GUID "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"

//+------------------------------------------------------------------+
//| Pip Calculation for 4/5 digits and 2/3 digits                    |
//| 5/3 digits: 1 Pip = 10 Points                                    |
//| 4/2 digits: 1 Pip = 1 Point                                     |
//+------------------------------------------------------------------+
double GetPipValue(string sym)
{
   int digits = (int)MarketInfo(sym, MODE_DIGITS);
   double point = MarketInfo(sym, MODE_POINT);
   if(digits == 3 || digits == 5)
      return point * 10.0;
   return point;
}

int OnInit()
{
   EventSetTimer(InpTimerSeconds);
   Print("[+] MetaTrader 4 Bridge EA initialized.");
   return INIT_SUCCEEDED;
}

void OnDeinit(const int reason)
{
   EventKillTimer();
   Print("[*] MetaTrader 4 Bridge EA deinitialized.");
}

void OnTimer()
{
   // Push open orders to connected clients
}

//+------------------------------------------------------------------+
//| Execute Open Order in MT4                                        |
//+------------------------------------------------------------------+
void ExecuteOpenOrderMT4(string symbol, string typeStr, double volume, double price, double slPips, double tpPips)
{
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
   else if(typeStr == "BUY_STOP")
   {
      cmd = OP_BUYSTOP;
      openPrice = (price > 0) ? price : ask + 20 * pip;
   }
   else if(typeStr == "SELL_STOP")
   {
      cmd = OP_SELLSTOP;
      openPrice = (price > 0) ? price : bid - 20 * pip;
   }

   bool isBuyType = (cmd == OP_BUY || cmd == OP_BUYLIMIT || cmd == OP_BUYSTOP);
   double sl = 0.0;
   double tp = 0.0;

   if(slPips > 0)
      sl = NormalizeDouble(isBuyType ? openPrice - slPips * pip : openPrice + slPips * pip, digits);
   if(tpPips > 0)
      tp = NormalizeDouble(isBuyType ? openPrice + tpPips * pip : openPrice - tpPips * pip, digits);

   int ticket = OrderSend(symbol, cmd, volume, openPrice, 3, sl, tp, "MT Bubble", InpMagicNumber, 0, (cmd == OP_BUY) ? clrGreen : clrRed);
   if(ticket > 0)
      Print("[+] MT4 Order placed successfully. Ticket: ", ticket);
   else
      Print("[-] MT4 OrderSend failed. Error: ", GetLastError());
}

//+------------------------------------------------------------------+
//| Modify SL/TP in MT4                                              |
//+------------------------------------------------------------------+
void ExecuteModifySlTpMT4(int ticket, double slDelta, double tpDelta)
{
   if(!OrderSelect(ticket, SELECT_BY_TICKET)) return;

   string symbol = OrderSymbol();
   int digits = (int)MarketInfo(symbol, MODE_DIGITS);
   double pip = GetPipValue(symbol);
   int type = OrderType();
   double currentSl = OrderStopLoss();
   double currentTp = OrderTakeProfit();
   double openPrice = OrderOpenPrice();

   double baseSl = (currentSl > 0) ? currentSl : openPrice;
   double baseTp = (currentTp > 0) ? currentTp : openPrice;

   double newSl = currentSl;
   double newTp = currentTp;

   if(type == OP_BUY || type == OP_BUYLIMIT || type == OP_BUYSTOP)
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
      Print("[+] MT4 Modified ticket #", ticket, " SL=", newSl, " TP=", newTp);
   else
      Print("[-] MT4 OrderModify failed. Error: ", GetLastError());
}

//+------------------------------------------------------------------+
//| Close Position in MT4                                            |
//+------------------------------------------------------------------+
void ExecuteClosePositionMT4(int ticket)
{
   if(!OrderSelect(ticket, SELECT_BY_TICKET)) return;

   double closePrice = (OrderType() == OP_BUY) ? MarketInfo(OrderSymbol(), MODE_BID) : MarketInfo(OrderSymbol(), MODE_ASK);
   if(OrderClose(ticket, OrderLots(), closePrice, 3, clrWhite))
      Print("[+] MT4 Position closed ticket #", ticket);
   else
      Print("[-] MT4 OrderClose failed. Error: ", GetLastError());
}
