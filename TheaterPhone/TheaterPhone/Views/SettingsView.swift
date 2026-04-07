// TheaterPhone v1.4.0
import SwiftUI
import UniformTypeIdentifiers

struct SettingsView: View {
    @EnvironmentObject var oscManager: OSCManager
    @EnvironmentObject var soundLibrary: SoundLibraryManager
    @Environment(\.dismiss) var dismiss
    @State private var portText: String = ""
    @State private var showFilePicker = false
    @State private var showNameAlert = false
    @State private var pendingFileURL: URL?
    @State private var soundName: String = ""

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Connection")) {
                    Picker("Mode", selection: $oscManager.mode) {
                        ForEach(CommunicationMode.allCases, id: \.self) { mode in
                            Text(mode.rawValue).tag(mode)
                        }
                    }
                    .onChange(of: oscManager.mode) { _, _ in
                        oscManager.startListening()
                    }
                    HStack {
                        Text("Protocol"); Spacer()
                        Text(oscManager.mode == .osc ? "UDP" : "UDP + TCP").foregroundColor(.secondary)
                    }
                    HStack {
                        Text("Status"); Spacer()
                        HStack(spacing: 6) {
                            Circle().fill(oscManager.isListening ? Color.green : Color.red).frame(width: 8, height: 8)
                            Text(oscManager.isListening ? "Active" : "Inactive").foregroundColor(.secondary)
                        }
                    }
                    HStack {
                        Text("IP Address"); Spacer()
                        Text(oscManager.localIP).foregroundColor(.secondary)
                    }
                    HStack {
                        Text("Port"); Spacer()
                        TextField("9000", text: $portText)
                            .keyboardType(.numberPad).multilineTextAlignment(.trailing).frame(width: 80)
                    }
                    Button("Restart Connection") {
                        if let p = UInt16(portText) { oscManager.restartWithPort(p) }
                        else { oscManager.startListening() }
                    }
                }

                Section(header: Text("Commands")) {
                    if oscManager.mode == .osc {
                        VStack(alignment: .leading, spacing: 8) {
                            CommandRow(command: "/call", args: "<Name> [Number]", desc: "Incoming call")
                            CommandRow(command: "/hangup", args: "", desc: "End call")
                            CommandRow(command: "/sms", args: "<Sender> <Text>", desc: "Receive SMS")
                            CommandRow(command: "/vibrate", args: "[single|pattern|stop]", desc: "Vibration only")
                            CommandRow(command: "/audio", args: "<Name> | stop", desc: "Play/stop audio file")
                            CommandRow(command: "/ping", args: "", desc: "App responds with /pong")
                        }
                        .font(.system(size: 13, design: .monospaced))
                    } else {
                        VStack(alignment: .leading, spacing: 8) {
                            CommandRow(command: "call", args: "<Name> [Number]", desc: "Incoming call")
                            CommandRow(command: "hangup", args: "", desc: "End call")
                            CommandRow(command: "sms", args: "<Sender> <Text>", desc: "Receive SMS")
                            CommandRow(command: "vibrate", args: "[single|pattern|stop]", desc: "Vibration only")
                            CommandRow(command: "audio", args: "<Name> | stop", desc: "Play/stop audio file")
                            CommandRow(command: "ping", args: "", desc: "App responds with pong")
                        }
                        .font(.system(size: 13, design: .monospaced))
                        Text("Use quotes for arguments with spaces:\ncall \"Mom\" \"+1 555 1234567\"")
                            .font(.system(size: 12, design: .monospaced))
                            .foregroundColor(.secondary)
                    }
                }

                Section(header: Text("Audio Library"),
                        footer: Text("Import audio files and trigger them via \(oscManager.mode == .osc ? "/audio <name>" : "audio <name>"). Names are case-insensitive.")) {
                    if soundLibrary.sounds.isEmpty {
                        Text("No sounds loaded").foregroundColor(.secondary)
                    } else {
                        ForEach(soundLibrary.sounds) { sound in
                            HStack {
                                VStack(alignment: .leading) {
                                    Text(sound.name).bold()
                                    Text(sound.fileName.components(separatedBy: ".").last?.uppercased() ?? "")
                                        .font(.caption).foregroundColor(.secondary)
                                }
                                Spacer()
                                if soundLibrary.nowPlaying == sound.name {
                                    Image(systemName: "speaker.wave.2.fill")
                                        .foregroundColor(.green).font(.caption)
                                }
                            }
                        }
                        .onDelete { indexSet in
                            for index in indexSet {
                                soundLibrary.deleteSound(id: soundLibrary.sounds[index].id)
                            }
                        }
                    }
                    Button("Add Sound") { showFilePicker = true }
                }

                Section(header: Text("Test")) {
                    Button("Test Call") {
                        dismiss()
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                            oscManager.callManager?.incomingCall(name: "Mom", number: "+1 555 1234567")
                        }
                    }
                    Button("Test SMS") {
                        dismiss()
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                            oscManager.smsManager?.receiveMessage(from: "Max", text: "Where are you? The show is about to start!")
                        }
                    }
                    if !soundLibrary.sounds.isEmpty {
                        Button("Test Audio (\(soundLibrary.sounds.first!.name))") {
                            soundLibrary.play(name: soundLibrary.sounds.first!.name)
                        }
                        if soundLibrary.nowPlaying != nil {
                            Button("Stop Audio") { soundLibrary.stop() }
                        }
                    }
                }

                Section(header: Text("Info"),
                        footer: Text("Ringtone and message tone are controlled via iPhone Settings → Sounds & Haptics.")) {
                    HStack {
                        Text("Version"); Spacer()
                        Text("1.4.0").foregroundColor(.secondary)
                    }
                    HStack {
                        Text("Last Command"); Spacer()
                        Text(oscManager.lastReceivedMessage.isEmpty ? "–" : oscManager.lastReceivedMessage)
                            .foregroundColor(.secondary).lineLimit(1)
                    }
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) { Button("Done") { dismiss() } }
            }
            .onAppear { portText = String(oscManager.port) }
            .sheet(isPresented: $showFilePicker) {
                AudioFilePicker { url in
                    pendingFileURL = url
                    soundName = url.deletingPathExtension().lastPathComponent
                    showNameAlert = true
                }
            }
            .alert("Sound Name", isPresented: $showNameAlert) {
                TextField("Name", text: $soundName)
                Button("Add") {
                    guard let url = pendingFileURL, !soundName.isEmpty else { return }
                    soundLibrary.importFile(from: url, name: soundName)
                    pendingFileURL = nil
                    soundName = ""
                }
                Button("Cancel", role: .cancel) {
                    pendingFileURL = nil
                    soundName = ""
                }
            } message: {
                Text("Enter a name for this sound. Use this name in OSC commands to play it.")
            }
        }
    }
}

// MARK: - File Picker

struct AudioFilePicker: UIViewControllerRepresentable {
    let onPick: (URL) -> Void

    func makeUIViewController(context: Context) -> UIDocumentPickerViewController {
        let types: [UTType] = [.audio, .mp3, .wav, .aiff, UTType("public.mpeg-4-audio") ?? .audio]
        let picker = UIDocumentPickerViewController(forOpeningContentTypes: types, asCopy: true)
        picker.delegate = context.coordinator
        picker.allowsMultipleSelection = false
        return picker
    }

    func updateUIViewController(_ uiViewController: UIDocumentPickerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(onPick: onPick) }

    class Coordinator: NSObject, UIDocumentPickerDelegate {
        let onPick: (URL) -> Void
        init(onPick: @escaping (URL) -> Void) { self.onPick = onPick }
        func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            if let url = urls.first { onPick(url) }
        }
    }
}

// MARK: - Command Row

struct CommandRow: View {
    let command: String; let args: String; let desc: String
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack(spacing: 4) {
                Text(command).foregroundColor(.blue).bold()
                Text(args).foregroundColor(.secondary)
            }
            Text(desc).font(.system(size: 12)).foregroundColor(.secondary)
        }
    }
}
