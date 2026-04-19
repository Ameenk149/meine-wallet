package org.multipaz.samples.wallet.cmp

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.multipaz.storage.Storage
import org.multipaz.storage.ios.IosStorage
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object AppPlatform {

    @OptIn(
        DelicateCoroutinesApi::class,
        ExperimentalForeignApi::class,
        ExperimentalCoroutinesApi::class
    )
    actual val storage: Storage = IosStorage(
        storageFileUrl = NSFileManager.defaultManager.containerURLForSecurityApplicationGroupIdentifier(
            groupIdentifier = "group.org.multipaz.samples.wallet.cmp.sharedgroup"
        )!!.URLByAppendingPathComponent("storageNoBackup.db")!!,
        excludeFromBackup = true
    )

    /**
     * Must match Android’s `/redirect/<applicationId>/` pattern so the issuer and apps.multipaz.org
     * use one redirect URI shape. [NSBundle.mainBundle.bundleIdentifier] is the iOS analogue of
     * `applicationContext.packageName`.
     *
     * **Server requirement:** `https://apps.multipaz.org/redirect/<your-bundle-id>/` must be allowed
     * for this OAuth client and listed in Universal Links (AASA) for `apps.multipaz.org`.
     */
    actual val redirectPath: String
        get() {
            val bundleId = NSBundle.mainBundle.bundleIdentifier
                ?: error("CFBundleIdentifier missing — cannot build OAuth redirect path")
            return "/redirect/$bundleId/"
        }

    actual val httpClientEngineFactory: HttpClientEngineFactory<*> by lazy {
        Darwin
    }
}
