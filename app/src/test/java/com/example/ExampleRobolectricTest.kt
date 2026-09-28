package com.example

import android.content.Context
import android.webkit.URLUtil
import androidx.test.core.app.ApplicationProvider
import com.nivora.browser.ai.LocalHeuristicAIProvider
import com.nivora.browser.ai.PageContext
import com.nivora.browser.browser.tabs.TabManager
import com.nivora.browser.data.backup.DataTransferManager
import com.nivora.browser.data.local.SearchProvider
import com.nivora.browser.database.NivoraDatabase
import com.nivora.browser.data.repository.BrowserRepository
import com.nivora.browser.privacy.DefaultSafeBrowsingProvider
import com.nivora.browser.privacy.PasswordGenerator
import com.nivora.browser.privacy.PasswordOptions
import com.nivora.browser.privacy.PasswordStrength
import com.nivora.browser.privacy.SafeBrowsingThreat
import com.nivora.browser.privacy.TrackerBlockerEngine
import com.nivora.browser.search.SearchResolver
import com.nivora.browser.translation.TranslationEngine
import com.nivora.browser.ui.browser.HistoryTimeRange
import com.nivora.browser.ui.qr.QrCodeGenerator
import com.nivora.browser.webapps.WebAppManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NIVORA", appName)
    }

    @Test
    fun `search resolver correctly identifies URLs`() {
        assertTrue(SearchResolver.isUrl("https://duckduckgo.com"))
        assertTrue(SearchResolver.isUrl("http://example.com"))
        assertTrue(SearchResolver.isUrl("wikipedia.org"))
        assertTrue(SearchResolver.isUrl("github.com/android"))
        assertFalse(SearchResolver.isUrl("how to make pasta"))
        assertFalse(SearchResolver.isUrl("jetpack compose browser"))
    }

    @Test
    fun `search resolver resolves query into search provider url`() {
        val resolved = SearchResolver.resolveInput("privacy browser", SearchProvider.DUCKDUCKGO)
        assertEquals("https://duckduckgo.com/?q=privacy+browser", resolved)

        val resolvedUrl = SearchResolver.resolveInput("example.com", SearchProvider.DUCKDUCKGO)
        assertEquals("https://example.com", resolvedUrl)
    }

    @Test
    fun `search resolver handles custom search shortcuts`() {
        // Test yt prefix
        val ytResult = SearchResolver.resolveInput("yt Android Compose Tutorial", SearchProvider.DUCKDUCKGO)
        assertEquals("https://www.youtube.com/results?search_query=Android+Compose+Tutorial", ytResult)

        // Test wiki prefix
        val wikiResult = SearchResolver.resolveInput("wiki Kotlin", SearchProvider.DUCKDUCKGO)
        assertEquals("https://en.wikipedia.org/wiki/Special:Search?search=Kotlin", wikiResult)

        // Test gh prefix
        val ghResult = SearchResolver.resolveInput("gh jetpack compose", SearchProvider.DUCKDUCKGO)
        assertEquals("https://github.com/search?q=jetpack+compose", ghResult)

        // Register custom shortcut
        SearchResolver.registerShortcut("stack", "https://stackoverflow.com/search?q=%s")
        val stackResult = SearchResolver.resolveInput("stack room database", SearchProvider.DUCKDUCKGO)
        assertEquals("https://stackoverflow.com/search?q=room+database", stackResult)
    }

    @Test
    fun `tab manager manages multiple tabs and active tab switching`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = TabManager(context) { _, _ -> }

        // Starts with 1 initial tab
        assertEquals(1, manager.tabs.value.size)

        // Create a new tab
        val tab2 = manager.createTab(url = "https://wikipedia.org", isIncognito = false)
        assertEquals(2, manager.tabs.value.size)
        assertEquals(tab2.id, manager.activeTabId.value)

        // Switch to initial tab
        val tab1 = manager.tabs.value.first()
        manager.selectTab(tab1.id)
        assertEquals(tab1.id, manager.activeTabId.value)
    }

    @Test
    fun `tab manager creates private tabs with incognito flag`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = TabManager(context) { _, _ -> }

        val privateTab = manager.createTab(url = "https://duckduckgo.com", isIncognito = true)
        assertTrue(privateTab.isIncognito)
        assertEquals(2, manager.tabs.value.size)
    }

    @Test
    fun `tab manager supports tab duplication and reopening closed tabs`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = TabManager(context) { _, _ -> }

        val tab = manager.createTab(url = "https://github.com", isIncognito = false)
        val dup = manager.duplicateTab(tab.id)
        assertNotNull(dup)
        assertEquals("https://github.com", dup?.url)
        assertEquals(3, manager.tabs.value.size)

        // Close the tab and verify reopen capability
        manager.closeTab(tab.id)
        assertTrue(manager.canReopenClosedTab.value)

        val reopened = manager.reopenClosedTab()
        assertNotNull(reopened)
        assertEquals("https://github.com", reopened?.url)
    }

    @Test
    fun `tab groups creation and assignment`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = TabManager(context) { _, _ -> }

        val group = manager.createGroup("Coding", "#0284C7")
        val tab = manager.createTab(url = "https://kotlinlang.org", isIncognito = false, groupId = group.id)

        assertEquals(group.id, tab.groupId)
        val foundGroup = manager.tabGroups.value.find { it.id == group.id }
        assertNotNull(foundGroup)
        assertEquals("Coding", foundGroup?.name)
    }

    @Test
    fun `history time range durations are valid`() {
        assertEquals(TimeUnit.HOURS.toMillis(1), HistoryTimeRange.LAST_HOUR.durationMillis)
        assertEquals(TimeUnit.DAYS.toMillis(1), HistoryTimeRange.LAST_24_HOURS.durationMillis)
        assertEquals(TimeUnit.DAYS.toMillis(7), HistoryTimeRange.LAST_7_DAYS.durationMillis)
        assertEquals(TimeUnit.DAYS.toMillis(28), HistoryTimeRange.LAST_4_WEEKS.durationMillis)
        assertEquals(-1L, HistoryTimeRange.ALL_TIME.durationMillis)
    }

    @Test
    fun `qr code generator produces valid bitmap`() {
        val bitmap = QrCodeGenerator.generateQrBitmap("https://nivora.browser", size = 256)
        assertNotNull(bitmap)
        assertEquals(256, bitmap.width)
        assertEquals(256, bitmap.height)
    }

    @Test
    fun `download filename guessing`() {
        val guessed = URLUtil.guessFileName("https://example.com/files/report.pdf", null, "application/pdf")
        assertEquals("report.pdf", guessed)
    }

    @Test
    fun `tracker blocker intercepts known tracking domains and counts real blocks`() {
        val engine = TrackerBlockerEngine()
        assertEquals(0, engine.stats.value.blockedOnCurrentPage)
        assertEquals(0, engine.stats.value.totalBlockedLifetime)

        // Intercepting an ad/tracker request
        val blockedResp = engine.interceptRequest("https://google-analytics.com/analytics.js", "example.com")
        assertNotNull(blockedResp)
        assertEquals(1, engine.stats.value.blockedOnCurrentPage)
        assertEquals(1, engine.stats.value.totalBlockedLifetime)

        // Intercepting another tracker
        val blockedResp2 = engine.interceptRequest("https://connect.facebook.net/en_US/fbevents.js", "example.com")
        assertNotNull(blockedResp2)
        assertEquals(2, engine.stats.value.blockedOnCurrentPage)
        assertEquals(2, engine.stats.value.totalBlockedLifetime)

        // Intercepting legitimate content - should NOT be blocked
        val allowedResp = engine.interceptRequest("https://cdn.example.com/main.js", "example.com")
        assertNull(allowedResp)
        assertEquals(2, engine.stats.value.blockedOnCurrentPage)

        // Whitelisting site allows all requests
        engine.allowSite("example.com")
        val bypassedResp = engine.interceptRequest("https://google-analytics.com/analytics.js", "example.com")
        assertNull(bypassedResp)
    }

    @Test
    fun `password generator creates secure password with required constraints and entropy`() {
        val options = PasswordOptions(
            length = 20,
            includeUppercase = true,
            includeLowercase = true,
            includeNumbers = true,
            includeSymbols = true
        )
        val password = PasswordGenerator.generatePassword(options)
        assertEquals(20, password.length)
        assertTrue(password.any { it.isUpperCase() })
        assertTrue(password.any { it.isLowerCase() })
        assertTrue(password.any { it.isDigit() })

        val entropy = PasswordGenerator.calculateEntropyBits(password)
        assertTrue("Entropy should be >= 80 bits for length 20 with full pool", entropy >= 80.0)
        assertEquals(PasswordStrength.VERY_STRONG, PasswordGenerator.getStrength(entropy))
    }

    @Test
    fun `safe browsing provider detects test threat vectors`() {
        val provider = DefaultSafeBrowsingProvider()

        val safeResult = provider.checkUrl("https://en.wikipedia.org/wiki/Kotlin")
        assertFalse(safeResult.isThreat)

        val threatResult = provider.checkUrl("https://testsafebrowsing.appspot.com/malware.html")
        assertTrue(threatResult.isThreat)
        assertEquals(SafeBrowsingThreat.MALWARE, threatResult.threatType)

        val phishingResult = provider.checkUrl("https://phishing.testing.google.test/login")
        assertTrue(phishingResult.isThreat)
        assertEquals(SafeBrowsingThreat.PHISHING, phishingResult.threatType)
    }

    @Test
    fun `ai provider generates structured responses locally`() = runBlocking {
        val provider = LocalHeuristicAIProvider()
        val page = PageContext(
            url = "https://kotlinlang.org",
            title = "Kotlin Programming Language",
            contentSnippet = "Kotlin is a modern, concise and safe programming language. It is the premier choice for modern Android development."
        )

        val summary = provider.processRequest("Summarize this page", page)
        assertTrue(summary.success)
        assertTrue(summary.generatedContent.contains("Executive Summary"))
        assertEquals("NIVORA Smart Engine (On-Device)", summary.providerUsed)

        val notes = provider.processRequest("Generate notes", page)
        assertTrue(notes.generatedContent.contains("Research Note"))
    }

    @Test
    fun `translation engine detects languages correctly`() {
        val hindi = TranslationEngine.detectLanguage("नमस्ते दुनिया")
        assertEquals("hi", hindi.code)

        val japanese = TranslationEngine.detectLanguage("こんにちは")
        assertEquals("ja", japanese.code)

        val english = TranslationEngine.detectLanguage("Hello world this is an Android browser")
        assertEquals("en", english.code)
    }

    @Test
    fun `web app manager manages pwa items`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = WebAppManager(context)

        val item = manager.addWebApp("GitHub PWA", "https://github.com")
        assertTrue(manager.hasWebApp("https://github.com"))

        manager.removeWebApp(item.id)
        assertFalse(manager.hasWebApp("https://github.com"))
    }

    @Test
    fun `data transfer manager imports and validates html bookmarks`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = NivoraDatabase.getDatabase(context)
        val repo = BrowserRepository(db)

        val sampleHtml = """
            <!DOCTYPE NETSCAPE-Bookmark-file-1>
            <H1>Bookmarks</H1>
            <DL><p>
                <DT><A HREF="https://android.com">Android Official</A>
                <DT><A HREF="javascript:alert(1)">Malicious Script</A>
            </DL><p>
        """.trimIndent()

        val count = DataTransferManager.importBookmarkHtml(sampleHtml, repo)
        // Should only import the valid https url and reject javascript scheme
        assertEquals(1, count)
    }

    @Test
    fun `notes repository operations support search and pinning`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = NivoraDatabase.getDatabase(context)
        val repo = BrowserRepository(db)

        val noteId = repo.addNote(
            title = "Jetpack Compose Research",
            content = "Declarative UI toolkit developed by Bhaskar for modern Android apps.",
            url = "https://developer.android.com/compose",
            isPinned = false
        )
        assertTrue(noteId > 0)

        val notes = repo.allNotes.first()
        val retrieved = notes.firstOrNull { it.id == noteId }
        assertNotNull(retrieved)
        assertEquals("Jetpack Compose Research", retrieved?.title)
        assertFalse(retrieved?.isPinned ?: true)

        // Toggle pin
        if (retrieved != null) {
            repo.togglePinNote(retrieved)
        }
        val updatedNotes = repo.allNotes.first()
        val pinned = updatedNotes.firstOrNull { it.id == noteId }
        assertTrue(pinned?.isPinned ?: false)

        // Search notes
        val searchResults = repo.searchNotes("Bhaskar").first()
        assertTrue(searchResults.any { it.id == noteId })
    }

    @Test
    fun `reading list supports item insertion, status toggle, and deletion`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = NivoraDatabase.getDatabase(context)
        val repo = BrowserRepository(db)

        repo.addToReadingList("NIVORA Architecture", "https://nivora.dev/arch")
        val items = repo.readingList.first()
        val target = items.firstOrNull { it.url == "https://nivora.dev/arch" }
        assertNotNull(target)
        assertFalse(target?.isRead ?: true)

        // Set read status
        if (target != null) {
            repo.setReadingItemStatus(target.id, isRead = true)
        }
        val updatedItems = repo.readingList.first()
        val readItem = updatedItems.firstOrNull { it.url == "https://nivora.dev/arch" }
        assertTrue(readItem?.isRead ?: false)

        // Delete item
        if (readItem != null) {
            repo.deleteReadingItem(readItem.id)
        }
        val remainingItems = repo.readingList.first()
        assertNull(remainingItems.firstOrNull { it.url == "https://nivora.dev/arch" })
    }
}
