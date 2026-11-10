package com.example.pocketpay.domain.stats

import com.example.pocketpay.data.TransactionEntity
import com.example.pocketpay.data.TxStatus
import com.example.pocketpay.data.TxType
import java.util.Calendar
import java.util.TimeZone

/** One bar of the statistics chart: money in and out during a day or a month. */
data class Bucket(val startMillis: Long, val income: Long, val expense: Long)

enum class StatsPeriod { WEEKLY, MONTHLY }

object Statistics {
    /**
     * Income is every successful CREDIT except refunds (a refund only undoes an expense, and that
     * expense is already excluded because it is REVERSED). Expense is every successful DEBIT.
     * Internal moves between pockets (REALLOCATION) are neither.
     */
    private fun income(tx: TransactionEntity) =
        tx.type == TxType.CREDIT && tx.status == TxStatus.SUCCESS && !tx.id.startsWith("refund_")

    private fun expense(tx: TransactionEntity) = tx.type == TxType.DEBIT && tx.status == TxStatus.SUCCESS

    private fun startOfDay(millis: Long, zone: TimeZone): Calendar =
        Calendar.getInstance(zone).apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

    /** Seven daily buckets ending with the day of [now] (oldest first). */
    fun weekly(txs: List<TransactionEntity>, now: Long, zone: TimeZone = TimeZone.getDefault()): List<Bucket> {
        val today = startOfDay(now, zone)
        val starts = (6 downTo 0).map { back ->
            (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -back) }.timeInMillis
        }
        return buckets(txs, starts, (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis)
    }

    /** Six monthly buckets ending with the month of [now] (oldest first). */
    fun monthly(txs: List<TransactionEntity>, now: Long, zone: TimeZone = TimeZone.getDefault()): List<Bucket> {
        val thisMonth = startOfDay(now, zone).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val starts = (5 downTo 0).map { back ->
            (thisMonth.clone() as Calendar).apply { add(Calendar.MONTH, -back) }.timeInMillis
        }
        return buckets(txs, starts, (thisMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }.timeInMillis)
    }

    private fun buckets(txs: List<TransactionEntity>, starts: List<Long>, end: Long): List<Bucket> =
        starts.mapIndexed { i, start ->
            val to = starts.getOrNull(i + 1) ?: end
            val inRange = txs.filter { it.timestamp in start until to }
            Bucket(start, inRange.filter(::income).sumOf { it.amount }, inRange.filter(::expense).sumOf { it.amount })
        }

    fun totalIncome(buckets: List<Bucket>) = buckets.sumOf { it.income }
    fun totalExpense(buckets: List<Bucket>) = buckets.sumOf { it.expense }

    /** The most recent successful expenses, newest first. */
    fun recentExpenses(txs: List<TransactionEntity>, limit: Int = 5): List<TransactionEntity> =
        txs.filter(::expense).sortedByDescending { it.timestamp }.take(limit)
}
