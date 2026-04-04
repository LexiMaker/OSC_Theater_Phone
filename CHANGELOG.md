# Changelog

## v1.4.0-experimental — 2026-04-04

- Added communication mode switch: OSC (binary) or Plain Text
- Plain Text mode accepts readable commands like `call "Mom" "+1 555"`
- Plain Text mode supports both UDP and TCP on the same port
- TCP listener starts/stops automatically when switching to/from Plain Text mode
- Tokenizer supports quoted arguments with spaces
- Pong response adapts to current mode (OSC binary or plain text)
- Settings: mode picker, protocol display (UDP / UDP+TCP), conditional command docs
- Python test script (`osc_send.py`) updated with `--plain` and `--tcp` flags
- Interactive test mode supports mode switching (o/p/t)

## v1.3.3 — 2026-03-31

- Restored in-app active call screen (iOS returns to app after accepting CallKit calls)
- Restored iMessage-style chat view when tapping SMS notifications
- Calls: native CallKit UI for ringing → app call screen after accept
- SMS: native iOS notification → tap opens fake iMessage chat

## v1.3.0 — 2026-03-31

- Removed in-app sound selection (ringtone/SMS tone now controlled via iPhone Settings)
- Removed Ring Mode picker (Sound+Vibration / Sound Only / Vibration Only)
- Removed Heartbeat feature (replaced by Ping/Pong)
- Simplified AudioService to vibration-only
- Cleaned up Settings view
- Updated QLab network preset with `/vibrate` and `/ping` commands

## v1.2.0 — 2026-03-31

- Fixed: CallKit now uses silent ringtone so app controls sound/vibration
- Fixed: Pong/Heartbeat UDP messages sent before connection ready (caused QLab fallback to always trigger)
- Fixed: Audio session conflicts between BackgroundService, AudioService, and CallKitService
- Fixed: `didSet` triggering during `init` causing premature heartbeat start
- Fixed: `scheduleReset` race condition when new call arrives during reset delay
- Fixed: Force-unwrap on `NWEndpoint.Port` removed
- Fixed: Deprecated `onChange` syntax updated for iOS 17
- Translated Info.plist network permission string to English
- Notification categories registered once at startup instead of per SMS
- Deployment target raised to iOS 17.0

## v1.1.0 — 2026-03-30

- All UI translated to English
- Added version numbering system
- Added Ring Mode: Sound+Vibration, Sound Only, Vibration Only
- Added `/vibrate` OSC command with `single`, `pattern`, `stop` modes
- Added vibration test buttons in Settings

## v1.0.0 — 2026-03-29

- Initial release
- OSC listener on configurable UDP port (default 9000)
- `/call`, `/hangup`, `/sms` commands
- `/ping` → `/pong` app-alive check for QLab fallback
- CallKit integration for native incoming call UI on lock screen
- Background audio mode to keep OSC listener alive when locked
- Local notifications for SMS on lock screen
- iMessage-style conversation view
- System ringtone and SMS tone selection
- QLab 5 network device preset
- QLab AppleScript fallback cues with Ping/Pong
- Python test script for sending OSC commands
