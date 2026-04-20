package com.theaterphone.service

import com.theaterphone.data.model.OscCommand
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Parses OSC (Open Sound Control) binary protocol messages. */
object OscParser {

    fun parse(data: ByteArray): OscCommand? {
        val address = readOscString(data, 0) ?: return null
        val addressEndOffset = oscStringLength(address)
        val path = address.lowercase()

        return when (path) {
            "/ping" -> OscCommand.Ping
            "/vibrate" -> {
                val args = parseArguments(data, addressEndOffset)
                OscCommand.Vibrate(args.firstOrNull()?.lowercase() ?: "single")
            }
            "/call" -> {
                val args = parseArguments(data, addressEndOffset)
                OscCommand.Call(
                    name = args.firstOrNull() ?: "Unknown",
                    number = args.getOrNull(1) ?: ""
                )
            }
            "/hangup", "/endcall" -> OscCommand.Hangup
            "/sms" -> {
                val args = parseArguments(data, addressEndOffset)
                OscCommand.Sms(
                    sender = args.firstOrNull() ?: "Unknown",
                    text = if (args.size > 1) args.drop(1).joinToString(" ") else ""
                )
            }
            "/audio" -> {
                val args = parseArguments(data, addressEndOffset)
                val name = args.firstOrNull()?.lowercase() ?: ""
                if (name == "stop") OscCommand.AudioStop
                else if (name.isNotEmpty()) OscCommand.Audio(name)
                else null
            }
            else -> null
        }
    }

    private fun readOscString(data: ByteArray, offset: Int): String? {
        var end = offset
        while (end < data.size && data[end] != 0.toByte()) end++
        if (end <= offset) return null
        return String(data, offset, end - offset, Charsets.UTF_8)
    }

    private fun oscStringLength(string: String): Int {
        val len = string.toByteArray(Charsets.UTF_8).size + 1
        return len + (4 - len % 4) % 4
    }

    private fun parseArguments(data: ByteArray, offset: Int): List<String> {
        val args = mutableListOf<String>()
        var pos = offset
        if (pos >= data.size || data[pos] != 0x2C.toByte()) return args // must start with ','

        val typeTag = readOscString(data, pos) ?: return args
        pos += oscStringLength(typeTag)
        val types = typeTag.drop(1) // remove leading ','

        for (type in types) {
            when (type) {
                's' -> {
                    val str = readOscString(data, pos)
                    if (str != null) {
                        args.add(str)
                        pos += oscStringLength(str)
                    }
                }
                'i' -> {
                    if (pos + 4 <= data.size) {
                        val value = ByteBuffer.wrap(data, pos, 4)
                            .order(ByteOrder.BIG_ENDIAN).int
                        args.add(value.toString())
                        pos += 4
                    }
                }
                'f' -> {
                    if (pos + 4 <= data.size) {
                        val value = ByteBuffer.wrap(data, pos, 4)
                            .order(ByteOrder.BIG_ENDIAN).float
                        args.add(value.toString())
                        pos += 4
                    }
                }
            }
        }
        return args
    }

    /** Build an OSC message (for pong reply). */
    fun buildMessage(address: String, vararg args: String): ByteArray {
        val buffer = mutableListOf<Byte>()
        buffer.addAll(oscStringBytes(address))
        val typeTag = "," + "s".repeat(args.size)
        buffer.addAll(oscStringBytes(typeTag))
        for (arg in args) {
            buffer.addAll(oscStringBytes(arg))
        }
        return buffer.toByteArray()
    }

    private fun oscStringBytes(string: String): List<Byte> {
        val bytes = string.toByteArray(Charsets.UTF_8).toMutableList()
        bytes.add(0) // null terminator
        while (bytes.size % 4 != 0) bytes.add(0) // pad to 4-byte boundary
        return bytes
    }
}
