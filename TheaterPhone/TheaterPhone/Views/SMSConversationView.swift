// TheaterPhone v1.3.2
import SwiftUI

struct SMSConversationView: View {
    @EnvironmentObject var smsManager: SMSManager

    var senderName: String {
        smsManager.messages.last(where: { !$0.isFromMe })?.sender ?? "Message"
    }

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            VStack(spacing: 0) {
                HStack {
                    Button(action: { smsManager.closeConversation() }) {
                        HStack(spacing: 4) {
                            Image(systemName: "chevron.left").font(.system(size: 18, weight: .semibold))
                            Text("Back").font(.system(size: 17))
                        }.foregroundColor(.blue)
                    }
                    Spacer()
                    VStack(spacing: 2) {
                        ZStack {
                            Circle().fill(Color.gray.opacity(0.4)).frame(width: 32, height: 32)
                            Image(systemName: "person.fill").font(.system(size: 14)).foregroundColor(.white.opacity(0.7))
                        }
                        Text(senderName).font(.system(size: 13, weight: .semibold)).foregroundColor(.white)
                    }
                    Spacer()
                    HStack(spacing: 4) {
                        Image(systemName: "video.fill")
                        Image(systemName: "phone.fill")
                    }.font(.system(size: 16)).foregroundColor(.blue)
                }
                .padding(.horizontal, 16).padding(.vertical, 10)
                .background(Color(white: 0.1))

                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(spacing: 4) {
                            ForEach(smsManager.messages) { msg in
                                MessageBubble(message: msg).id(msg.id)
                            }
                        }.padding(.horizontal, 12).padding(.vertical, 8)
                    }
                    .onChange(of: smsManager.messages.count) { _, _ in
                        if let last = smsManager.messages.last {
                            withAnimation { proxy.scrollTo(last.id, anchor: .bottom) }
                        }
                    }
                }

                HStack(spacing: 10) {
                    Image(systemName: "plus").font(.system(size: 22)).foregroundColor(.gray)
                    RoundedRectangle(cornerRadius: 18)
                        .stroke(Color.gray.opacity(0.4), lineWidth: 1)
                        .frame(height: 36)
                        .overlay(
                            Text("iMessage").font(.system(size: 15)).foregroundColor(.gray.opacity(0.5)).padding(.leading, 12),
                            alignment: .leading
                        )
                }
                .padding(.horizontal, 12).padding(.vertical, 8)
                .background(Color(white: 0.1))
            }
        }
    }
}

struct MessageBubble: View {
    let message: SMSMessage
    var body: some View {
        HStack {
            if message.isFromMe { Spacer() }
            Text(message.text).font(.system(size: 16)).foregroundColor(.white)
                .padding(.horizontal, 14).padding(.vertical, 8)
                .background(message.isFromMe ? Color.blue : Color(white: 0.25))
                .clipShape(RoundedRectangle(cornerRadius: 18))
            if !message.isFromMe { Spacer() }
        }
    }
}
