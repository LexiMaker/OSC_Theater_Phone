// TheaterPhone v1.3.4
import AudioToolbox
import AVFoundation
import Foundation

/// Handles vibration and call-end tone — all other sounds managed by iOS
class AudioService {
    static let shared = AudioService()
    private var vibrationTimer: Timer?
    private var endCallPlayer: AVAudioPlayer?
    private init() {}

    // MARK: - Call End Tone

    /// Play the short "call ended" beep
    func playEndCallTone() {
        // System sound 1112 = phone call disconnect tone
        AudioServicesPlaySystemSound(1112)
    }

    // MARK: - Vibration

    /// Single vibration pulse
    func vibrate() {
        AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
    }

    /// Repeating vibration pattern (like a ringtone but vibration only)
    func startVibrationPattern() {
        stopVibrationPattern()
        vibrate()
        vibrationTimer = Timer.scheduledTimer(withTimeInterval: 1.5, repeats: true) { [weak self] _ in
            self?.vibrate()
        }
    }

    func stopVibrationPattern() {
        vibrationTimer?.invalidate()
        vibrationTimer = nil
    }
}
