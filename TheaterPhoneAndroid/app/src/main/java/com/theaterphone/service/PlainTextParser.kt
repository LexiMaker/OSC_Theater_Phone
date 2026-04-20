package com.theaterphone.service

import com.theaterphone.data.model.OscCommand

/** Parses plain text commands like: call "Mom" "+1 555 1234567" */
object PlainTextParser {

    fun parse(line: String): OscCommand? {
        val tokens = tokenize(line.trim())
        if (tokens.isEmpty()) return null
        val command = tokens[0].lowercase()
        val args = tokens.drop(1)

        return when (command) {
            "ping" -> OscCommand.Ping
            "vibrate" -> OscCommand.Vibrate(args.firstOrNull()?.lowercase() ?: "single")
            "call" -> OscCommand.Call(
                name = args.firstOrNull() ?: "Unknown",
                number = args.getOrNull(1) ?: ""
            )
            "hangup", "endcall" -> OscCommand.Hangup
            "sms" -> OscCommand.Sms(
                sender = args.firstOrNull() ?: "Unknown",
                text = if (args.size > 1) args.drop(1).joinToString(" ") else ""
            )
            "audio" -> {
                val name = args.firstOrNull()?.lowercase() ?: ""
                if (name == "stop") OscCommand.AudioStop
                else if (name.isNotEmpty()) OscCommand.Audio(name)
                else null
            }
            else -> null
        }
    }

    /**
     * Tokenize a string, respecting quoted substrings.
     * e.g. `call "Mom" "+1 555 123"` → ["call", "Mom", "+1 555 123"]
     */
    private fun tokenize(input: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var quoteChar = '"'

        for (char in input) {
            when {
                inQuotes -> {
                    if (char == quoteChar) inQuotes = false
                    else current.append(char)
                }
                char == '"' || char == '\'' -> {
                    inQuotes = true
                    quoteChar = char
                }
                char == ' ' -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current.clear()
                    }
                }
                else -> current.append(char)
            }
        }
        if (current.isNotEmpty()) tokens.add(current.toString())
        return tokens
    }
}
