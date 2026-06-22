package com.ldtoypad.remote.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.ldtoypad.remote.data.local.settings.ConnectionSettingsStore
import com.ldtoypad.remote.data.remote.EmulatorHttpClient
import com.ldtoypad.remote.data.remote.EmulatorSocketClient
import com.ldtoypad.remote.data.repository.ConnectionRepository
import com.ldtoypad.remote.domain.usecase.ConnectToServer
import com.ldtoypad.remote.feature.connection.ConnectionScreen
import com.ldtoypad.remote.feature.connection.ConnectionViewModel
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for [ConnectionScreen] — the app's main entry screen.
 *
 * All dependencies are constructed inline. The HTTP client uses an empty
 * baseUrl so network calls short-circuit immediately; the DataStore uses
 * the real instrumentation target context (sandboxed per test run).
 */
class MainScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun buildViewModel(): ConnectionViewModel {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val httpClient = EmulatorHttpClient(
            client = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true },
            baseUrl = ""
        )
        val socketClient = EmulatorSocketClient()
        val settingsStore = ConnectionSettingsStore(context)
        val connectionRepository = ConnectionRepository(httpClient, socketClient, settingsStore)
        val connectToServer = ConnectToServer(connectionRepository)
        return ConnectionViewModel(connectionRepository, connectToServer)
    }

    @Test
    fun connectionScreen_appTitle_isDisplayed() {
        composeTestRule.setContent {
            ConnectionScreen(viewModel = buildViewModel(), onConnected = {})
        }
        composeTestRule
            .onNodeWithText("LD Toy Pad Remote")
            .assertIsDisplayed()
    }

    @Test
    fun connectionScreen_serverAddressField_isDisplayed() {
        composeTestRule.setContent {
            ConnectionScreen(viewModel = buildViewModel(), onConnected = {})
        }
        composeTestRule
            .onNodeWithText("Server Address (e.g. 192.168.1.25)")
            .assertIsDisplayed()
    }

    @Test
    fun connectionScreen_connectButton_isDisplayed() {
        composeTestRule.setContent {
            ConnectionScreen(viewModel = buildViewModel(), onConnected = {})
        }
        composeTestRule
            .onNodeWithText("Connect")
            .assertIsDisplayed()
    }
}
