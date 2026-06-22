package com.ldtoypad.remote.feature.pad

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ldtoypad.remote.domain.model.PadSlot
import com.ldtoypad.remote.domain.model.PadZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToyPadScreen(
    viewModel: ToyPadViewModel,
    onNavigateToLibrary: (Int) -> Unit
) {
    val padState by viewModel.padState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Toy Pad") },
                actions = {
                    IconButton(onClick = { viewModel.syncToyPad() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync Toy Pad")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Toy Pad Configuration",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // The Toy Pad has 7 slots. Let's arrange them somewhat like the physical pad.
            // Center is index 2. Left are 1, 4, 5. Right are 3, 6, 7.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Left Zone (1, 4, 5)
                ZoneColumn(
                    title = "Left",
                    slots = padState.slots.filter { it.zone == PadZone.LEFT },
                    onSlotClick = { slot -> handleSlotClick(slot, viewModel, onNavigateToLibrary) }
                )

                // Center Zone (2)
                ZoneColumn(
                    title = "Center",
                    slots = padState.slots.filter { it.zone == PadZone.CENTER },
                    onSlotClick = { slot -> handleSlotClick(slot, viewModel, onNavigateToLibrary) }
                )

                // Right Zone (3, 6, 7)
                ZoneColumn(
                    title = "Right",
                    slots = padState.slots.filter { it.zone == PadZone.RIGHT },
                    onSlotClick = { slot -> handleSlotClick(slot, viewModel, onNavigateToLibrary) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Toy Box (Unplaced Tokens)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(padState.toyBoxTokens.size) { i ->
                    val token = padState.toyBoxTokens[i]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = token.name,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private fun handleSlotClick(
    slot: PadSlot,
    viewModel: ToyPadViewModel,
    onNavigateToLibrary: (Int) -> Unit
) {
    if (slot.token != null) {
        // Remove token if tapped and already has one
        viewModel.removeToken(slot.token, slot.index)
    } else {
        // Navigate to library to select a token for this slot
        onNavigateToLibrary(slot.index)
    }
}

@Composable
fun ZoneColumn(
    title: String,
    slots: List<PadSlot>,
    onSlotClick: (PadSlot) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        slots.forEach { slot ->
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (slot.token != null) MaterialTheme.colorScheme.primaryContainer 
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onSlotClick(slot) }
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = slot.token?.name ?: "Empty\n(${slot.index})",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = if (slot.token != null) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
