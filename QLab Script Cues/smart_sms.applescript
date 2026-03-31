-- TheaterPhone Smart SMS – prüft ob App läuft via /ping → /pong

set phoneIP to "192.168.1.XXX"
set phonePort to "9000"
set listenPort to "9001"

set checkScript to "python3 -c \"
import socket,threading,time
ok=[False]
def listen():
    s=socket.socket(socket.AF_INET,socket.SOCK_DGRAM)
    s.settimeout(2)
    s.bind(('',int(" & listenPort & ")))
    try:
        d,_=s.recvfrom(256)
        if b'pong' in d: ok[0]=True
    except: pass
    s.close()
t=threading.Thread(target=listen);t.start()
time.sleep(0.05)
s=socket.socket(socket.AF_INET,socket.SOCK_DGRAM)
s.sendto(b'/ping\\x00\\x00\\x00,\\x00\\x00\\x00',('" & phoneIP & "'," & phonePort & "))
s.close()
t.join()
print('online' if ok[0] else 'offline')
\""

try
	set phoneStatus to do shell script checkScript
on error
	set phoneStatus to "offline"
end try

tell application id "com.figure53.QLab.5" to tell front workspace
	if phoneStatus is "online" then
		start cue "phone_sms"
	else
		start cue "fallback_sms"
	end if
end tell
