package com.example.pocketpay

import com.example.pocketpay.data.TransactionEntity
import com.example.pocketpay.data.TxStatus
import com.example.pocketpay.data.TxType
import com.example.pocketpay.domain.stats.Statistics
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StatisticsTest {
    private val zone = TimeZone.getTimeZone("UTC")
    private val day = 24L * 3600 * 1000
    // Wednesday 2026-11-11 12:00 UTC
    private val now = Calendar.getInstance(zone).apply { clear(); set(2026, Calendar.NOVEMBER, 11, 12, 0) }.timeInMillis

    private fun tx(id: String, type: TxType, amount: Long, at: Long, status: TxStatus = TxStatus.SUCCESS) =
        TransactionEntity(id = id, title = id, amount = amount, sourceOfFund = "x", type = type, status = status, timestamp = at)

    @Test fun weekly_hasSevenDays_oldestFirst_endingToday() {
        val b = Statistics.weekly(emptyList(), now, zone)
        assertEquals(7, b.size)
        assertEquals(true, b.zipWithNext().all { (a, c) -> a.startMillis < c.startMillis })
        assertEquals(now - 12 * 3600 * 1000, b.last().startMillis) // midnight today
    }

    @Test fun weekly_sumsIncomeAndExpensePerDay() {
        val txs = listOf(
            tx("a", TxType.DEBIT, 10_000, now),
            tx("b", TxType.DEBIT, 5_000, now - 1000),
            tx("c", TxType.CREDIT, 50_000, now - day),
            tx("d", TxType.DEBIT, 7_000, now - 3 * day)
        )
        val b = Statistics.weekly(txs, now, zone)
        assertEquals(15_000L, b[6].expense)
        assertEquals(50_000L, b[5].income)
        assertEquals(7_000L, b[3].expense)
        assertEquals(22_000L, Statistics.totalExpense(b))
        assertEquals(50_000L, Statistics.totalIncome(b))
    }

    @Test fun reversedPayments_andRefunds_andInternalMoves_areExcluded() {
        val txs = listOf(
            tx("p", TxType.DEBIT, 20_000, now, TxStatus.REVERSED),
            tx("refund_p", TxType.CREDIT, 20_000, now),
            tx("move", TxType.REALLOCATION, 99_000, now)
        )
        val b = Statistics.weekly(txs, now, zone)
        assertEquals(0L, Statistics.totalExpense(b))
        assertEquals(0L, Statistics.totalIncome(b))
    }

    @Test fun outsideTheWindow_isIgnored() {
        val b = Statistics.weekly(listOf(tx("old", TxType.DEBIT, 1_000, now - 10 * day)), now, zone)
        assertEquals(0L, Statistics.totalExpense(b))
    }

    @Test fun monthly_hasSixMonths_andBucketsByCalendarMonth() {
        val oct = Calendar.getInstance(zone).apply { clear(); set(2026, Calendar.OCTOBER, 15) }.timeInMillis
        val b = Statistics.monthly(listOf(tx("o", TxType.DEBIT, 3_000, oct), tx("n", TxType.DEBIT, 4_000, now)), now, zone)
        assertEquals(6, b.size)
        assertEquals(3_000L, b[4].expense)
        assertEquals(4_000L, b[5].expense)
    }

    @Test fun recentExpenses_newestFirst_limited() {
        val txs = (1..8).map { tx("t$it", TxType.DEBIT, it * 1000L, now - it * 1000L) }
        val r = Statistics.recentExpenses(txs, 3)
        assertEquals(listOf("t1", "t2", "t3"), r.map { it.id })
    }
}
