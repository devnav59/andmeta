// ==UserScript==
// @name         TradingView Free Auto-Trader (MetaTrader Bridge)
// @namespace    http://tampermonkey.net/
// @version      1.0
// @description  Watches the SIGNAL row in TradingView and auto-sends trades to MetaTrader VPS Bridge!
// @author       MetaTrader Bridge
// @match        https://*.tradingview.com/chart/*
// @grant        GM_xmlhttpRequest
// ==/UserScript==

(function() {
    'use strict';
    let lastSignal = "FLAT";
    const VPS_BRIDGE_URL = "http://127.0.0.1:8080/api/command";

    console.log("[AutoTrader] TradingView Watcher initialized. Connecting to MetaTrader Bridge at " + VPS_BRIDGE_URL);

    function sendCommand(cmd, label) {
        console.log("[AutoTrader] >>> Sending command to MetaTrader:", label, cmd);
        GM_xmlhttpRequest({
            method: "POST",
            url: VPS_BRIDGE_URL,
            headers: { "Content-Type": "application/json" },
            data: JSON.stringify(cmd),
            onload: function(r) {
                console.log("[AutoTrader] Response from MetaTrader Bridge:", r.responseText);
            },
            onerror: function(err) {
                console.error("[AutoTrader] Error communicating with Bridge:", err);
            }
        });
    }

    setInterval(() => {
        // Find text elements on the page / chart table
        const allSpans = document.querySelectorAll('div, span, td');
        let currentSignal = null;

        for (let i = 0; i < allSpans.length; i++) {
            const txt = (allSpans[i].textContent || "").trim();
            if (txt === "BUY" || txt === "SELL" || txt.startsWith("CLOSE_") || txt.startsWith("BUY (HOLD)") || txt.startsWith("SELL (HOLD)")) {
                currentSignal = txt;
                break;
            }
        }

        if (currentSignal && currentSignal !== lastSignal) {
            console.log("[AutoTrader] *** SIGNAL CHANGE DETECTED:", lastSignal, "-->", currentSignal);
            lastSignal = currentSignal;

            if (currentSignal.includes("BUY")) {
                sendCommand({ action: "OPEN_ORDER", type: "BUY", volume: 0.01 }, "BUY");
            } else if (currentSignal.includes("SELL")) {
                sendCommand({ action: "OPEN_ORDER", type: "SELL", volume: 0.01 }, "SELL");
            } else if (currentSignal.includes("CLOSE")) {
                sendCommand({ action: "CLOSE_ALL" }, "CLOSE_ALL");
            }
        }
    }, 1000);
})();
