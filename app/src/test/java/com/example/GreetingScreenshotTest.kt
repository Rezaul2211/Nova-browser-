package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.SearchEngine
import com.example.privacy.CumulativePrivacyStats
import com.example.ui.components.NewTabPage
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        NewTabPage(
          searchEngine = SearchEngine.DUCKDUCKGO,
          cumulativeStats = CumulativePrivacyStats(
            totalAdsBlocked = 1420,
            totalTrackersBlocked = 1284,
            totalRequestsIntercepted = 4500
          ),
          bookmarks = emptyList(),
          recentHistory = emptyList(),
          isPrivate = false,
          onNavigate = {},
          onOpenAi = {},
          onOpenBookmarks = {},
          onOpenHistory = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
