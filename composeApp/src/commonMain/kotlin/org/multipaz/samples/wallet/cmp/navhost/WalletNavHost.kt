package org.multipaz.samples.wallet.cmp.navhost

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import org.multipaz.compose.document.DocumentModel
import org.multipaz.document.DocumentStore
import org.multipaz.presentment.PresentmentSource
import org.multipaz.prompt.PromptModel
import org.multipaz.samples.wallet.cmp.SettingsModel
import org.multipaz.samples.wallet.cmp.WalletRoute
import org.multipaz.eventlogger.SimpleEventLogger
import org.multipaz.samples.wallet.cmp.activity.IssuanceActivityStore
import org.multipaz.samples.wallet.cmp.logging.AppLogCollector
import org.multipaz.samples.wallet.cmp.ui.ActivityEventDetailScreen
import org.multipaz.samples.wallet.cmp.ui.ActivityIssuanceDetailScreen
import org.multipaz.samples.wallet.cmp.ui.ActivityScreen
import org.multipaz.samples.wallet.cmp.ui.AppLogsScreen
import org.multipaz.samples.wallet.cmp.ui.DocumentDetailsScreen
import org.multipaz.samples.wallet.cmp.ui.DocumentClaimsScreen
import org.multipaz.samples.wallet.cmp.ui.DocumentViewerScreen
import org.multipaz.samples.wallet.cmp.ui.WalletScreen

@Composable
fun WalletNavHost(
    documentModel: DocumentModel,
    settingsModel: SettingsModel,
    promptModel: PromptModel,
    presentmentSource: PresentmentSource,
    documentStore: DocumentStore,
    activityEventLogger: SimpleEventLogger,
    issuanceActivityStore: IssuanceActivityStore,
    onCreateTestMdl: () -> Unit = {},
) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = WalletRoute.WalletList,
    ) {
        composable<WalletRoute.AppLogs> {
            AppLogsScreen(
                onBack = { navController.popBackStack() },
                onClear = { AppLogCollector.clear() },
            )
        }

        composable<WalletRoute.Activity> {
            ActivityScreen(
                eventLogger = activityEventLogger,
                issuanceActivityStore = issuanceActivityStore,
                onBack = { navController.popBackStack() },
                onOpenLogs = { navController.navigate(WalletRoute.AppLogs) },
                onPresentationSelected = { eventId ->
                    navController.navigate(WalletRoute.ActivityEventDetail(eventId = eventId))
                },
                onIssuanceSelected = { recordId ->
                    navController.navigate(WalletRoute.ActivityIssuanceDetail(recordId = recordId))
                },
            )
        }

        composable<WalletRoute.ActivityIssuanceDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<WalletRoute.ActivityIssuanceDetail>()
            ActivityIssuanceDetailScreen(
                recordId = route.recordId,
                issuanceActivityStore = issuanceActivityStore,
                onBack = { navController.popBackStack() },
            )
        }

        composable<WalletRoute.ActivityEventDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<WalletRoute.ActivityEventDetail>()
            ActivityEventDetailScreen(
                eventId = route.eventId,
                eventLogger = activityEventLogger,
                onBack = { navController.popBackStack() },
            )
        }

        composable<WalletRoute.WalletList> {
            WalletScreen(
                documentModel = documentModel,
                settingsModel = settingsModel,
                onDocumentSelected = { documentInfo ->
                    navController.navigate(
                        WalletRoute.WalletDetails(
                            documentId = documentInfo.document.identifier
                        )
                    )
                },
                onActivity = { navController.navigate(WalletRoute.Activity) },
                onCreateTestMdl = onCreateTestMdl,
            )
        }

        composable<WalletRoute.WalletDetails> { backStackEntry ->
            val route = backStackEntry.toRoute<WalletRoute.WalletDetails>()
            DocumentViewerScreen(
                documentId = route.documentId,
                documentModel = documentModel,
                promptModel = promptModel,
                presentmentSource = presentmentSource,
                onBack = { navController.popBackStack() },
                onMenuClick = {
                    navController.navigate(
                        WalletRoute.DocumentDetails(documentId = route.documentId)
                    )
                }
            )
        }

        composable<WalletRoute.PersonalIdInfo> { backStackEntry ->
            val route = backStackEntry.toRoute<WalletRoute.PersonalIdInfo>()
            DocumentClaimsScreen(
                documentId = route.documentId,
                documentModel = documentModel,
                documentTypeRepository = presentmentSource.documentTypeRepository,
                onBack = { navController.popBackStack() }
            )
        }

        composable<WalletRoute.DocumentDetails> { backStackEntry ->
            val route = backStackEntry.toRoute<WalletRoute.DocumentDetails>()
            DocumentDetailsScreen(
                documentId = route.documentId,
                documentModel = documentModel,
                onBack = { navController.popBackStack() },
                onPersonalIdInfoClick = {
                    navController.navigate(
                        WalletRoute.PersonalIdInfo(documentId = route.documentId)
                    )
                },
                onRemove = {
                    coroutineScope.launch {
                        documentStore.deleteDocument(route.documentId)
                        navController.popBackStack(
                            route = WalletRoute.WalletList,
                            inclusive = false
                        )
                    }
                }
            )
        }
    }
}
