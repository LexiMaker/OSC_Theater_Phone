// TheaterPhone v1.3.0
import AVFoundation
import UIKit

class BackgroundService {
    static let shared = BackgroundService()
    private var silentPlayer: AVAudioPlayer?
    private var backgroundTask: UIBackgroundTaskIdentifier = .invalid
    private init() {}

    func startBackgroundMode() {
        playSilentAudio()
        NotificationCenter.default.addObserver(self, selector: #selector(appDidEnterBackground),
            name: UIApplication.didEnterBackgroundNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(appWillEnterForeground),
            name: UIApplication.willEnterForegroundNotification, object: nil)
    }

    @objc private func appDidEnterBackground() {
        playSilentAudio()
        backgroundTask = UIApplication.shared.beginBackgroundTask { [weak self] in self?.endBackgroundTask() }
    }

    @objc private func appWillEnterForeground() { endBackgroundTask() }

    private func endBackgroundTask() {
        if backgroundTask != .invalid {
            UIApplication.shared.endBackgroundTask(backgroundTask)
            backgroundTask = .invalid
        }
    }

    private func playSilentAudio() {
        guard silentPlayer == nil || silentPlayer?.isPlaying == false else { return }
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: [.mixWithOthers])
            try AVAudioSession.sharedInstance().setActive(true)
            let data = generateSilentWAV()
            silentPlayer = try AVAudioPlayer(data: data)
            silentPlayer?.numberOfLoops = -1
            silentPlayer?.volume = 0.0
            silentPlayer?.play()
        } catch { print("Background audio error: \(error)") }
    }

    private func generateSilentWAV() -> Data {
        let sampleRate: Int = 44100
        let samples = sampleRate // 1 second
        var data = Data()
        let dataSize = UInt32(samples * 2)
        // RIFF header
        data.append(contentsOf: "RIFF".utf8)
        var fileSize = UInt32(36 + dataSize); data.append(Data(bytes: &fileSize, count: 4))
        data.append(contentsOf: "WAVE".utf8)
        data.append(contentsOf: "fmt ".utf8)
        var fmtSize: UInt32 = 16; data.append(Data(bytes: &fmtSize, count: 4))
        var fmt: UInt16 = 1; data.append(Data(bytes: &fmt, count: 2))
        var ch: UInt16 = 1; data.append(Data(bytes: &ch, count: 2))
        var sr = UInt32(sampleRate); data.append(Data(bytes: &sr, count: 4))
        var br = UInt32(sampleRate * 2); data.append(Data(bytes: &br, count: 4))
        var ba: UInt16 = 2; data.append(Data(bytes: &ba, count: 2))
        var bps: UInt16 = 16; data.append(Data(bytes: &bps, count: 2))
        data.append(contentsOf: "data".utf8)
        var ds = dataSize; data.append(Data(bytes: &ds, count: 4))
        data.append(Data(count: Int(dataSize)))
        return data
    }
}
