// TheaterPhone v1.3.3
import SwiftUI

struct CallEndedView: View {
    var body: some View {
        ZStack {
            Color.black.opacity(0.95).ignoresSafeArea()
            VStack {
                Spacer()
                Text("Call Ended")
                    .font(.system(size: 22, weight: .regular))
                    .foregroundColor(.white.opacity(0.8))
                Spacer()
            }
        }
    }
}
