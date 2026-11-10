package com.example.pocketpay.domain.qris

/** Decoded content of an EMVCo / QRIS merchant-presented QR code. */
data class QrisPayload(
    val merchantName: String,
    val merchantCity: String,
    /** Fixed amount in Rupiah, or null for a static QR where the payer types the amount. */
    val amount: Long?,
    val isDynamic: Boolean
)

/** Minimal EMVCo TLV parser/builder for QRIS (tag 2 chars + length 2 chars + value). */
object Qris {

    /** CRC-16/CCITT-FALSE (poly 0x1021, init 0xFFFF), as required by EMVCo tag 63. */
    fun crc16(input: String): String {
        var crc = 0xFFFF
        for (b in input.toByteArray(Charsets.US_ASCII)) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1
                crc = crc and 0xFFFF
            }
        }
        return "%04X".format(crc)
    }

    private fun tlv(tag: String, value: String): String = tag + "%02d".format(value.length) + value

    private fun parseTlv(data: String): Map<String, String>? {
        val out = LinkedHashMap<String, String>()
        var i = 0
        while (i < data.length) {
            if (i + 4 > data.length) return null
            val tag = data.substring(i, i + 2)
            val len = data.substring(i + 2, i + 4).toIntOrNull() ?: return null
            if (i + 4 + len > data.length) return null
            out[tag] = data.substring(i + 4, i + 4 + len)
            i += 4 + len
        }
        return out
    }

    /**
     * Parses a QRIS string. Returns null if it is not a structurally valid QRIS code:
     * wrong format indicator, malformed TLV, bad CRC, or missing merchant name.
     */
    fun parse(raw: String): QrisPayload? {
        val text = raw.trim()
        if (text.length < 20 || !text.startsWith("000201")) return null

        // The CRC covers everything up to and including the "6304" header of the CRC field.
        val crcHeaderAt = text.length - 8
        if (crcHeaderAt < 0 || text.substring(crcHeaderAt, crcHeaderAt + 4) != "6304") return null
        val expectedCrc = crc16(text.substring(0, crcHeaderAt + 4))
        if (!expectedCrc.equals(text.substring(crcHeaderAt + 4), ignoreCase = true)) return null

        val fields = parseTlv(text) ?: return null
        val name = fields["59"]?.takeIf { it.isNotBlank() } ?: return null
        if (fields["53"] != null && fields["53"] != "360") return null // IDR only

        val amount = fields["54"]?.let { a ->
            // QRIS amounts are decimal strings, e.g. "23500" or "23500.00"
            a.substringBefore('.').toLongOrNull()?.takeIf { it > 0 } ?: return null
        }
        return QrisPayload(
            merchantName = name.trim(),
            merchantCity = fields["60"].orEmpty(),
            amount = amount,
            isDynamic = fields["01"] == "12"
        )
    }

    /**
     * Builds a QRIS-formatted string for receiving money into [accountNumber].
     * NOTE: this is structurally valid EMVCo/QRIS, but PocketPay is not a registered QRIS
     * issuer, so third-party e-wallets will not settle against it.
     */
    fun build(
        merchantName: String,
        city: String,
        accountNumber: String,
        amount: Long? = null
    ): String {
        val account = tlv("00", "ID.POCKETPAY.WWW") + tlv("01", accountNumber)
        val body = buildString {
            append(tlv("00", "01"))
            append(tlv("01", if (amount != null && amount > 0) "12" else "11"))
            append(tlv("26", account))
            append(tlv("52", "0000"))
            append(tlv("53", "360"))
            if (amount != null && amount > 0) append(tlv("54", amount.toString()))
            append(tlv("58", "ID"))
            append(tlv("59", merchantName.take(25)))
            append(tlv("60", city.take(15)))
            append("6304")
        }
        return body + crc16(body)
    }
}
