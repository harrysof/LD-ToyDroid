package com.ldtoypad.remote.feature.connection

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ldtoypad.remote.core.result.AppError
import com.ldtoypad.remote.domain.model.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    viewModel: ConnectionViewModel,
    onConnected: () -> Unit
) {
    val urlInput by viewModel.urlInput.collectAsState()
    val rememberConnection by viewModel.rememberConnection.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    if (connectionState is ConnectionState.Connected) {
        onConnected()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("LD Toy Pad Remote") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = { viewModel.setUrlInput(it) },
                label = { Text("Server Address (e.g. 192.168.1.25)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberConnection,
                    onCheckedChange = { viewModel.setRememberConnection(it) }
                )
                Text("Remember connection")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.connect() },
                modifier = Modifier.fillMaxWidth(),
                enabled = connectionState !is ConnectionState.Connecting && urlInput.isNotBlank()
            ) {
                if (connectionState is ConnectionState.Connecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connecting...")
                } else {
                    Text("Connect")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (connectionState is ConnectionState.Failed) {
                val error = (connectionState as ConnectionState.Failed).error
                val errorMessage = when (error) {
                    is AppError.Network -> "The Toy Pad server did not respond or network error."
                    is AppError.Server -> "Server error: ${error.code} ${error.message}"
                    is AppError.Parsing -> "The emulator returned data this app cannot read."
                    is AppError.State -> error.message
                    is AppError.Unknown -> "An unknown error occurred."
                }

                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
