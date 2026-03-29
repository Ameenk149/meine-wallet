package org.multipaz.samples.wallet.cmp

import org.multipaz.mdoc.zkp.ZkSystemRepository

/**
 * Android provides a [ZkSystemRepository] with Longfellow circuits; other platforms return null.
 */
expect fun createZkSystemRepository(): ZkSystemRepository?
