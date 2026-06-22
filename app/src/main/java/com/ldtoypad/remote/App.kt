package com.ldtoypad.remote

import android.app.Application
import androidx.room.Room
import com.ldtoypad.remote.data.local.AppDatabase
import com.ldtoypad.remote.data.local.settings.ConnectionSettingsStore
import com.ldtoypad.remote.data.remote.EmulatorHttpClient
import com.ldtoypad.remote.data.remote.EmulatorSocketClient
import com.ldtoypad.remote.data.repository.CatalogueRepository
import com.ldtoypad.remote.data.repository.ConnectionRepository
import com.ldtoypad.remote.data.repository.PresetRepository
import com.ldtoypad.remote.data.repository.ToyPadRepository
import com.ldtoypad.remote.domain.usecase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

class AppContainer(private val application: Application) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val json = Json { ignoreUnknownKeys = true }
    private val okHttpClient = OkHttpClient.Builder().build()

    val emulatorHttpClient = EmulatorHttpClient(okHttpClient, json)
    val emulatorSocketClient = EmulatorSocketClient()
    val connectionSettingsStore = ConnectionSettingsStore(application)

    val appDatabase = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "ldtoypad_db"
    ).build()

    val connectionRepository = ConnectionRepository(emulatorHttpClient, emulatorSocketClient, connectionSettingsStore)
    val catalogueRepository = CatalogueRepository(emulatorHttpClient)
    val toyPadRepository = ToyPadRepository(emulatorHttpClient, emulatorSocketClient, applicationScope)
    val presetRepository = PresetRepository(appDatabase.presetDao())

    val connectToServer = ConnectToServer(connectionRepository)
    val createToken = CreateToken(toyPadRepository)
    val placeToken = PlaceToken(toyPadRepository)
    val removeToken = RemoveToken(toyPadRepository)
    val moveToken = MoveToken(removeToken, placeToken)
    val deleteToken = DeleteToken(toyPadRepository)
    val loadPreset = LoadPreset(presetRepository, toyPadRepository)
    val savePreset = SavePreset(presetRepository)
}

class App : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
