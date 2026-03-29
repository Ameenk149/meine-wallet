package org.multipaz.samples.wallet.cmp

import kotlinx.io.bytestring.ByteString
import org.multipaz.context.applicationContext
import org.multipaz.mdoc.zkp.ZkSystemRepository
import org.multipaz.mdoc.zkp.longfellow.LongfellowZkSystem
import org.multipaz.util.Logger

private const val TAG = "ZkSystemRepositoryFactory"

/**
 * Circuit filenames from OpenWallet multipaz testapp (longfellow-libzk-v1).
 * Shipped under android assets: [ASSET_DIR]/filename
 */
private const val ASSET_DIR = "longfellow-libzk-v1"

private val CIRCUIT_FILENAMES = listOf(
    "6_1_4096_2945_137e5a75ce72735a37c8a72da1a8a0a5df8d13365c2ae3d2c2bd6a0e7197c7c6",
    "6_2_4025_2945_b4bb6f01b7043f4f51d8302a30b36e3d4d2d0efc3c24557ab9212ad524a9764e",
    "6_3_4121_2945_b2211223b954b34a1081e3fbf71b8ea2de28efc888b4be510f532d6ba76c2010",
    "6_4_4283_2945_c70b5f44a1365c53847eb8948ad5b4fdc224251a2bc02d958c84c862823c49d6",
)

actual fun createZkSystemRepository(): ZkSystemRepository? {
    return try {
        val longfellow = LongfellowZkSystem()
        val assets = applicationContext.assets
        for (name in CIRCUIT_FILENAMES) {
            assets.open("$ASSET_DIR/$name").use { input ->
                val bytes = input.readBytes()
                val added = longfellow.addCircuit(name, ByteString(bytes))
                if (!added) {
                    Logger.w(TAG, "Longfellow rejected circuit file: $name")
                }
            }
        }
        if (longfellow.systemSpecs.isEmpty()) {
            Logger.w(TAG, "No Longfellow circuits loaded; ZKP presentment disabled")
            null
        } else {
            ZkSystemRepository().add(longfellow)
        }
    } catch (e: Throwable) {
        Logger.w(TAG, "Failed to initialize Longfellow ZK", e)
        null
    }
}
