package com.koolicream.zelu

/**
 * The shape of a bank message with no payee name: the sending bank and the message's words with every number taken
 * out. Two cash withdrawals at the KPHB machine share a shape ("rs withdrawn from hdfc bank card at kphb cd on avl
 * bal"); one at Hafeezpet does not. What you decide for one payment of a shape is used for the others.
 */
object MessageShape {
    const val PREFIX = "~shape:"
    private val TAIL = Regex("""(?i)\b(not\s+you|if\s+not\s+(?:done|you)|call\s+\d|sms\s+block|to\s+block)\b.*$""", RegexOption.DOT_MATCHES_ALL)
    private val PIECE = Regex("""[A-Za-z0-9]+""")

    fun key(raw: String, source: String): String? {
        val body = TAIL.replace(raw, "")
        val words = PIECE.findAll(body).map { it.value.lowercase() }
            .filter { w -> w.none { it.isDigit() } && w.length >= 2 }
            .take(24).toList()
        if (words.size < 4) return null
        val bank = source.uppercase().split('-').filter { it.length >= 4 }.maxByOrNull { it.length } ?: source.uppercase()
        return PREFIX + bank + "|" + words.joinToString(" ")
    }
}
