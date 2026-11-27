package com.android.settings.spa.app.appinfo

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.GosPackageState
import android.content.pm.GosPackageStateFlag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.R
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoSession
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@RunWith(AndroidJUnit4::class)
class AppContactScopesPreferenceTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var mockSession: MockitoSession

    @Before
    fun setUp() {
        mockSession = ExtendedMockito.mockitoSession()
            .mockStatic(GosPackageState::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()

        val gosPackageState = mock<GosPackageState> {
            on { hasFlag(GosPackageStateFlag.CONTACT_SCOPES_ENABLED) } doReturn true
        }
        whenever(GosPackageState.get(PACKAGE_NAME, 0)).thenReturn(gosPackageState)
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    @Test
    fun installedApp_displayed() {
        val app = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            flags = ApplicationInfo.FLAG_INSTALLED
        }

        setContent(app)

        composeTestRule.onNodeWithText(context.getString(R.string.contact_scopes)).assertIsDisplayed()
    }

    @Test
    fun notInstalledApp_notDisplayed() {
        val app = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
        }

        setContent(app)

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    private fun setContent(app: ApplicationInfo) {
        composeTestRule.setContent {
            AppContactScopesPreference(app)
        }
    }

    companion object {
        private const val PACKAGE_NAME = "package.name"
    }
}
