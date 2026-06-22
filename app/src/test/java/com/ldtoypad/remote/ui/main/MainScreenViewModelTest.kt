package com.ldtoypad.remote.ui.main

import com.ldtoypad.remote.data.remote.EmulatorHttpClient
import com.ldtoypad.remote.data.remote.EmulatorSocketClient
import com.ldtoypad.remote.data.repository.ToyPadRepository
import com.ldtoypad.remote.domain.model.PadSlot
import com.ldtoypad.remote.domain.model.ToyPadState
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.Test
import java.time.Instant

class MainScreenViewModelTest {

    /**
     * Builds a [ToyPadRepository] with no-op HTTP/socket clients.
     * The HTTP client has an empty baseUrl so any network call returns
     * immediately with a failure — no real network access occurs.
     */
    private fun buildRepository(): ToyPadRepository {
        val httpClient = EmulatorHttpClient(
            client = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true },
            baseUrl = ""
        )
        val socketClient = EmulatorSocketClient()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        return ToyPadRepository(httpClient, socketClient, scope)
    }

    @Test
    fun padState_initiallyHasSevenSlots() = runTest {
        val repository = buildRepository()
        val state: ToyPadState = repository.padState.first()
        assertEquals(7, state.slots.size)
    }

    @Test
    fun padState_initiallyHasNoTokensOnPad() = runTest {
        val repository = buildRepository()
        val state: ToyPadState = repository.padState.first()
        assertTrue("Expected no tokens on pad initially", state.allTokens.isEmpty())
        assertTrue("Expected no tokens in toy box initially", state.toyBoxTokens.isEmpty())
    }

    @Test
    fun padState_slotsAreIndexedOneToSeven() = runTest {
        val repository = buildRepository()
        val slots: List<PadSlot> = repository.padState.first().slots
        val indices = slots.map { it.index }
        assertEquals((1..7).toList(), indices)
    }

    @Test
    fun padState_allSlotsInitiallyEmpty() = runTest {
        val repository = buildRepository()
        val slots: List<PadSlot> = repository.padState.first().slots
        assertTrue("All slots should have no token initially", slots.all { it.token == null })
    }
}
