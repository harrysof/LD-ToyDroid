package com.ldtoypad.remote.feature.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ldtoypad.remote.data.repository.ConnectionRepository
import com.ldtoypad.remote.domain.usecase.ConnectToServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectionViewModel(
    private val connectionRepository: ConnectionRepository,
    private val connectToServer: ConnectToServer
) : ViewModel() {

    val connectionState = connectionRepository.connectionState

    private val _urlInput = MutableStateFlow("")
    val urlInput = _urlInput.asStateFlow()

    private val _rememberConnection = MutableStateFlow(true)
    val rememberConnection = _rememberConnection.asStateFlow()

    init {
        viewModelScope.launch {
            val savedUrl = connectionRepository.getSavedBaseUrl()
            if (savedUrl != null) {
                _urlInput.value = savedUrl
                _rememberConnection.value = true
            }
        }
    }

    fun setUrlInput(url: String) {
        _urlInput.value = url
    }

    fun setRememberConnection(remember: Boolean) {
        _rememberConnection.value = remember
    }

    fun connect() {
        val url = _urlInput.value.trim()
        val normalizedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "http://$url"
        } else {
            url
        }
        
        viewModelScope.launch {
            connectToServer(normalizedUrl, _rememberConnection.value)
        }
    }

    fun disconnect() {
        connectionRepository.disconnect()
    }
}
