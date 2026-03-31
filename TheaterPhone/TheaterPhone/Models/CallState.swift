// TheaterPhone v1.3.0
import Foundation
import UIKit

enum CallPhase: Equatable {
    case inactive
    case ringing(callerName: String, callerNumber: String)
    case active(callerName: String, callerNumber: String, startTime: Date)
    case ended
}

class CallManager: ObservableObject {
    @Published var phase: CallPhase = .inactive

    private let callKit = CallKitService.shared

    init() {
        callKit.onCallAccepted = { [weak self] in self?.callKitDidAccept() }
        callKit.onCallDeclined = { [weak self] in self?.callKitDidDecline() }
    }

    func incomingCall(name: String, number: String) {
        DispatchQueue.main.async {
            self.phase = .ringing(callerName: name, callerNumber: number)
            self.callKit.reportIncomingCall(name: name, number: number)
        }
    }

    func acceptCall() {
        if case .ringing(let name, let number) = phase {
            callKit.reportCallConnected()
            phase = .active(callerName: name, callerNumber: number, startTime: Date())
        }
    }

    func declineCall() {
        callKit.endCurrentCall()
        phase = .ended
        scheduleReset()
    }

    func endCall() {
        callKit.endCurrentCall()
        phase = .ended
        scheduleReset()
    }

    func hangUp() {
        AudioService.shared.playEndCallTone()
        callKit.reportCallEnded(reason: .remoteEnded)
        phase = .ended
        scheduleReset()
    }

    // MARK: - CallKit Callbacks

    private func callKitDidAccept() {
        DispatchQueue.main.async {
            if case .ringing(let name, let number) = self.phase {
                self.phase = .active(callerName: name, callerNumber: number, startTime: Date())
            }
        }
    }

    private func callKitDidDecline() {
        DispatchQueue.main.async {
            self.phase = .ended
            self.scheduleReset()
        }
    }

    private func scheduleReset() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            if case .ended = self.phase { self.phase = .inactive }
        }
    }
}
