# TheaterPhone

An **iOS & Android** app that receives **OSC commands** (or **plain text** via UDP/TCP) over Wi-Fi to simulate realistic incoming phone calls and SMS messages on stage. Designed for theater productions where an actor's phone needs to ring or receive messages on cue.

![iOS](https://img.shields.io/badge/iOS-17%2B-blue)
![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen)
![Swift](https://img.shields.io/badge/Swift-5.9-orange)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple)
![License](https://img.shields.io/badge/License-MIT-green)

## How It Works

A show control system (like **QLab**) sends an OSC command over the local network → the iPhone receives it and triggers a native phone call or SMS notification — indistinguishable from the real thing.

```
QLab / OSC Sender  ──UDP──▶  iPhone / Android (TheaterPhone App)
                                 ├── /call    → Native incoming call
                                 ├── /sms     → Native notification + chat view
                                 ├── /hangup  → End call remotely
                                 ├── /vibrate → Vibration pulse or pattern
                                 ├── /audio   → Play imported sound file
                                 └── /ping    → Responds with /pong (alive check)
```

## Features

- **Native incoming calls** — CallKit on iOS, full-screen notification on Android — works on lock screen
- **SMS notifications** — native notifications on both platforms, tap to open iMessage-style chat
- **Audio playback** — import sound files and trigger them by name
- **Two communication modes** — OSC (binary, UDP) or Plain Text (UDP + TCP) — switchable in Settings
- **Background mode** — listener stays active when the phone is locked
- **Ping/Pong** — QLab can check if the app is running before sending commands
- **Vibration control** — trigger vibration remotely
- **Cross-platform** — identical command set for iOS and Android, use the same QLab cues for both

## Commands

### OSC Mode (default)

| Command | Arguments | Description |
|---------|-----------|-------------|
| `/call` | `<Name> [Number]` | Trigger incoming call |
| `/hangup` | — | End current call remotely |
| `/sms` | `<Sender> <Text>` | Send SMS notification |
| `/vibrate` | `[single\|pattern\|stop]` | Vibration only (default: single) |
| `/audio` | `<Name> \| stop` | Play imported sound file / stop playback |
| `/ping` | — | App responds with `/pong` on port 9001 |

### Plain Text Mode (UDP + TCP)

Switch to Plain Text mode in Settings for simpler integrations that don't support OSC binary protocol.

| Command | Arguments | Description |
|---------|-----------|-------------|
| `call` | `<Name> [Number]` | Trigger incoming call |
| `hangup` | — | End current call remotely |
| `sms` | `<Sender> <Text>` | Send SMS notification |
| `vibrate` | `[single\|pattern\|stop]` | Vibration only (default: single) |
| `audio` | `<Name> \| stop` | Play imported sound file / stop playback |
| `ping` | — | App responds with `pong ready` on port 9001 |

Use quotes for arguments with spaces: `call "Mom" "+1 555 1234567"`

### Examples

```
# OSC mode
/call "Mom" "+1 555 1234567"    → Incoming call from Mom
/sms "Max" "Where are you?"    → SMS notification from Max
/audio doorbell                 → Plays the sound named "doorbell"
/audio stop                     → Stops current audio playback
/hangup                         → Ends the active call

# Plain Text mode (same commands without /)
call "Mom" "+1 555 1234567"
sms "Max" "Where are you?"
audio doorbell
hangup
```

## Requirements

- **iOS:** iPhone with iOS 17+, Mac with Xcode 15+
- **Android:** Phone with Android 8.0+ (API 26), Android Studio
- Both devices on the **same Wi-Fi network**
- OSC-capable show control software (QLab, ETC Eos, etc.), a plain text sender, or the included Python test script

## Installation

### iOS

1. Clone this repository
2. Open `TheaterPhone/TheaterPhone.xcodeproj` in Xcode
3. Select your iPhone as the build target
4. Build and run (⌘R)
5. Allow notifications when prompted
6. Note the IP address shown on the lock screen

> **Tip:** Set your preferred ringtone and message tone in iPhone Settings → Sounds & Haptics before the show.

### Android

1. Clone this repository
2. Open the `TheaterPhoneAndroid/` folder in Android Studio
3. Wait for Gradle sync to complete
4. Connect your Android phone via USB (enable USB debugging)
5. Build and run (▶️)
6. Allow notification permissions when prompted
7. Note the IP address shown on the lock screen

> **Tip:** Disable battery optimization for TheaterPhone in Android Settings → Apps → TheaterPhone → Battery → Unrestricted. This prevents the OS from killing the background listener during a show.

## Audio Library

Import sound files (mp3, wav, aiff, m4a) to play them on cue — great for doorbell rings, ambient sounds, or custom effects.

1. Open **Settings** (gear icon) → **Audio Library**
2. Tap **Add Sound** → select a file from your iPhone
3. Give it a name (e.g. `doorbell`)
4. Trigger it via OSC: `/audio doorbell` or Plain Text: `audio doorbell`
5. Stop playback: `/audio stop`

Sound names are case-insensitive. Files are stored within the app and persist across restarts.

## Testing Without QLab

Use the included Python script to send commands from any computer:

```bash
# Interactive mode (supports OSC, Plain Text UDP, Plain Text TCP)
python3 osc_send.py

# Direct OSC commands
python3 osc_send.py call "Mom" "+1 555 1234567"
python3 osc_send.py sms "Max" "Break a leg!"
python3 osc_send.py hangup
python3 osc_send.py audio doorbell
python3 osc_send.py ping

# Plain Text mode (UDP)
python3 osc_send.py --plain call "Mom"
python3 osc_send.py --plain audio doorbell

# Plain Text mode (TCP)
python3 osc_send.py --plain --tcp sms "Max" "Hello!"
```

Edit `TARGET_IP` in `osc_send.py` to match your iPhone's IP address (shown in the app). In interactive mode, use `ip <address>` to change it on the fly, and `o`/`p`/`t` to switch between OSC, Plain Text UDP, and Plain Text TCP.

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
├── TheaterPhone/                  # iOS app (Xcode / Swift / SwiftUI)
│   └── TheaterPhone/
│       ├── TheaterPhoneApp.swift
│       ├── Models/
│       │   ├── CallState.swift
│       │   └── SMSState.swift
│       ├── Services/
│       │   ├── OSCManager.swift        # OSC + Plain Text listener
│       │   ├── SoundLibraryManager.swift
│       │   ├── CallKitService.swift
│       │   ├── NotificationService.swift
│       │   ├── AudioService.swift
│       │   └── BackgroundService.swift
│       └── Views/
│           ├── ContentView.swift
│           ├── LockScreenView.swift
│           ├── ActiveCallView.swift
│           ├── CallEndedView.swift
│           ├── SMSConversationView.swift
│           └── SettingsView.swift
│
├── TheaterPhoneAndroid/           # Android app (Kotlin / Jetpack Compose)
│   └── app/src/main/java/com/theaterphone/
│       ├── service/
│       │   ├── OscListenerService.kt   # Foreground Service (UDP/TCP)
│       │   ├── OscParser.kt            # OSC binary protocol parser
│       │   ├── PlainTextParser.kt      # Plain text tokenizer
│       │   └── CommandDispatcher.kt    # Routes commands to managers
│       ├── call/
│       │   ├── CallManager.kt          # Call state machine
│       │   ├── CallNotificationHelper.kt # Full-screen call notification
│       │   └── IncomingCallActivity.kt # Lock screen call UI
│       ├── sms/
│       │   ├── SmsManager.kt           # SMS state + messages
│       │   └── SmsNotificationHelper.kt
│       ├── audio/
│       │   ├── AudioService.kt         # Vibration + tones
│       │   └── SoundLibraryManager.kt  # Audio file import + playback
│       └── ui/screen/
│           ├── LockScreen.kt
│           ├── ActiveCallScreen.kt
│           ├── CallEndedScreen.kt
│           ├── SmsConversationScreen.kt
│           └── SettingsScreen.kt
│
├── QLab Script Cues/              # AppleScript fallback cues
├── TheaterPhone.qlabnetwork       # QLab 5 network device preset
├── osc_send.py                    # Python test tool (OSC + Plain Text)
└── osc_ping_test.py               # Ping/Pong debug script
```

## How It Stays Alive on Lock Screen

### iOS
1. **CallKit** — iOS natively handles incoming call UI on the lock screen
2. **Background Audio** — A silent audio loop keeps the app process alive
3. **Local Notifications** — SMS messages appear as native iOS notifications

The app requests `audio` and `voip` background modes in `Info.plist`.

### Android
1. **Foreground Service** — A persistent notification keeps the listener running
2. **Wake Lock** — Prevents the CPU from sleeping while waiting for commands
3. **Full-Screen Intent** — Incoming calls show over the lock screen via high-priority notification

> **Note:** Some manufacturers (Samsung, Xiaomi, Huawei) aggressively kill background services. Disable battery optimization for TheaterPhone before the show.

## Inspiration

*Reading about Stage Caller was the inspiration for tackling this project.*

## License

MIT — see [LICENSE](LICENSE)
