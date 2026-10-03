#!/usr/bin/env python3
"""
MetaTrader VPS Bridge Server (Zero-Dependency Python 3)
Specially crafted for Windows VPS running MetaTrader 4 / MetaTrader 5
and remote Android Phone connecting over Internet (4G/5G/Wi-Fi).

Architecture:
1. Listens on 0.0.0.0:8080 (publicly accessible on VPS).
2. Android Phone connects as WebSocket client: ws://<VPS_IP>:8080
3. MetaTrader EA on VPS pushes positions to http://127.0.0.1:8080/api/positions
4. Real-time 2-way sync: Position updates pushed to Android; orders from Android sent to MT.
"""

import sys
import json
import threading
import socket
import hashlib
import base64
import struct
from http.server import HTTPServer, BaseHTTPRequestHandler
from socketserver import ThreadingMixIn

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 8080

lock = threading.Lock()
latest_positions = []
pending_commands = []
connected_ws_clients = []

def ws_handshake_key(key):
    guid = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
    sha1 = hashlib.sha1((key + guid).encode('utf-8')).digest()
    return base64.b64encode(sha1).decode('utf-8')

def broadcast_positions(positions_data):
    msg = json.dumps({"action": "POSITIONS_UPDATE", "data": positions_data})
    payload = msg.encode('utf-8')
    length = len(payload)

    if length <= 125:
        header = bytes([0x81, length])
    elif length <= 65535:
        header = struct.pack('!BBH', 0x81, 126, length)
    else:
        header = struct.pack('!BBQ', 0x81, 127, length)

    frame = header + payload

    with lock:
        dead = []
        for client in connected_ws_clients:
            try:
                client.sendall(frame)
            except Exception:
                dead.append(client)
        for client in dead:
            if client in connected_ws_clients:
                connected_ws_clients.remove(client)

class ThreadedHTTPServer(ThreadingMixIn, HTTPServer):
    daemon_threads = True

class BridgeHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        # WebSocket Upgrade Check
        upgrade = self.headers.get('Upgrade', '')
        if upgrade.lower() == 'websocket':
            key = self.headers.get('Sec-WebSocket-Key', '')
            if key:
                accept_key = ws_handshake_key(key)
                response = (
                    "HTTP/1.1 101 Switching Protocols\r\n"
                    "Upgrade: websocket\r\n"
                    "Connection: Upgrade\r\n"
                    f"Sec-WebSocket-Accept: {accept_key}\r\n\r\n"
                )
                self.wfile.write(response.encode('utf-8'))

                sock = self.connection
                sock.setblocking(True)
                with lock:
                    connected_ws_clients.append(sock)
                print(f"[+] Android client connected via WebSocket from {self.client_address[0]}")

                # Push latest positions immediately on connect
                with lock:
                    current = list(latest_positions)
                if current:
                    broadcast_positions(current)

                self.handle_websocket(sock)
                return

        if self.path == '/' or self.path == '/status' or self.path == '/api/status':
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            with lock:
                info = {
                    "status": "online",
                    "server": "MetaTrader VPS Bridge",
                    "open_positions": len(latest_positions),
                    "ws_clients": len(connected_ws_clients),
                    "pending_commands": len(pending_commands)
                }
            self.wfile.write(json.dumps(info).encode('utf-8'))
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        content_len = int(self.headers.get('Content-Length', 0))
        post_body = self.rfile.read(content_len).decode('utf-8', errors='ignore')

        global latest_positions
        try:
            data = json.loads(post_body)
            action = data.get('action')

            if action == 'POSITIONS_UPDATE':
                positions = data.get('data', [])
                with lock:
                    latest_positions = positions
                broadcast_positions(positions)

                # Return pending commands to MetaTrader EA
                with lock:
                    cmds = list(pending_commands)
                    pending_commands.clear()

                resp = json.dumps({"status": "ok", "commands": cmds})
                self.send_response(200)
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                self.wfile.write(resp.encode('utf-8'))
                return

            elif action in ['OPEN_ORDER', 'MODIFY_SL_TP', 'CLOSE_POSITION']:
                with lock:
                    pending_commands.append(data)
                print(f"[+] Command received via HTTP: {action}")

                self.send_response(200)
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                self.wfile.write(json.dumps({"status": "queued"}).encode('utf-8'))
                return

        except Exception as e:
            print(f"[-] POST processing error: {e}")

        self.send_response(400)
        self.end_headers()

    def handle_websocket(self, sock):
        while True:
            try:
                head = sock.recv(2)
                if not head or len(head) < 2:
                    break
                b1, b2 = head[0], head[1]
                opcode = b1 & 0x0F
                if opcode == 0x08: # close
                    break

                is_masked = (b2 & 0x80) != 0
                length = b2 & 0x7F
                if length == 126:
                    length = struct.unpack('!H', sock.recv(2))[0]
                elif length == 127:
                    length = struct.unpack('!Q', sock.recv(8))[0]

                mask = sock.recv(4) if is_masked else b''
                data = bytearray()
                while len(data) < length:
                    chunk = sock.recv(length - len(data))
                    if not chunk:
                        break
                    data.extend(chunk)

                if is_masked:
                    for i in range(len(data)):
                        data[i] ^= mask[i % 4]

                text = data.decode('utf-8', errors='ignore')
                print(f"[WS Command from Android]: {text}")

                cmd = json.loads(text)
                if cmd.get('action') in ['OPEN_ORDER', 'MODIFY_SL_TP', 'CLOSE_POSITION']:
                    with lock:
                        pending_commands.append(cmd)

            except Exception:
                break

        with lock:
            if sock in connected_ws_clients:
                connected_ws_clients.remove(sock)
        print("[-] Android WebSocket client disconnected")

    def log_message(self, format, *args):
        return

def run_server():
    server = ThreadedHTTPServer(('0.0.0.0', PORT), BridgeHandler)
    print(f"============================================================")
    print(f"[*] MetaTrader VPS Bridge Server running on 0.0.0.0:{PORT}")
    print(f"[*] Windows VPS Ready - Auto threading enabled")
    print(f"[*] Android App URL: ws://<YOUR_VPS_PUBLIC_IP>:{PORT}")
    print(f"[*] MT4/MT5 EA Target: http://127.0.0.1:{PORT}/api/positions")
    print(f"============================================================")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        server.server_close()
        print("\n[*] Server terminated.")

if __name__ == '__main__':
    run_server()
