#!/usr/bin/env python3
"""Testet ob die TheaterPhone App antwortet."""

import socket, threading, time, sys

PHONE_IP = sys.argv[1] if len(sys.argv) > 1 else "192.168.1.XXX"
PHONE_PORT = 9000
LISTEN_PORT = 9001
TIMEOUT = 2

ok = [False]

def listen():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.settimeout(TIMEOUT)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    s.bind(('', LISTEN_PORT))
    try:
        d, addr = s.recvfrom(256)
        print(f"  Antwort von {addr}: {d}", file=sys.stderr)
        if b'pong' in d:
            ok[0] = True
    except socket.timeout:
        print(f"  Keine Antwort nach {TIMEOUT}s", file=sys.stderr)
    except Exception as e:
        print(f"  Fehler: {e}", file=sys.stderr)
    s.close()

t = threading.Thread(target=listen)
t.start()
time.sleep(0.05)

print(f"  Sende /ping an {PHONE_IP}:{PHONE_PORT}...", file=sys.stderr)
s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
s.sendto(b'/ping\x00\x00\x00,\x00\x00\x00', (PHONE_IP, PHONE_PORT))
s.close()

t.join()
status = "online" if ok[0] else "offline"
print(f"  Status: {status}", file=sys.stderr)
print(status)
