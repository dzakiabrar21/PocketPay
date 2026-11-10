package com.example.pocketpay

import com.example.pocketpay.domain.qris.Qris
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrisTest {

    @Test
    fun crc16_matchesStandardCheckValue() {
        // CRC-16/CCITT-FALSE check value for "123456789" is 0x29B1.
        assertEquals("29B1", Qris.crc16("123456789"))
    }

    @Test
    fun buildThenParse_dynamicQr_roundTrips() {
        val raw = Qris.build("Kantin Kampus", "BANDUNG", "9012839102", amount = 23_500L)
        val parsed = Qris.parse(raw)
        assertNotNull(parsed)
        assertEquals("Kantin Kampus", parsed!!.merchantName)
        assertEquals("BANDUNG", parsed.merchantCity)
        assertEquals(23_500L, parsed.amount)
        assertTrue(parsed.isDynamic)
    }

    @Test
    fun buildWithoutAmount_isStaticAndHasNoAmount() {
        val parsed = Qris.parse(Qris.build("Warung", "JAKARTA", "123"))!!
        assertNull(parsed.amount)
        assertFalse(parsed.isDynamic)
    }

    @Test
    fun tamperedPayload_failsCrc() {
        val raw = Qris.build("Kantin", "JAKARTA", "123", amount = 10_000L)
        val tampered = raw.replace("10000", "99999")
        assertNull(Qris.parse(tampered))
    }

    @Test
    fun wrongCrc_isRejected() {
        val raw = Qris.build("Kantin", "JAKARTA", "123", amount = 10_000L)
        assertNull(Qris.parse(raw.dropLast(4) + "0000"))
    }

    @Test
    fun nonQrisContent_isRejected() {
        assertNull(Qris.parse("https://example.com"))
        assertNull(Qris.parse("Kantin|50000")) // legacy ad-hoc format is no longer accepted
        assertNull(Qris.parse(""))
        assertNull(Qris.parse("000201"))
    }

    @Test
    fun nonIdrCurrency_isRejected() {
        // Hand-build a payload with currency 840 (USD) and a valid CRC.
        val body = "000201" + "010211" + "5303840" + "5802ID" + "5906Kantin" + "6007JAKARTA" + "6304"
        assertNull(Qris.parse(body + Qris.crc16(body)))
    }

    @Test
    fun merchantNameAndCity_areTruncatedToSpecLimits() {
        val raw = Qris.build("N".repeat(40), "C".repeat(30), "123")
        val parsed = Qris.parse(raw)!!
        assertEquals(25, parsed.merchantName.length)
        assertEquals(15, parsed.merchantCity.length)
    }

    @Test
    fun decimalAmount_isParsedToWholeRupiah() {
        val body = "000201" + "010212" + "5303360" + "540823500.00" + "5802ID" + "5906Kantin" + "6007JAKARTA" + "6304"
        assertEquals(23_500L, Qris.parse(body + Qris.crc16(body))!!.amount)
    }
}
