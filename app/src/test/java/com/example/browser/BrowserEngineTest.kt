package com.example.browser

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.privacy.CookieController
import com.example.privacy.FilterEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BrowserEngineTest {

    private lateinit var context: Context
    private lateinit var filterEngine: FilterEngine
    private lateinit var cookieController: CookieController
    private lateinit var tabManager: TabManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        filterEngine = FilterEngine()
        cookieController = CookieController(context)
        tabManager = TabManager(
            context = context,
            filterEngine = filterEngine,
            cookieController = cookieController,
            onPageCommitted = { _, _ -> },
            onDownloadRequested = { _, _, _, _, _ -> }
        )
    }

    @Test
    fun testTabCreationAndInitialLoadingState() {
        val tabId = tabManager.newTab("https://duckduckgo.com")
        assertNotNull(tabId)

        val activeTab = tabManager.tabs.value.firstOrNull { it.id == tabId }
        assertNotNull(activeTab)
        assertEquals("https://duckduckgo.com", activeTab?.url)

        val session = tabManager.getSession(tabId)
        assertNotNull(session)
        assertEquals("https://duckduckgo.com", session?.currentUrl)
    }

    @Test
    fun testNavigationAndUrlFormatting() {
        val tabId = tabManager.newTab()
        val session = tabManager.getSession(tabId)
        assertNotNull(session)

        session?.loadUrl("wikipedia.org")
        assertEquals("https://wikipedia.org", session?.currentUrl)

        session?.loadUrl("http://example.com")
        assertEquals("http://example.com", session?.currentUrl)

        session?.loadUrl("about:blank")
        assertEquals("about:blank", session?.currentUrl)
    }

    @Test
    fun testBackForwardHistoryContract() {
        val tabId = tabManager.newTab("https://duckduckgo.com")
        val session = tabManager.getSession(tabId)
        assertNotNull(session)

        // Initial state
        assertFalse(session?.canGoBack ?: true)
        assertFalse(session?.canGoForward ?: true)

        // Clear history contract
        session?.clearHistory()
        assertFalse(session?.canGoBack ?: true)
    }

    @Test
    fun testDesktopModeToggle() {
        val tabId = tabManager.newTab("https://duckduckgo.com")
        val session = tabManager.getSession(tabId)
        assertNotNull(session)

        session?.setDesktopMode(true)
        val tabAfterDesktop = tabManager.tabs.value.firstOrNull { it.id == tabId }
        assertTrue(tabAfterDesktop?.isDesktopMode == true)

        session?.setDesktopMode(false)
        val tabAfterMobile = tabManager.tabs.value.firstOrNull { it.id == tabId }
        assertFalse(tabAfterMobile?.isDesktopMode == true)
    }

    @Test
    fun testTabClosingAndRestoration() {
        val tab1 = tabManager.newTab("https://duckduckgo.com")
        val tab2 = tabManager.newTab("https://wikipedia.org")
        assertEquals(2, tabManager.tabs.value.size)

        tabManager.closeTab(tab2)
        assertEquals(1, tabManager.tabs.value.size)
        assertTrue(tabManager.hasClosedTabs())

        val restoredId = tabManager.restoreClosedTab()
        assertNotNull(restoredId)
        assertEquals(2, tabManager.tabs.value.size)
    }

    @Test
    fun testPageLoadingStateDataTypes() {
        val idleState: PageLoadingState = PageLoadingState.Idle
        assertEquals(PageLoadingState.Idle, idleState)

        val loadingState: PageLoadingState = PageLoadingState.Loading("https://example.com", 45)
        assertTrue(loadingState is PageLoadingState.Loading)
        assertEquals(45, (loadingState as PageLoadingState.Loading).progress)

        val finishedState: PageLoadingState = PageLoadingState.Finished("https://example.com", "Example Domain")
        assertTrue(finishedState is PageLoadingState.Finished)
        assertEquals("Example Domain", (finishedState as PageLoadingState.Finished).title)

        val errorState: PageLoadingState = PageLoadingState.Error("https://example.com", "Connection Refused", -6)
        assertTrue(errorState is PageLoadingState.Error)
        assertEquals(-6, (errorState as PageLoadingState.Error).errorCode)
    }

    @Test
    fun testNavigationProtectionBlocksGamblingAndMaliciousSites() {
        val navEngine = filterEngine.navigationProtectionEngine
        val currentUri = android.net.Uri.parse("https://news.example.com/article/123")

        // Gambling site should be blocked even if claimed as user gesture
        val gamblingUri = android.net.Uri.parse("https://1xbet.com/landing")
        val gamblingDecision = navEngine.evaluateNavigation(
            targetUri = gamblingUri,
            currentUri = currentUri,
            hasUserGesture = true,
            isRedirect = false
        )
        assertTrue(gamblingDecision is com.example.privacy.NavigationDecision.Block)
        assertEquals(com.example.privacy.BlockReason.GAMBLING_SPAM, (gamblingDecision as com.example.privacy.NavigationDecision.Block).reason)

        // Malicious ad redirect network should be blocked
        val maliciousUri = android.net.Uri.parse("https://popads.net/track?id=456")
        val maliciousDecision = navEngine.evaluateNavigation(
            targetUri = maliciousUri,
            currentUri = currentUri,
            hasUserGesture = false,
            isRedirect = true
        )
        assertTrue(maliciousDecision is com.example.privacy.NavigationDecision.Block)
        assertEquals(com.example.privacy.BlockReason.MALICIOUS_REDIRECT, (maliciousDecision as com.example.privacy.NavigationDecision.Block).reason)
    }

    @Test
    fun testNavigationProtectionBlocksUnsolicitedThirdPartyRedirects() {
        val navEngine = filterEngine.navigationProtectionEngine
        val currentUri = android.net.Uri.parse("https://example.org/blog")

        // Unsolicited 3rd party redirect without user gesture must be blocked
        val unsolicitedUri = android.net.Uri.parse("https://some-random-affiliate.com/offer")
        val decision = navEngine.evaluateNavigation(
            targetUri = unsolicitedUri,
            currentUri = currentUri,
            hasUserGesture = false,
            isRedirect = true
        )
        assertTrue(decision is com.example.privacy.NavigationDecision.Block)
        assertEquals(com.example.privacy.BlockReason.UNSOLICITED_REDIRECT, (decision as com.example.privacy.NavigationDecision.Block).reason)
    }

    @Test
    fun testNavigationProtectionAllowsLegitimateAuthAndPaymentFlows() {
        val navEngine = filterEngine.navigationProtectionEngine
        val currentUri = android.net.Uri.parse("https://myshop.com/checkout")

        // OAuth login redirect should be allowed
        val googleAuthUri = android.net.Uri.parse("https://accounts.google.com/o/oauth2/auth?client_id=123")
        val authDecision = navEngine.evaluateNavigation(
            targetUri = googleAuthUri,
            currentUri = currentUri,
            hasUserGesture = false,
            isRedirect = true
        )
        assertTrue(authDecision is com.example.privacy.NavigationDecision.Allow)

        // Stripe checkout gateway should be allowed
        val stripeUri = android.net.Uri.parse("https://checkout.stripe.com/pay/cs_test_123")
        val stripeDecision = navEngine.evaluateNavigation(
            targetUri = stripeUri,
            currentUri = currentUri,
            hasUserGesture = false,
            isRedirect = true
        )
        assertTrue(stripeDecision is com.example.privacy.NavigationDecision.Allow)
    }

    @Test
    fun testNavigationProtectionAllowsSameOriginAndDirectUserAction() {
        val navEngine = filterEngine.navigationProtectionEngine
        val currentUri = android.net.Uri.parse("https://subdomain.example.com/page1")

        // Same origin navigation
        val sameOriginUri = android.net.Uri.parse("https://example.com/page2")
        val sameOriginDecision = navEngine.evaluateNavigation(
            targetUri = sameOriginUri,
            currentUri = currentUri,
            hasUserGesture = false,
            isRedirect = true
        )
        assertTrue(sameOriginDecision is com.example.privacy.NavigationDecision.Allow)

        // Direct address bar navigation
        val addressBarUri = android.net.Uri.parse("https://wikipedia.org")
        val directDecision = navEngine.evaluateNavigation(
            targetUri = addressBarUri,
            currentUri = currentUri,
            hasUserGesture = false,
            isRedirect = false,
            isUserDirectAction = true
        )
        assertTrue(directDecision is com.example.privacy.NavigationDecision.Allow)
    }
}
