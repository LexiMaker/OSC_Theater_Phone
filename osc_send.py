#!/usr/bin/env python3
"""
OSC Sender für TheaterPhone v1.2.0
Sendet OSC-Befehle an die App zum Testen.

Verwendung:
  python3 osc_send.py call "Mama" "+49 171 1234567"
  python3 osc_send.py sms "Max" "Wo bist du?"
  python3 osc_send.py hangup
  python3 osc_send.py vibrate [single|pattern|stop]
  python3 osc_send.py ping
  python3 osc_send.py              (interaktiver Modus)
"""

import socket
import struct
import sys

TARGET_IP = "192.168.1.XXX"
TARGET_PORT = 9000


def osc_string(s: str) -> bytes:
    """OSC-String: null-terminated, padded to 4-byte boundary."""
    b = s.encode("utf-8") + b"\x00"
    b += b"\x00" * ((4 - len(b) % 4) % 4)
    return b


def osc_message(address: str, *args: str) -> bytes:
    """Baut eine OSC-Nachricht mit String-Argumenten."""
    msg = osc_string(address)
    type_tag = "," + "s" * len(args)
    msg += osc_string(type_tag)
    for arg in args:
        msg += osc_string(arg)
    return msg


def send(address: str, *args: str):
    msg = osc_message(address, *args)
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.sendto(msg, (TARGET_IP, TARGET_PORT))
    sock.close()
    arg_str = " ".join(f'"{a}"' for a in args)
    print(f"  → {address} {arg_str}  an {TARGET_IP}:{TARGET_PORT}")


def send_ping():
    """Sendet /ping und wartet 3 Sekunden auf /pong Antwort."""
    msg = osc_message("/ping")
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.sendto(msg, (TARGET_IP, TARGET_PORT))
    print(f"  → /ping an {TARGET_IP}:{TARGET_PORT}")
    # Listen for pong on port 9001
    listen_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    listen_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    listen_sock.bind(("0.0.0.0", 9001))
    listen_sock.settimeout(3.0)
    try:
        data, addr = listen_sock.recvfrom(1024)
        print(f"  ✅ Pong empfangen von {addr[0]} ({len(data)} bytes)")
    except socket.timeout:
        print(f"  ❌ Kein Pong innerhalb von 3 Sekunden")
    finally:
        listen_sock.close()
        sock.close()


def interactive():
    global TARGET_IP
    print(f"\n🎭 TheaterPhone OSC Sender")
    print(f"   Ziel: {TARGET_IP}:{TARGET_PORT}")
    print(f"   (Ändere TARGET_IP im Script für echtes iPhone)\n")
    print(f"   Befehle:")
    print(f"     1  Anruf auslösen")
    print(f"     2  Anruf beenden")
    print(f"     3  SMS senden")
    print(f"     4  Vibration (einmal)")
    print(f"     5  Vibration (Muster)")
    print(f"     6  Vibration stoppen")
    print(f"     7  Ping (wartet auf Pong)")
    print(f"     ip <adresse>  Ziel-IP ändern")
    print(f"     q  Beenden\n")
    while True:
        try:
            cmd = input("  > ").strip()
        except (EOFError, KeyboardInterrupt):
            print()
            break

        if cmd == "q":
            break
        elif cmd == "1":
            name = input("    Anrufer-Name: ").strip() or "Unbekannt"
            number = input("    Nummer (optional): ").strip()
            args = [name]
            if number:
                args.append(number)
            send("/call", *args)
        elif cmd == "2":
            send("/hangup")
        elif cmd == "3":
            sender = input("    Absender: ").strip() or "Unbekannt"
            text = input("    Text: ").strip() or "Hallo!"
            send("/sms", sender, text)
        elif cmd == "4":
            send("/vibrate", "single")
        elif cmd == "5":
            send("/vibrate", "pattern")
        elif cmd == "6":
            send("/vibrate", "stop")
        elif cmd == "7":
            send_ping()
        elif cmd.startswith("ip "):
            TARGET_IP = cmd.split(" ", 1)[1].strip()
            print(f"    Ziel-IP geändert: {TARGET_IP}")
        else:
            print("    ? Unbekannter Befehl (1-7/ip/q)")


if __name__ == "__main__":
    if len(sys.argv) > 1:
        cmd = sys.argv[1].lower()
        if cmd == "call":
            args = sys.argv[2:]
            if not args:
                args = ["Unbekannt"]
            send("/call", *args)
        elif cmd in ("hangup", "endcall"):
            send("/hangup")
        elif cmd == "sms":
            sender = sys.argv[2] if len(sys.argv) > 2 else "Unbekannt"
            text = " ".join(sys.argv[3:]) if len(sys.argv) > 3 else "Hallo!"
            send("/sms", sender, text)
        elif cmd == "vibrate":
            mode = sys.argv[2] if len(sys.argv) > 2 else "single"
            send("/vibrate", mode)
        elif cmd == "ping":
            send_ping()
        else:
            print(f"Unbekannter Befehl: {cmd}")
            print("Verfügbar: call, hangup, sms, vibrate, ping")
    else:
        interactive()
