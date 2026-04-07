// TheaterPhone v1.4.0
import SwiftUI

@main
struct TheaterPhoneApp: App {
    @StateObject private var oscManager = OSCManager()
    @StateObject private var callManager = CallManager()
    @StateObject private var smsManager = SMSManager()
    @StateObject private var soundLibrary = SoundLibraryManager()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(oscManager)
                .environmentObject(callManager)
                .environmentObject(smsManager)
                .environmentObject(soundLibrary)
                .onAppear {
                    oscManager.callManager = callManager
                    oscManager.smsManager = smsManager
                    oscManager.soundLibrary = soundLibrary
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
