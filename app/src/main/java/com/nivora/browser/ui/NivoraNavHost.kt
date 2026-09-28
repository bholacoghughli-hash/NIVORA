package com.nivora.browser.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nivora.browser.ui.ai.AiScreen
import com.nivora.browser.ui.backup.BackupRestoreScreen
import com.nivora.browser.ui.bookmarks.BookmarksScreen
import com.nivora.browser.ui.browser.BrowserScreen
import com.nivora.browser.ui.browser.BrowserViewModel
import com.nivora.browser.ui.downloads.DownloadsScreen
import com.nivora.browser.ui.history.HistoryScreen
import com.nivora.browser.ui.home.HomeScreen
import com.nivora.browser.ui.notes.NotesScreen
import com.nivora.browser.ui.pdf.PdfViewerScreen
import com.nivora.browser.ui.permissions.PermissionsScreen
import com.nivora.browser.ui.privacy.CookieManagerScreen
import com.nivora.browser.ui.privacy.PasswordGeneratorScreen
import com.nivora.browser.ui.privacy.PrivacyDashboardScreen
import com.nivora.browser.ui.privacy.PrivacyPolicyScreen
import com.nivora.browser.ui.privacy.PrivacyShieldScreen
import com.nivora.browser.ui.privacy.SecurityCenterScreen
import com.nivora.browser.ui.qr.QrGeneratorScreen
import com.nivora.browser.ui.qr.QrScannerScreen
import com.nivora.browser.ui.reader.ReaderModeScreen
import com.nivora.browser.ui.readinglist.ReadingListScreen
import com.nivora.browser.ui.settings.SettingsScreen
import com.nivora.browser.ui.sync.SyncScreen
import com.nivora.browser.ui.tabs.TabsScreen
import com.nivora.browser.ui.tools.ToolsScreen
import com.nivora.browser.ui.webapps.WebAppsScreen

object NivoraDestinations {
    const val HOME = "home"
    const val BROWSER = "browser"
    const val TABS = "tabs"
    const val BOOKMARKS = "bookmarks"
    const val HISTORY = "history"
    const val DOWNLOADS = "downloads"
    const val READING_LIST = "reading_list"
    const val AI = "ai"
    const val TOOLS = "tools"
    const val SETTINGS = "settings"
    const val QR_SCANNER = "qr_scanner"
    const val QR_GENERATOR = "qr_generator"
    const val PDF_VIEWER = "pdf_viewer"
    const val PERMISSIONS = "permissions"
    const val PRIVACY_DASHBOARD = "privacy_dashboard"
    const val PRIVACY_SHIELD = "privacy_shield"
    const val COOKIE_MANAGER = "cookie_manager"
    const val SECURITY_CENTER = "security_center"
    const val PASSWORD_GENERATOR = "password_generator"
    const val PRIVACY_POLICY = "privacy_policy"
    const val NOTES = "notes"
    const val WEB_APPS = "web_apps"
    const val SYNC = "sync"
    const val BACKUP = "backup"
    const val READER_MODE = "reader_mode"
}

@Composable
fun NivoraNavHost(
    navController: NavHostController,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NivoraDestinations.HOME,
        modifier = modifier
    ) {
        composable(NivoraDestinations.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateUrl = { url ->
                    viewModel.loadUrlOrSearch(url)
                    navController.navigate(NivoraDestinations.BROWSER)
                },
                onNavigateBookmarks = { navController.navigate(NivoraDestinations.BOOKMARKS) },
                onNavigateHistory = { navController.navigate(NivoraDestinations.HISTORY) },
                onNavigateDownloads = { navController.navigate(NivoraDestinations.DOWNLOADS) },
                onNavigateReadingList = { navController.navigate(NivoraDestinations.READING_LIST) },
                onNavigateAi = { navController.navigate(NivoraDestinations.AI) },
                onNavigateSettings = { navController.navigate(NivoraDestinations.SETTINGS) }
            )
        }

        composable(NivoraDestinations.BROWSER) {
            BrowserScreen(
                viewModel = viewModel,
                onNavigateHome = { navController.navigate(NivoraDestinations.HOME) },
                onNavigateTabs = { navController.navigate(NivoraDestinations.TABS) },
                onNavigateBookmarks = { navController.navigate(NivoraDestinations.BOOKMARKS) },
                onNavigateHistory = { navController.navigate(NivoraDestinations.HISTORY) },
                onNavigateDownloads = { navController.navigate(NivoraDestinations.DOWNLOADS) },
                onNavigateTools = { navController.navigate(NivoraDestinations.TOOLS) },
                onNavigateSettings = { navController.navigate(NivoraDestinations.SETTINGS) }
            )
        }

        composable(NivoraDestinations.TABS) {
            TabsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateHome = { navController.navigate(NivoraDestinations.HOME) }
            )
        }

        composable(NivoraDestinations.BOOKMARKS) {
            BookmarksScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenUrl = { url ->
                    viewModel.loadUrlOrSearch(url)
                    navController.navigate(NivoraDestinations.BROWSER)
                }
            )
        }

        composable(NivoraDestinations.HISTORY) {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenUrl = { url ->
                    viewModel.loadUrlOrSearch(url)
                    navController.navigate(NivoraDestinations.BROWSER)
                }
            )
        }

        composable(NivoraDestinations.READING_LIST) {
            ReadingListScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenUrl = { url ->
                    viewModel.loadUrlOrSearch(url)
                    navController.navigate(NivoraDestinations.BROWSER)
                }
            )
        }

        composable(NivoraDestinations.DOWNLOADS) {
            DownloadsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NivoraDestinations.AI) {
            AiScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NivoraDestinations.TOOLS) {
            ToolsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateQrScanner = { navController.navigate(NivoraDestinations.QR_SCANNER) },
                onNavigateQrGenerator = { navController.navigate(NivoraDestinations.QR_GENERATOR) },
                onNavigatePdfViewer = { navController.navigate(NivoraDestinations.PDF_VIEWER) },
                onNavigatePermissions = { navController.navigate(NivoraDestinations.PERMISSIONS) },
                onNavigateReaderMode = { navController.navigate(NivoraDestinations.READER_MODE) },
                onNavigateNotes = { navController.navigate(NivoraDestinations.NOTES) },
                onNavigateWebApps = { navController.navigate(NivoraDestinations.WEB_APPS) },
                onNavigateBackup = { navController.navigate(NivoraDestinations.BACKUP) },
                onNavigatePasswordGenerator = { navController.navigate(NivoraDestinations.PASSWORD_GENERATOR) }
            )
        }

        composable(NivoraDestinations.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigatePrivacyDashboard = { navController.navigate(NivoraDestinations.PRIVACY_DASHBOARD) },
                onNavigateShield = { navController.navigate(NivoraDestinations.PRIVACY_SHIELD) },
                onNavigateCookies = { navController.navigate(NivoraDestinations.COOKIE_MANAGER) },
                onNavigateSecurity = { navController.navigate(NivoraDestinations.SECURITY_CENTER) },
                onNavigatePasswordGenerator = { navController.navigate(NivoraDestinations.PASSWORD_GENERATOR) },
                onNavigatePrivacyPolicy = { navController.navigate(NivoraDestinations.PRIVACY_POLICY) },
                onNavigateSync = { navController.navigate(NivoraDestinations.SYNC) },
                onNavigateBackup = { navController.navigate(NivoraDestinations.BACKUP) },
                onNavigateWebApps = { navController.navigate(NivoraDestinations.WEB_APPS) }
            )
        }

        composable(NivoraDestinations.QR_SCANNER) {
            QrScannerScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateUrl = { url ->
                    viewModel.loadUrlOrSearch(url)
                    navController.navigate(NivoraDestinations.BROWSER)
                }
            )
        }

        composable(NivoraDestinations.QR_GENERATOR) {
            QrGeneratorScreen(
                initialUrl = viewModel.activeEngineState.value.url,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NivoraDestinations.PDF_VIEWER) {
            PdfViewerScreen(
                pdfTitle = "NIVORA Document Viewer",
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NivoraDestinations.PERMISSIONS) {
            PermissionsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NivoraDestinations.PRIVACY_DASHBOARD) {
            PrivacyDashboardScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateShield = { navController.navigate(NivoraDestinations.PRIVACY_SHIELD) },
                onNavigateCookies = { navController.navigate(NivoraDestinations.COOKIE_MANAGER) },
                onNavigateSecurity = { navController.navigate(NivoraDestinations.SECURITY_CENTER) },
                onNavigatePasswordGenerator = { navController.navigate(NivoraDestinations.PASSWORD_GENERATOR) },
                onNavigatePermissions = { navController.navigate(NivoraDestinations.PERMISSIONS) },
                onNavigatePrivacyPolicy = { navController.navigate(NivoraDestinations.PRIVACY_POLICY) }
            )
        }

        composable(NivoraDestinations.PRIVACY_SHIELD) {
            PrivacyShieldScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NivoraDestinations.COOKIE_MANAGER) {
            CookieManagerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NivoraDestinations.SECURITY_CENTER) {
            SecurityCenterScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NivoraDestinations.PASSWORD_GENERATOR) {
            PasswordGeneratorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NivoraDestinations.PRIVACY_POLICY) {
            PrivacyPolicyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NivoraDestinations.NOTES) {
            NotesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenUrl = { url ->
                    viewModel.loadUrlOrSearch(url)
                    navController.navigate(NivoraDestinations.BROWSER)
                }
            )
        }

        composable(NivoraDestinations.WEB_APPS) {
            WebAppsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenUrl = { url ->
                    viewModel.loadUrlOrSearch(url)
                    navController.navigate(NivoraDestinations.BROWSER)
                }
            )
        }

        composable(NivoraDestinations.SYNC) {
            SyncScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NivoraDestinations.BACKUP) {
            BackupRestoreScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NivoraDestinations.READER_MODE) {
            val state = viewModel.activeEngineState.value
            val content = if (state.title.isNotBlank()) {
                "${state.title}\n\nArticle excerpt: Loaded securely via NIVORA Reader Mode. Full text is parsed into clean, readable typography with customizable line spacing, font sizes, and reader themes."
            } else ""
            ReaderModeScreen(
                title = state.title,
                url = state.url,
                content = content,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
