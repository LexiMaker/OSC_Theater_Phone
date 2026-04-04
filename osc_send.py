#!/usr/bin/env python3
"""
OSC / Plain Text Sender für TheaterPhone v1.4.0
Sendet Befehle an die App zum Testen.

Verwendung (OSC – Standard):
  python3 osc_send.py call "Mama" "+49 171 1234567"
  python3 osc_send.py sms "Max" "Wo bist du?"
  python3 osc_send.py hangup
  python3 osc_send.py vibrate [single|pattern|stop]
  python3 osc_send.py ping

Verwendung (Plain Text):
  python3 osc_send.py --plain call "Mama" "+49 171 1234567"
  python3 osc_send.py --plain --tcp sms "Max" "Wo bist du?"
  python3 osc_send.py --plain ping

Interaktiver Modus:
  python3 osc_send.py
"""

import socket
import struct
import sys

TARGET_IP = "192.168.1.XXX"
TARGET_PORT = 9000


# --- OSC helpers ---

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


# --- Send functions ---

def send_osc(address: str, *args: str):
    msg = osc_message(address, *args)
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.sendto(msg, (TARGET_IP, TARGET_PORT))
    sock.close()
    arg_str = " ".join(f'"{a}"' for a in args)
    print(f"  → OSC {address} {arg_str}  an {TARGET_IP}:{TARGET_PORT}")


def send_plain(text: str, use_tcp: bool = False):
    if use_tcp:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(5)
        sock.connect((TARGET_IP, TARGET_PORT))
        sock.sendall((text + "\n").encode("utf-8"))
        sock.close()
        print(f"  → TCP \"{text}\"  an {TARGET_IP}:{TARGET_PORT}")
    else:
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.sendto((text + "\n").encode("utf-8"), (TARGET_IP, TARGET_PORT))
        sock.close()
        print(f"  → UDP \"{text}\"  an {TARGET_IP}:{TARGET_PORT}")


def send_ping_osc():
    """Sendet /ping (OSC) und wartet 3 Sekunden auf /pong Antwort."""
    msg = osc_message("/ping")
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.sendto(msg, (TARGET_IP, TARGET_PORT))
    print(f"  → OSC /ping an {TARGET_IP}:{TARGET_PORT}")
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


def send_ping_plain(use_tcp: bool = False):
    """Sendet ping (Plain Text) und wartet 3 Sekunden auf pong Antwort."""
    send_plain("ping", use_tcp)
    listen_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    listen_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    listen_sock.bind(("0.0.0.0", 9001))
    listen_sock.settimeout(3.0)
    try:
        data, addr = listen_sock.recvfrom(1024)
        text = data.decode("utf-8", errors="replace").strip()
        print(f"  ✅ Pong empfangen von {addr[0]}: \"{text}\"")
    except socket.timeout:
        print(f"  ❌ Kein Pong innerhalb von 3 Sekunden")
    finally:
        listen_sock.close()


# --- Build plain text command string ---

def build_plain_command(cmd: str, args: list) -> str:
    """Baut einen Plain-Text-Befehl mit Anführungszeichen bei Leerzeichen."""
    parts = [cmd]
    for a in args:
        if " " in a:
            parts.append(f'"{a}"')
        else:
            parts.append(a)
    return " ".join(parts)


# --- Interactive mode ---

def interactive():
    global TARGET_IP
    print(f"\n🎭 TheaterPhone Sender")
    print(f"   Ziel: {TARGET_IP}:{TARGET_PORT}")
    print(f"   (Ändere TARGET_IP im Script für echtes iPhone)\n")
    print(f"   Modus:  o = OSC (Standard)  |  p = Plain Text UDP  |  t = Plain Text TCP")
    print(f"   Befehle:")
    print(f"     1  Anruf auslösen")
    print(f"     2  Anruf beenden")
    print(f"     3  SMS senden")
    print(f"     4  Vibration (einmal)")
    print(f"     5  Vibration (Muster)")
    print(f"     6  Vibration stoppen")
    print(f"     7  Ping (wartet auf Pong)")
    print(f"     ip <adresse>  Ziel-IP ändern")
    print(f"     o/p/t  Modus wechseln")
    print(f"     q  Beenden\n")

    mode = "o"  # o=OSC, p=plain UDP, t=plain TCP
    mode_names = {"o": "OSC", "p": "Plain Text UDP", "t": "Plain Text TCP"}

    while True:
        try:
            cmd = input(f"  [{mode_names[mode]}] > ").strip()
        except (EOFError, KeyboardInterrupt):
            print()
            break

        if cmd == "q":
            break
        elif cmd in ("o", "p", "t"):
            mode = cmd
            print(f"    Modus: {mode_names[mode]}")
            continue
        elif cmd.startswith("ip "):
            TARGET_IP = cmd.split(" ", 1)[1].strip()
            print(f"    Ziel-IP geändert: {TARGET_IP}")
            continue

        if mode == "o":
            # OSC mode
            if cmd == "1":
                name = input("    Anrufer-Name: ").strip() or "Unbekannt"
                number = input("    Nummer (optional): ").strip()
                args = [name]
                if number: args.append(number)
                send_osc("/call", *args)
            elif cmd == "2":
                send_osc("/hangup")
            elif cmd == "3":
                sender = input("    Absender: ").strip() or "Unbekannt"
                text = input("    Text: ").strip() or "Hallo!"
                send_osc("/sms", sender, text)
            elif cmd == "4":
                send_osc("/vibrate", "single")
            elif cmd == "5":
                send_osc("/vibrate", "pattern")
            elif cmd == "6":
                send_osc("/vibrate", "stop")
            elif cmd == "7":
                send_ping_osc()
            else:
                print("    ? Unbekannter Befehl (1-7/o/p/t/ip/q)")
        else:
            # Plain text mode (UDP or TCP)
            use_tcp = (mode == "t")
            if cmd == "1":
                name = input("    Anrufer-Name: ").strip() or "Unbekannt"
                number = input("    Nummer (optional): ").strip()
                args = [name]
                if number: args.append(number)
                send_plain(build_plain_command("call", args), use_tcp)
            elif cmd == "2":
                send_plain("hangup", use_tcp)
            elif cmd == "3":
                sender = input("    Absender: ").strip() or "Unbekannt"
                text = input("    Text: ").strip() or "Hallo!"
                send_plain(build_plain_command("sms", [sender, text]), use_tcp)
            elif cmd == "4":
                send_plain("vibrate single", use_tcp)
            elif cmd == "5":
                send_plain("vibrate pattern", use_tcp)
            elif cmd == "6":
                send_plain("vibrate stop", use_tcp)
            elif cmd == "7":
                send_ping_plain(use_tcp)
            else:
                print("    ? Unbekannter Befehl (1-7/o/p/t/ip/q)")


# --- CLI mode ---

if __name__ == "__main__":
    args = sys.argv[1:]

    # Parse flags
    plain_mode = False
    use_tcp = False
    while args and args[0].startswith("--"):
        flag = args.pop(0)
        if flag == "--plain":
            plain_mode = True
        elif flag == "--tcp":
            use_tcp = True

    if not args:
        interactive()
        sys.exit(0)

    cmd = args[0].lower()
    cmd_args = args[1:]

    if plain_mode:
        # Plain text mode
        if cmd == "call":
            if not cmd_args: cmd_args = ["Unbekannt"]
            send_plain(build_plain_command("call", cmd_args), use_tcp)
        elif cmd in ("hangup", "endcall"):
            send_plain("hangup", use_tcp)
        elif cmd == "sms":
            sender = cmd_args[0] if cmd_args else "Unbekannt"
            text = " ".join(cmd_args[1:]) if len(cmd_args) > 1 else "Hallo!"
            send_plain(build_plain_command("sms", [sender, text]), use_tcp)
        elif cmd == "vibrate":
            vibmode = cmd_args[0] if cmd_args else "single"
            send_plain(f"vibrate {vibmode}", use_tcp)
        elif cmd == "ping":
            send_ping_plain(use_tcp)
        else:
            print(f"Unbekannter Befehl: {cmd}")
            print("Verfügbar: call, hangup, sms, vibrate, ping")
    else:
        # OSC mode (Standard)
        if cmd == "call":
            if not cmd_args: cmd_args = ["Unbekannt"]
            send_osc("/call", *cmd_args)
        elif cmd in ("hangup", "endcall"):
            send_osc("/hangup")
        elif cmd == "sms":
            sender = cmd_args[0] if cmd_args else "Unbekannt"
            text = " ".join(cmd_args[1:]) if len(cmd_args) > 1 else "Hallo!"
            send_osc("/sms", sender, text)
        elif cmd == "vibrate":
            vibmode = cmd_args[0] if cmd_args else "single"
            send_osc("/vibrate", vibmode)
        elif cmd == "ping":
            send_ping_osc()
        else:
            print(f"Unbekannter Befehl: {cmd}")
            print("Verfügbar: call, hangup, sms, vibrate, ping")
