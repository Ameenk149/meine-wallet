package org.multipaz.samples.wallet.cmp

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.Logger as KtorLogger
import org.multipaz.samples.wallet.cmp.activity.IssuanceActivityStore
import org.multipaz.util.Logger

/**
 * HTTP client for OpenID4VCI provisioning with full request/response logging to [Logger]
 * (tag `OpenID4VCI-HTTP`, visible in Activity → Open logs) and into [issuanceActivityStore]
 * for structured viewing under Activity → issuance detail.
 */
fun createProvisioningHttpClient(
    engineFactory: HttpClientEngineFactory<*>,
    issuanceActivityStore: IssuanceActivityStore,
): HttpClient {
    return HttpClient(engineFactory) {
        followRedirects = false
        install(Logging) {
            logger = object : KtorLogger {
                override fun log(message: String) {
                    Logger.d("OpenID4VCI-HTTP", message)
                    issuanceActivityStore.appendProvisioningHttpLog(message)
                }
            }
            level = LogLevel.ALL
        }
    }
}
