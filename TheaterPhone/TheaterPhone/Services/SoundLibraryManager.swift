// TheaterPhone v1.4.0
import AVFoundation
import Foundation

struct SoundItem: Identifiable, Codable {
    let id: UUID
    var name: String
    let fileName: String
}

class SoundLibraryManager: ObservableObject {
    @Published var sounds: [SoundItem] = []
    @Published var nowPlaying: String? = nil

    private var player: AVAudioPlayer?
    private let storageFile = "sound_library.json"

    init() {
        loadSounds()
    }

    // MARK: - Documents Directory

    private var documentsDir: URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
    }

    private var audioDir: URL {
        let dir = documentsDir.appendingPathComponent("AudioLibrary")
        if !FileManager.default.fileExists(atPath: dir.path) {
            try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        }
        return dir
    }

    // MARK: - Import

    func importFile(from sourceURL: URL, name: String) {
        let accessing = sourceURL.startAccessingSecurityScopedResource()
        defer { if accessing { sourceURL.stopAccessingSecurityScopedResource() } }

        let ext = sourceURL.pathExtension
        let id = UUID()
        let fileName = "\(id.uuidString).\(ext)"
        let destURL = audioDir.appendingPathComponent(fileName)

        do {
            try FileManager.default.copyItem(at: sourceURL, to: destURL)
            let item = SoundItem(id: id, name: name, fileName: fileName)
            DispatchQueue.main.async {
                self.sounds.append(item)
                self.saveSounds()
            }
        } catch {
            print("SoundLibrary: import failed — \(error.localizedDescription)")
        }
    }

    // MARK: - Delete

    func deleteSound(id: UUID) {
        guard let index = sounds.firstIndex(where: { $0.id == id }) else { return }
        let item = sounds[index]
        let fileURL = audioDir.appendingPathComponent(item.fileName)
        try? FileManager.default.removeItem(at: fileURL)
        sounds.remove(at: index)
        saveSounds()
    }

    // MARK: - Playback

    func play(name: String) {
        guard let item = sounds.first(where: { $0.name.lowercased() == name.lowercased() }) else {
            print("SoundLibrary: no sound named '\(name)'")
            return
        }
        let fileURL = audioDir.appendingPathComponent(item.fileName)
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, options: .mixWithOthers)
            try AVAudioSession.sharedInstance().setActive(true)
            player = try AVAudioPlayer(contentsOf: fileURL)
            player?.delegate = nil
            player?.play()
            DispatchQueue.main.async { self.nowPlaying = item.name }
            // Clear nowPlaying when done
            let duration = player?.duration ?? 0
            DispatchQueue.main.asyncAfter(deadline: .now() + duration + 0.1) { [weak self] in
                if self?.nowPlaying == item.name { self?.nowPlaying = nil }
            }
        } catch {
            print("SoundLibrary: playback failed — \(error.localizedDescription)")
        }
    }

    func stop() {
        player?.stop()
        player = nil
        DispatchQueue.main.async { self.nowPlaying = nil }
    }

    // MARK: - Persistence

    private func saveSounds() {
        let url = documentsDir.appendingPathComponent(storageFile)
        if let data = try? JSONEncoder().encode(sounds) {
            try? data.write(to: url)
        }
    }

    private func loadSounds() {
        let url = documentsDir.appendingPathComponent(storageFile)
        guard let data = try? Data(contentsOf: url),
              let items = try? JSONDecoder().decode([SoundItem].self, from: data) else { return }
        sounds = items
    }
}
