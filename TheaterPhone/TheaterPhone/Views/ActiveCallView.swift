// TheaterPhone v1.3.3
import SwiftUI

struct ActiveCallView: View {
    @EnvironmentObject var callManager: CallManager
    @State private var elapsed: TimeInterval = 0
    let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var callerName: String {
        if case .active(let name, _, _) = callManager.phase { return name }
        return ""
    }
    var startTime: Date? {
        if case .active(_, _, let time) = callManager.phase { return time }
        return nil
    }

    var body: some View {
        ZStack {
            Color.black.opacity(0.95).ignoresSafeArea()
            VStack(spacing: 0) {
                Spacer().frame(height: 80)
                Text(callerName)
                    .font(.system(size: 30, weight: .regular)).foregroundColor(.white)
                Text(durationString)
                    .font(.system(size: 16, design: .monospaced)).foregroundColor(.white.opacity(0.6)).padding(.top, 6)
                Spacer()
                LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 3), spacing: 24) {
                    CallControlButton(icon: "speaker.wave.2.fill", label: "Speaker")
                    CallControlButton(icon: "mic.slash.fill", label: "Mute")
                    CallControlButton(icon: "person.crop.circle.badge.plus", label: "Add")
                    CallControlButton(icon: "pause.fill", label: "Hold")
                    CallControlButton(icon: "video.fill", label: "FaceTime")
                    CallControlButton(icon: "circle.grid.3x3.fill", label: "Keypad")
                }
                .padding(.horizontal, 30)
                Spacer()
                Button(action: { callManager.endCall() }) {
                    ZStack {
                        Circle().fill(Color.red).frame(width: 70, height: 70)
                        Image(systemName: "phone.down.fill").font(.system(size: 28)).foregroundColor(.white)
                    }
                }
                .padding(.bottom, 50)
            }
        }
        .onReceive(timer) { _ in
            if let start = startTime { elapsed = Date().timeIntervalSince(start) }
        }
    }

    private var durationString: String {
        String(format: "%02d:%02d", Int(elapsed) / 60, Int(elapsed) % 60)
    }
}

struct CallControlButton: View {
    let icon: String; let label: String
    @State private var isActive = false
    var body: some View {
        Button(action: { isActive.toggle() }) {
            VStack(spacing: 6) {
                ZStack {
                    Circle().fill(isActive ? Color.white : Color.white.opacity(0.15)).frame(width: 60, height: 60)
                    Image(systemName: icon).font(.system(size: 22)).foregroundColor(isActive ? .black : .white)
                }
                Text(label).font(.system(size: 12)).foregroundColor(.white.opacity(0.6))
            }
        }
    }
}
