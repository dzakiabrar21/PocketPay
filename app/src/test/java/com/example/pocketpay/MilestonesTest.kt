package com.example.pocketpay

import com.example.pocketpay.data.PocketEntity
import com.example.pocketpay.data.PocketType
import com.example.pocketpay.domain.notify.Milestone
import com.example.pocketpay.domain.notify.Milestones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MilestonesTest {
    private val now = 1_000_000L
    private fun spending(balance: Long, target: Long?) =
        PocketEntity("s", "Liburan", balance, PocketType.SPENDING, 3.5, targetAmount = target)
    private fun deposit(maturity: Long) =
        PocketEntity("d", "Deposito", 1_000, PocketType.LOCKED_DEPOSIT, 6.0, 3, maturity)

    @Test fun goalReached_whenBalanceMeetsTarget() {
        val m = Milestones.detect(listOf(spending(500, 500)), now, emptySet())
        assertEquals(listOf("goal:s"), m.map { it.key })
    }

    @Test fun goalNotReached_belowTargetOrWithoutTarget() {
        assertTrue(Milestones.detect(listOf(spending(499, 500), spending(10_000, null)), now, emptySet()).isEmpty())
    }

    @Test fun depositMatured_onlyOnceDatePassed() {
        assertTrue(Milestones.detect(listOf(deposit(now + 1)), now, emptySet()).isEmpty())
        val m = Milestones.detect(listOf(deposit(now)), now, emptySet())
        assertTrue(m.single() is Milestone.DepositMatured)
    }

    @Test fun alreadyNotifiedMilestonesAreSkipped() {
        val pockets = listOf(spending(600, 500), deposit(now - 5))
        assertEquals(2, Milestones.detect(pockets, now, emptySet()).size)
        assertEquals(1, Milestones.detect(pockets, now, setOf("goal:s")).size)
        assertTrue(Milestones.detect(pockets, now, setOf("goal:s", "matured:d")).isEmpty())
    }
}
