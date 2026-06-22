package com.ldtoypad.remote.data.remote

import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class EmulatorSocketClient {
    private var socket: Socket? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _refreshTokensEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshTokensEvent = _refreshTokensEvent.asSharedFlow()

    private val _colorOneEvent = MutableSharedFlow<JSONArray>(extraBufferCapacity = 1)
    val colorOneEvent = _colorOneEvent.asSharedFlow()

    private val _colorAllEvent = MutableSharedFlow<JSONArray>(extraBufferCapacity = 1)
    val colorAllEvent = _colorAllEvent.asSharedFlow()

    private val _fadeOneEvent = MutableSharedFlow<JSONArray>(extraBufferCapacity = 1)
    val fadeOneEvent = _fadeOneEvent.asSharedFlow()

    private val _fadeAllEvent = MutableSharedFlow<JSONArray>(extraBufferCapacity = 1)
    val fadeAllEvent = _fadeAllEvent.asSharedFlow()

    fun connect(baseUrl: String) {
        disconnect()
        try {
            val options = IO.Options.builder()
                .setReconnection(true)
                .build()
            
            socket = IO.socket(baseUrl, options).apply {
                on(Socket.EVENT_CONNECT) {
                    _isConnected.value = true
                    emit("connectionStatus")
                }
                on(Socket.EVENT_DISCONNECT) {
                    _isConnected.value = false
                }
                on("refreshTokens") {
                    _refreshTokensEvent.tryEmit(Unit)
                }
                on("Color One") { args ->
                    if (args.isNotEmpty() && args[0] is JSONArray) {
                        _colorOneEvent.tryEmit(args[0] as JSONArray)
                    }
                }
                on("Color All") { args ->
                    if (args.isNotEmpty() && args[0] is JSONArray) {
                        _colorAllEvent.tryEmit(args[0] as JSONArray)
                    }
                }
                on("Fade One") { args ->
                    if (args.isNotEmpty() && args[0] is JSONArray) {
                        _fadeOneEvent.tryEmit(args[0] as JSONArray)
                    }
                }
                on("Fade All") { args ->
                    if (args.isNotEmpty() && args[0] is JSONArray) {
                        _fadeAllEvent.tryEmit(args[0] as JSONArray)
                    }
                }
                connect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun syncToyPad() {
        socket?.emit("syncToyPad")
    }

    fun deleteToken(uid: String) {
        socket?.emit("deleteToken", uid)
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        _isConnected.value = false
    }
}
