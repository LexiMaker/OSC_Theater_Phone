// TheaterPhone v1.3.3
import SwiftUI

struct ContentView: View {
    @EnvironmentObject var oscManager: OSCManager
    @EnvironmentObject var callManager: CallManager
    @EnvironmentObject var smsManager: SMSManager
    @State private var showSettings = false

    var body: some View {
        ZStack {
            LockScreenView().environmentObject(oscManager)

            // Active call / call ended overlay (after accepting via CallKit)
            switch callManager.phase {
            case .active:
                ActiveCallView().environmentObject(callManager).transition(.opacity)
            case .ended:
                CallEndedView().transition(.opacity)
            default:
                EmptyView()
            }

            // SMS conversation (opened via notification tap)
            if smsManager.isShowingConversation {
                SMSConversationView()
                    .environmentObject(smsManager)
                    .transition(.move(edge: .bottom))
            }

            VStack {
                HStack {
                    Spacer()
                    Button(action: { showSettings = true }) {
                        Image(systemName: "gear").font(.system(size: 16))
                            .foregroundColor(.white.opacity(0.3)).padding(12)
                    }
                }
                Spacer()
            }
        }
        .animation(.easeInOut(duration: 0.3), value: callManager.phase)
        .animation(.easeInOut(duration: 0.3), value: smsManager.isShowingConversation)
        .sheet(isPresented: $showSettings) {
            SettingsView()
                .environmentObject(oscManager)
        }
        .preferredColorScheme(.dark)
    }
}
