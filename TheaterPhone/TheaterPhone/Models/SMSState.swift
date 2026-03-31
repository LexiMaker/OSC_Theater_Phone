// TheaterPhone v1.3.2
import Foundation

struct SMSMessage: Identifiable, Equatable {
    let id = UUID()
    let sender: String
    let text: String
    let timestamp: Date
    let isFromMe: Bool
}

class SMSManager: ObservableObject {
    @Published var messages: [SMSMessage] = []
    @Published var isShowingConversation: Bool = false

    func receiveMessage(from sender: String, text: String) {
        DispatchQueue.main.async {
            let msg = SMSMessage(sender: sender, text: text, timestamp: Date(), isFromMe: false)
            self.messages.append(msg)
            // iOS handles notification sound + banner
            NotificationService.shared.showSMSNotification(from: sender, text: text)
        }
    }

    func openConversation() {
        isShowingConversation = true
    }

    func closeConversation() {
        isShowingConversation = false
    }

    func clearMessages() {
        messages.removeAll()
    }
}
