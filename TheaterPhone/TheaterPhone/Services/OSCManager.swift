// TheaterPhone v1.3.0
import Foundation
import Network

class OSCManager: ObservableObject {
    @Published var isListening = false
    @Published var port: UInt16 = 9000
    @Published var lastReceivedMessage: String = ""
    @Published var localIP: String = "..."

    var callManager: CallManager?
    var smsManager: SMSManager?

    private var listener: NWListener?

    func startListening() {
        stopListening()
        guard let nwPort = NWEndpoint.Port(rawValue: port) else {
            print("OSC Listener error: invalid port \(port)")
            return
        }
        do {
            let params = NWParameters.udp
            listener = try NWListener(using: params, on: nwPort)
            listener?.stateUpdateHandler = { [weak self] state in
                DispatchQueue.main.async {
                    switch state {
                    case .ready:
                        self?.isListening = true
                        self?.updateLocalIP()
                    case .failed, .cancelled:
                        self?.isListening = false
                    default: break
                    }
                }
            }
            listener?.newConnectionHandler = { [weak self] connection in
                connection.start(queue: .main)
                self?.receiveData(on: connection)
            }
            listener?.start(queue: .main)
        } catch {
            print("OSC Listener error: \(error)")
        }
    }

    func stopListening() {
        listener?.cancel()
        listener = nil
        isListening = false
    }

    func restartWithPort(_ newPort: UInt16) {
        port = newPort
        startListening()
    }

    private func oscStringData(_ string: String) -> Data {
        var data = string.data(using: .utf8)!
        data.append(0)
        while data.count % 4 != 0 { data.append(0) }
        return data
    }

    // MARK: - Receive

    private func receiveData(on connection: NWConnection) {
        connection.receiveMessage { [weak self] data, _, _, error in
            if let data = data {
                let remoteEndpoint = connection.currentPath?.remoteEndpoint
                self?.parseOSC(data: data, remoteEndpoint: remoteEndpoint)
            }
            if error == nil { self?.receiveData(on: connection) }
        }
    }

    // MARK: - Ping/Pong

    private let pongReplyPort: UInt16 = 9001

    private func sendPong(to host: String) {
        guard let nwPort = NWEndpoint.Port(rawValue: pongReplyPort) else { return }
        let connection = NWConnection(
            host: NWEndpoint.Host(host),
            port: nwPort,
            using: .udp
        )
        connection.stateUpdateHandler = { [weak self] state in
            guard let self = self else { return }
            if state == .ready {
                var msg = Data()
                msg.append(self.oscStringData("/theaterphone/pong"))
                msg.append(self.oscStringData(",s"))
                msg.append(self.oscStringData("ready"))
                connection.send(content: msg, completion: .contentProcessed { _ in connection.cancel() })
            } else if case .failed = state {
                connection.cancel()
            }
        }
        connection.start(queue: .global(qos: .utility))
    }

    private func hostString(from endpoint: NWEndpoint?) -> String? {
        guard let endpoint = endpoint else { return nil }
        switch endpoint {
        case .hostPort(let host, _):
            var hostStr = "\(host)"
            if hostStr.hasPrefix("::ffff:") { hostStr = String(hostStr.dropFirst(7)) }
            return hostStr
        default: return nil
        }
    }

    // MARK: - OSC Parsing

    private func parseOSC(data: Data, remoteEndpoint: NWEndpoint?) {
        guard let address = readOSCString(data: data, offset: 0) else { return }
        let addressEndOffset = oscStringLength(address)
        let path = address.lowercased()

        DispatchQueue.main.async {
            self.lastReceivedMessage = "OSC: \(address)"
        }

        if path == "/ping" {
            if let senderIP = hostString(from: remoteEndpoint) {
                sendPong(to: senderIP)
            }
        }
        else if path == "/vibrate" {
            let args = parseOSCArguments(data: data, offset: addressEndOffset)
            let mode = args.first?.lowercased() ?? "single"
            DispatchQueue.main.async {
                if mode == "pattern" || mode == "repeat" {
                    AudioService.shared.startVibrationPattern()
                } else if mode == "stop" {
                    AudioService.shared.stopVibrationPattern()
                } else {
                    AudioService.shared.vibrate()
                }
            }
        }
        else if path == "/call" {
            let args = parseOSCArguments(data: data, offset: addressEndOffset)
            let name = args.first ?? "Unknown"
            let number = args.count > 1 ? args[1] : ""
            DispatchQueue.main.async {
                self.callManager?.incomingCall(name: name, number: number)
            }
        }
        else if path == "/hangup" || path == "/endcall" {
            DispatchQueue.main.async {
                self.callManager?.hangUp()
            }
        }
        else if path == "/sms" {
            let args = parseOSCArguments(data: data, offset: addressEndOffset)
            let sender = args.first ?? "Unknown"
            let text = args.count > 1 ? args.dropFirst().joined(separator: " ") : ""
            DispatchQueue.main.async {
                self.smsManager?.receiveMessage(from: sender, text: text)
            }
        }
    }

    private func readOSCString(data: Data, offset: Int) -> String? {
        var end = offset
        while end < data.count && data[end] != 0 { end += 1 }
        guard end > offset else { return nil }
        return String(data: data[offset..<end], encoding: .utf8)
    }

    private func oscStringLength(_ string: String) -> Int {
        let len = string.utf8.count + 1
        return len + (4 - len % 4) % 4
    }

    private func parseOSCArguments(data: Data, offset: Int) -> [String] {
        var args: [String] = []
        var pos = offset
        guard pos < data.count, data[pos] == 0x2C else { return args }
        guard let typeTag = readOSCString(data: data, offset: pos) else { return args }
        pos += oscStringLength(typeTag)
        let types = Array(typeTag.dropFirst())
        for type in types {
            switch type {
            case "s":
                if let str = readOSCString(data: data, offset: pos) {
                    args.append(str)
                    pos += oscStringLength(str)
                }
            case "i":
                if pos + 4 <= data.count {
                    let value = data[pos..<pos+4].withUnsafeBytes { $0.load(as: Int32.self).bigEndian }
                    args.append(String(value))
                    pos += 4
                }
            case "f":
                if pos + 4 <= data.count {
                    let bits = data[pos..<pos+4].withUnsafeBytes { $0.load(as: UInt32.self).bigEndian }
                    args.append(String(Float(bitPattern: bits)))
                    pos += 4
                }
            default: break
            }
        }
        return args
    }

    private func updateLocalIP() {
        var address: String = "Not available"
        var ifaddr: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&ifaddr) == 0, let firstAddr = ifaddr else { return }
        for ptr in sequence(first: firstAddr, next: { $0.pointee.ifa_next }) {
            let interface = ptr.pointee
            let addrFamily = interface.ifa_addr.pointee.sa_family
            if addrFamily == UInt8(AF_INET) {
                let name = String(cString: interface.ifa_name)
                if name == "en0" {
                    var hostname = [CChar](repeating: 0, count: Int(NI_MAXHOST))
                    getnameinfo(interface.ifa_addr, socklen_t(interface.ifa_addr.pointee.sa_len),
                               &hostname, socklen_t(hostname.count), nil, socklen_t(0), NI_NUMERICHOST)
                    address = String(cString: hostname)
                }
            }
        }
        freeifaddrs(ifaddr)
        DispatchQueue.main.async { self.localIP = address }
    }
}
