package org.multipaz.samples.wallet.cmp

import org.multipaz.mdoc.zkp.ZkSystemRepository
import org.multipaz.mdoc.zkp.longfellow.LongfellowZkSystem
import org.multipaz.samples.wallet.cmp.benchmark.InstrumentedZkSystem
import org.multipaz.util.Logger

private const val TAG = "ZkSystemRepositoryFactory"

actual fun createZkSystemRepository(): ZkSystemRepository? {
    return try {
        // Multipaz 0.99.0+ bundles the recommended Longfellow circuits (v6 and v7)
        // inside multipaz-longfellow, so no asset files are needed.
        val longfellow = LongfellowZkSystem().apply { addDefaultCircuits() }
        if (longfellow.systemSpecs.isEmpty()) {
            Logger.w(TAG, "No Longfellow circuits loaded; ZKP presentment disabled")
            null
        } else {
            Logger.i(TAG, "Loaded ${longfellow.systemSpecs.size} Longfellow circuits")
            ZkSystemRepository().add(InstrumentedZkSystem(longfellow))
        }
    } catch (e: Throwable) {
        Logger.w(TAG, "Failed to initialize Longfellow ZK", e)
        null
    }
}
