// TheaterPhone v1.3.3
import SwiftUI

@main
struct TheaterPhoneApp: App {
    @StateObject private var oscManager = OSCManager()
    @StateObject private var callManager = CallManager()
    @StateObject private var smsManager = SMSManager()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(oscManager)
                .environmentObject(callManager)
                .environmentObject(smsManager)
                .onAppear {
                    oscManager.callManager = callManager
                    oscManager.smsManager = smsManager
                    oscManager.startListening()
                    UIApplication.shared.isIdleTimerDisabled = true
                    NotificationService.shared.setup()
                    NotificationService.shared.onNotificationTapped = { [weak smsManager] in
                        smsManager?.openConversation()
                    }
                    BackgroundService.shared.startBackgroundMode()
                }
        }
    }
}
