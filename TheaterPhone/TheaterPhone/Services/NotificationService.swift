// TheaterPhone v1.3.0
import UserNotifications

class NotificationService: NSObject, UNUserNotificationCenterDelegate {
    static let shared = NotificationService()

    /// Called when a notification is tapped — set by SMSManager
    var onNotificationTapped: (() -> Void)?

    private override init() {
        super.init()
    }

    func setup() {
        UNUserNotificationCenter.current().delegate = self
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
        registerCategories()
    }

    private func registerCategories() {
        let action = UNNotificationAction(identifier: "OPEN_SMS", title: "Open", options: [.foreground])
        let category = UNNotificationCategory(identifier: "SMS_MESSAGE", actions: [action], intentIdentifiers: [], options: [.customDismissAction])
        UNUserNotificationCenter.current().setNotificationCategories([category])
    }

    func showSMSNotification(from sender: String, text: String) {
        let content = UNMutableNotificationContent()
        content.title = sender
        content.body = text
        content.sound = .default
        content.categoryIdentifier = "SMS_MESSAGE"

        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request)
    }

    // MARK: - Show notifications even when app is in foreground
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        // Show banner + play sound even when app is active
        completionHandler([.banner, .sound])
    }

    // MARK: - Handle notification tap
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        if response.notification.request.content.categoryIdentifier == "SMS_MESSAGE" {
            DispatchQueue.main.async { self.onNotificationTapped?() }
        }
        completionHandler()
    }
}
