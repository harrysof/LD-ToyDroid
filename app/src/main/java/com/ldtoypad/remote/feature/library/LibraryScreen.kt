package com.ldtoypad.remote.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ldtoypad.remote.domain.model.CatalogueItem
import com.ldtoypad.remote.domain.model.Character
import com.ldtoypad.remote.domain.model.TokenType
import com.ldtoypad.remote.domain.model.Vehicle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onBack: () -> Unit
) {
    val items by viewModel.items.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterType by viewModel.filterType.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search characters, vehicles, abilities...") }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterType == null,
                    onClick = { viewModel.setFilterType(null) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = filterType == TokenType.CHARACTER,
                    onClick = { viewModel.setFilterType(TokenType.CHARACTER) },
                    label = { Text("Characters") }
                )
                FilterChip(
                    selected = filterType == TokenType.VEHICLE,
                    onClick = { viewModel.setFilterType(TokenType.VEHICLE) },
                    label = { Text("Vehicles") }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    CatalogueItemRow(
                        item = item,
                        onCreateClick = { viewModel.createToken(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun CatalogueItemRow(
    item: CatalogueItem,
    onCreateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCreateClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = item.world,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val type = if (item is Character) "Character" else "Vehicle"
                Text(
                    text = type,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onCreateClick) {
                Icon(Icons.Default.Add, contentDescription = "Create Token")
            }
        }
    }
}
