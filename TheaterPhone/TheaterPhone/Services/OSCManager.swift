// TheaterPhone v1.4.0
import Foundation
import Network

enum CommunicationMode: String, CaseIterable {
    case osc = "OSC"
    case plainText = "Plain Text"
}

class OSCManager: ObservableObject {
    @Published var isListening = false
    @Published var port: UInt16 = 9000
    @Published var lastReceivedMessage: String = ""
    @Published var localIP: String = "..."
    @Published var mode: CommunicationMode {
        didSet { UserDefaults.standard.set(mode.rawValue, forKey: "communicationMode") }
    }

    var callManager: CallManager?
    var smsManager: SMSManager?

    private var udpListener: NWListener?
    private var tcpListener: NWListener?

    init() {
        if let saved = UserDefaults.standard.string(forKey: "communicationMode"),
           let m = CommunicationMode(rawValue: saved) {
            mode = m
        } else {
            mode = .osc
        }
    }

    func startListening() {
        stopListening()
        guard let nwPort = NWEndpoint.Port(rawValue: port) else {
            print("Listener error: invalid port \(port)")
            return
        }

        // Always start UDP listener
        startUDPListener(on: nwPort)

        // In plain text mode, also start TCP listener
        if mode == .plainText {
            startTCPListener(on: nwPort)
        }
    }

    private func startUDPListener(on nwPort: NWEndpoint.Port) {
        do {
            udpListener = try NWListener(using: .udp, on: nwPort)
            udpListener?.stateUpdateHandler = { [weak self] state in
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
            udpListener?.newConnectionHandler = { [weak self] connection in
                connection.start(queue: .main)
                self?.receiveUDPData(on: connection)
            }
            udpListener?.start(queue: .main)
        } catch {
            print("UDP Listener error: \(error)")
        }
    }

    private func startTCPListener(on nwPort: NWEndpoint.Port) {
        do {
            tcpListener = try NWListener(using: .tcp, on: nwPort)
            tcpListener?.stateUpdateHandler = { state in
                if case .failed(let error) = state {
                    print("TCP Listener failed: \(error)")
                }
            }
            tcpListener?.newConnectionHandler = { [weak self] connection in
                connection.start(queue: .main)
                self?.receiveTCPData(on: connection, buffer: Data())
            }
            tcpListener?.start(queue: .main)
        } catch {
            print("TCP Listener error: \(error)")
        }
    }

    func stopListening() {
        udpListener?.cancel()
        udpListener = nil
        tcpListener?.cancel()
        tcpListener = nil
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

    // MARK: - Receive UDP

    private func receiveUDPData(on connection: NWConnection) {
        connection.receiveMessage { [weak self] data, _, _, error in
            guard let self = self else { return }
            if let data = data {
                let remoteEndpoint = connection.currentPath?.remoteEndpoint
                if self.mode == .osc {
                    self.parseOSC(data: data, remoteEndpoint: remoteEndpoint)
                } else {
                    if let text = String(data: data, encoding: .utf8) {
                        for line in text.components(separatedBy: "\n") {
                            let trimmed = line.trimmingCharacters(in: .whitespacesAndNewlines)
                            if !trimmed.isEmpty {
                                self.parsePlainText(line: trimmed, remoteEndpoint: remoteEndpoint)
                            }
                        }
                    }
                }
            }
            if error == nil { self.receiveUDPData(on: connection) }
        }
    }

    // MARK: - Receive TCP

    private func receiveTCPData(on connection: NWConnection, buffer: Data) {
        connection.receive(minimumIncompleteLength: 1, maximumLength: 4096) { [weak self] data, _, isComplete, error in
            guard let self = self else { return }
            var accumulated = buffer
            if let data = data {
                accumulated.append(data)
                // Process complete lines
                while let newlineRange = accumulated.range(of: Data([0x0A])) {
                    let lineData = accumulated[accumulated.startIndex..<newlineRange.lowerBound]
                    accumulated = Data(accumulated[newlineRange.upperBound...])
                    if let line = String(data: lineData, encoding: .utf8)?
                        .trimmingCharacters(in: .whitespacesAndNewlines), !line.isEmpty {
                        let remoteEndpoint = connection.currentPath?.remoteEndpoint
                        self.parsePlainText(line: line, remoteEndpoint: remoteEndpoint)
                    }
                }
            }
            if isComplete || error != nil {
                // Process any remaining data without trailing newline
                if !accumulated.isEmpty,
                   let line = String(data: accumulated, encoding: .utf8)?
                    .trimmingCharacters(in: .whitespacesAndNewlines), !line.isEmpty {
                    let remoteEndpoint = connection.currentPath?.remoteEndpoint
                    self.parsePlainText(line: line, remoteEndpoint: remoteEndpoint)
                }
                connection.cancel()
            } else {
                self.receiveTCPData(on: connection, buffer: accumulated)
            }
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
                if self.mode == .osc {
                    var msg = Data()
                    msg.append(self.oscStringData("/theaterphone/pong"))
                    msg.append(self.oscStringData(",s"))
                    msg.append(self.oscStringData("ready"))
                    connection.send(content: msg, completion: .contentProcessed { _ in connection.cancel() })
                } else {
                    let msg = "pong ready\n".data(using: .utf8)!
                    connection.send(content: msg, completion: .contentProcessed { _ in connection.cancel() })
                }
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

    // MARK: - Plain Text Parsing

    private func parsePlainText(line: String, remoteEndpoint: NWEndpoint?) {
        let tokens = tokenize(line)
        guard let command = tokens.first?.lowercased() else { return }
        let args = Array(tokens.dropFirst())

        DispatchQueue.main.async {
            self.lastReceivedMessage = "Text: \(line)"
        }

        switch command {
        case "ping":
            if let senderIP = hostString(from: remoteEndpoint) {
                sendPong(to: senderIP)
            }
        case "vibrate":
            let vibMode = args.first?.lowercased() ?? "single"
            DispatchQueue.main.async {
                if vibMode == "pattern" || vibMode == "repeat" {
                    AudioService.shared.startVibrationPattern()
                } else if vibMode == "stop" {
                    AudioService.shared.stopVibrationPattern()
                } else {
                    AudioService.shared.vibrate()
                }
            }
        case "call":
            let name = args.first ?? "Unknown"
            let number = args.count > 1 ? args[1] : ""
            DispatchQueue.main.async {
                self.callManager?.incomingCall(name: name, number: number)
            }
        case "hangup", "endcall":
            DispatchQueue.main.async {
                self.callManager?.hangUp()
            }
        case "sms":
            let sender = args.first ?? "Unknown"
            let text = args.count > 1 ? args.dropFirst().joined(separator: " ") : ""
            DispatchQueue.main.async {
                self.smsManager?.receiveMessage(from: sender, text: text)
            }
        default:
            print("Unknown plain text command: \(command)")
        }
    }

    /// Tokenize a string, respecting quoted substrings.
    /// e.g. `call "Mom" "+1 555 123"` → ["call", "Mom", "+1 555 123"]
    private func tokenize(_ input: String) -> [String] {
        var tokens: [String] = []
        var current = ""
        var inQuotes = false
        var quoteChar: Character = "\""

        for char in input {
            if inQuotes {
                if char == quoteChar {
                    inQuotes = false
                } else {
                    current.append(char)
                }
            } else if char == "\"" || char == "'" {
                inQuotes = true
                quoteChar = char
            } else if char == " " {
                if !current.isEmpty {
                    tokens.append(current)
                    current = ""
                }
            } else {
                current.append(char)
            }
        }
        if !current.isEmpty { tokens.append(current) }
        return tokens
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
