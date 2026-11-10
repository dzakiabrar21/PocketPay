package com.example.pocketpay.domain.notify

import com.example.pocketpay.data.PocketEntity
import com.example.pocketpay.data.PocketType

/** Events worth telling the customer about, detected from the current pockets. */
sealed interface Milestone {
    /** Stable key so each milestone is announced once. */
    val key: String
    val pocket: PocketEntity

    data class DepositMatured(override val pocket: PocketEntity) : Milestone {
        override val key get() = "matured:${pocket.id}"
    }

    data class GoalReached(override val pocket: PocketEntity) : Milestone {
        override val key get() = "goal:${pocket.id}"
    }
}

object Milestones {
    /**
     * Returns milestones not yet in [alreadyNotified]. A deposit counts once its maturity date has
     * passed; a goal counts once the balance reaches the target.
     */
    fun detect(pockets: List<PocketEntity>, now: Long, alreadyNotified: Set<String>): List<Milestone> =
        pockets.flatMap { p ->
            buildList {
                if (p.pocketType == PocketType.LOCKED_DEPOSIT && p.maturityDate != null && now >= p.maturityDate) {
                    add(Milestone.DepositMatured(p))
                }
                if (p.targetAmount != null && p.targetAmount > 0 && p.balance >= p.targetAmount) {
                    add(Milestone.GoalReached(p))
                }
            }
        }.filter { it.key !in alreadyNotified }
}
