// TheaterPhone v1.3.0
import SwiftUI

struct LockScreenView: View {
    @EnvironmentObject var oscManager: OSCManager
    @State private var currentTime = Date()
    let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        ZStack {
            LinearGradient(
                gradient: Gradient(colors: [
                    Color(red: 0.05, green: 0.05, blue: 0.15),
                    Color(red: 0.1, green: 0.05, blue: 0.2)
                ]),
                startPoint: .top, endPoint: .bottom
            ).ignoresSafeArea()

            VStack(spacing: 8) {
                Spacer().frame(height: 60)
                Text(timeString)
                    .font(.system(size: 72, weight: .thin))
                    .foregroundColor(.white)
                Text(dateString)
                    .font(.system(size: 18, weight: .regular))
                    .foregroundColor(.white.opacity(0.7))
                Spacer()
                VStack(spacing: 8) {
                    Circle()
                        .fill(oscManager.isListening ? Color.green : Color.red)
                        .frame(width: 8, height: 8)
                    Text(oscManager.isListening ? "OSC Ready" : "Not Connected")
                        .font(.system(size: 12))
                        .foregroundColor(.white.opacity(0.4))
                    if oscManager.isListening {
                        Text("\(oscManager.localIP):\(oscManager.port)")
                            .font(.system(size: 11, design: .monospaced))
                            .foregroundColor(.white.opacity(0.3))
                    }
                    if !oscManager.lastReceivedMessage.isEmpty {
                        Text(oscManager.lastReceivedMessage)
                            .font(.system(size: 10, design: .monospaced))
                            .foregroundColor(.white.opacity(0.2))
                    }
                }
                .padding(.bottom, 50)
            }
        }
        .onReceive(timer) { _ in currentTime = Date() }
    }

    private var timeString: String {
        let f = DateFormatter(); f.dateFormat = "HH:mm"; return f.string(from: currentTime)
    }
    private var dateString: String {
        let f = DateFormatter(); f.locale = Locale(identifier: "en_US")
        f.dateFormat = "EEEE, MMMM d"; return f.string(from: currentTime)
    }
}
