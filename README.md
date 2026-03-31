# TheaterPhone

An iOS app that receives **OSC commands** over Wi-Fi to simulate realistic incoming phone calls and SMS messages on stage. Designed for theater productions where an actor's phone needs to ring or receive messages on cue.

![iOS](https://img.shields.io/badge/iOS-17%2B-blue)
![Swift](https://img.shields.io/badge/Swift-5.9-orange)
![License](https://img.shields.io/badge/License-MIT-green)

## How It Works

A show control system (like **QLab**) sends an OSC command over the local network → the iPhone receives it and triggers a native phone call or SMS notification — indistinguishable from the real thing.

```
QLab / OSC Sender  ──UDP──▶  iPhone (TheaterPhone App)
                                 ├── /call  → Native incoming call (CallKit)
                                 ├── /sms   → Native notification + iMessage chat
                                 ├── /hangup → End call remotely
                                 ├── /vibrate → Vibration pulse or pattern
                                 └── /ping  → Responds with /pong (alive check)
```

## Features

- **Native incoming calls** via CallKit — full iOS call screen, works on lock screen
- **SMS notifications** via iOS notification system — tap to open iMessage-style chat
- **Ringtone & message tone** controlled via iPhone Settings → Sounds & Haptics (uses the real system sounds)
- **Background mode** — OSC listener stays active when the phone is locked
- **Ping/Pong** — QLab can check if the app is running before sending commands, with automatic fallback
- **Vibration control** — trigger vibration remotely via OSC

## OSC Commands

| Command | Arguments | Description |
|---------|-----------|-------------|
| `/call` | `<Name> [Number]` | Trigger incoming call |
| `/hangup` | — | End current call remotely |
| `/sms` | `<Sender> <Text>` | Send SMS notification |
| `/vibrate` | `[single\|pattern\|stop]` | Vibration only (default: single) |
| `/ping` | — | App responds with `/pong` on port 9001 |

### Examples

```
/call "Mom" "+1 555 1234567"    → Incoming call from Mom
/call "Director"                → Incoming call (no number shown)
/sms "Max" "Where are you?"    → SMS notification from Max
/hangup                         → Ends the active call
/vibrate pattern                → Repeating vibration
/vibrate stop                   → Stop vibration
/ping                           → Returns /theaterphone/pong on port 9001
```

## Requirements

- iPhone with **iOS 17** or later
- Mac with **Xcode 15+** for building
- Both devices on the **same Wi-Fi network**
- OSC-capable show control software (QLab, ETC Eos, etc.) or the included Python test script

## Installation

1. Clone this repository
2. Open `TheaterPhone/TheaterPhone.xcodeproj` in Xcode
3. Select your iPhone as the build target
4. Build and run (⌘R)
5. Allow notifications when prompted
6. Note the IP address shown on the lock screen

> **Tip:** Set your preferred ringtone and message tone in iPhone Settings → Sounds & Haptics before the show.

## Testing Without QLab

Use the included Python script to send OSC commands from any computer:

```bash
# Interactive mode
python3 osc_send.py

# Direct commands
python3 osc_send.py call "Mom" "+1 555 1234567"
python3 osc_send.py sms "Max" "Break a leg!"
python3 osc_send.py hangup
python3 osc_send.py vibrate pattern
python3 osc_send.py ping
```

Edit `TARGET_IP` in `osc_send.py` to match your iPhone's IP address (shown in the app).

## QLab 5 Integration

### Network Preset

1. Copy `TheaterPhone.qlabnetwork` to `/Applications/QLab.app/Contents/Resources/NetworkDeviceDescriptions`
2. In QLab: create a Network Cue → select "TheaterPhone" as the device
3. Choose the action (Call, Hang Up, SMS, Vibrate, Ping) and fill in the parameters

### Fallback Logic (Ping/Pong)

The included AppleScript cues check if the app is running before sending commands. If the app doesn't respond, a fallback cue is triggered instead (e.g., a sound effect from speakers).

**Setup:**

1. Create your phone cues with cue numbers: `phone_call`, `phone_sms`, `phone_hangup`
2. Create fallback cues: `fallback_call`, `fallback_sms`, `fallback_hangup`
3. Create a Script Cue and paste the content of the matching script from `QLab Script Cues/`:
   - `smart_call.applescript` — for call cues
   - `smart_sms.applescript` — for SMS cues
   - `smart_hangup.applescript` — for hangup cues
4. Set `phoneIP` in the script to your iPhone's IP address

Each script sends `/ping`, waits 2 seconds for `/pong`, then triggers either `phone_<action>` or `fallback_<action>`.

## Project Structure

```
├── TheaterPhone/                  # Xcode project
│   └── TheaterPhone/
│       ├── TheaterPhoneApp.swift   # App entry point
│       ├── Models/
│       │   ├── CallState.swift     # Call state machine
│       │   └── SMSState.swift      # SMS message model
│       ├── Services/
│       │   ├── OSCManager.swift    # OSC listener + parser (UDP)
│       │   ├── CallKitService.swift # Native iOS call integration
│       │   ├── NotificationService.swift # iOS notifications
│       │   ├── AudioService.swift  # Vibration control
│       │   └── BackgroundService.swift # Keeps app alive when locked
│       └── Views/
│           ├── ContentView.swift   # Main view controller
│           ├── LockScreenView.swift # Idle screen with OSC status
│           ├── ActiveCallView.swift # In-call screen (after accepting)
│           ├── CallEndedView.swift  # Brief "Call Ended" display
│           ├── SMSConversationView.swift # iMessage-style chat
│           └── SettingsView.swift   # OSC config + test buttons
├── QLab Script Cues/              # AppleScript fallback cues
├── TheaterPhone.qlabnetwork       # QLab 5 network device preset
├── osc_send.py                    # Python OSC test tool
└── osc_ping_test.py               # Ping/Pong debug script
```

## How It Stays Alive on Lock Screen

TheaterPhone uses three mechanisms to work reliably when the phone is locked:

1. **CallKit** — iOS natively handles incoming call UI on the lock screen
2. **Background Audio** — A silent audio loop keeps the app process alive
3. **Local Notifications** — SMS messages appear as native iOS notifications

The app requests `audio` and `voip` background modes in `Info.plist`.

## Inspiration

*Reading about Stage Caller was the inspiration for tackling this project.*

## License

MIT — see [LICENSE](LICENSE)
