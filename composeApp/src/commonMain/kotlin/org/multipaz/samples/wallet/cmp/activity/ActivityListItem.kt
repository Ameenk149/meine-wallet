package org.multipaz.samples.wallet.cmp.activity

import org.multipaz.eventlogger.Event

sealed interface ActivityListItem {
    val sortKey: Long

    data class PresentationItem(val event: Event) : ActivityListItem {
        override val sortKey: Long
            get() {
                val t = event.timestamp
                return t.epochSeconds * 1000L + t.nanosecondsOfSecond / 1_000_000
            }
    }

    data class IssuanceItem(val record: IssuanceActivityRecord) : ActivityListItem {
        override val sortKey: Long get() = record.startedAtEpochMs
    }
}
