// TheaterPhone v1.3.0
import CallKit
import AVFoundation

class CallKitService: NSObject, ObservableObject {
    static let shared = CallKitService()

    private let provider: CXProvider
    private let callController = CXCallController()
    private var currentCallUUID: UUID?

    var onCallAccepted: (() -> Void)?
    var onCallDeclined: (() -> Void)?

    override init() {
        let config = CXProviderConfiguration()
        config.supportsVideo = false
        config.maximumCallsPerCallGroup = 1
        config.maximumCallGroups = 1
        config.supportedHandleTypes = [.phoneNumber, .generic]
        // nil = system default ringtone (set via iPhone Settings → Sounds & Haptics)
        config.ringtoneSound = nil
        provider = CXProvider(configuration: config)
        super.init()
        provider.setDelegate(self, queue: nil)
    }

    func reportIncomingCall(name: String, number: String, completion: ((Bool) -> Void)? = nil) {
        let uuid = UUID()
        currentCallUUID = uuid
        let update = CXCallUpdate()
        update.localizedCallerName = name
        update.remoteHandle = number.isEmpty
            ? CXHandle(type: .generic, value: name)
            : CXHandle(type: .phoneNumber, value: number)
        update.hasVideo = false
        update.supportsDTMF = true
        update.supportsHolding = true
        provider.reportNewIncomingCall(with: uuid, update: update) { error in
            completion?(error == nil)
        }
    }

    func endCurrentCall() {
        guard let uuid = currentCallUUID else { return }
        let action = CXEndCallAction(call: uuid)
        callController.request(CXTransaction(action: action)) { error in
            if error != nil { self.provider.reportCall(with: uuid, endedAt: Date(), reason: .remoteEnded) }
        }
        currentCallUUID = nil
    }

    func reportCallConnected() {
        guard let uuid = currentCallUUID else { return }
        provider.reportOutgoingCall(with: uuid, connectedAt: Date())
    }

    func reportCallEnded(reason: CXCallEndedReason = .remoteEnded) {
        guard let uuid = currentCallUUID else { return }
        provider.reportCall(with: uuid, endedAt: Date(), reason: reason)
        currentCallUUID = nil
    }
}

extension CallKitService: CXProviderDelegate {
    func providerDidReset(_ provider: CXProvider) { currentCallUUID = nil }

    func provider(_ provider: CXProvider, perform action: CXAnswerCallAction) {
        onCallAccepted?()
        action.fulfill()
    }

    func provider(_ provider: CXProvider, perform action: CXEndCallAction) {
        if currentCallUUID != nil { onCallDeclined?() }
        currentCallUUID = nil
        action.fulfill()
    }

    func provider(_ provider: CXProvider, didActivate audioSession: AVAudioSession) {}
    func provider(_ provider: CXProvider, didDeactivate audioSession: AVAudioSession) {}
}
