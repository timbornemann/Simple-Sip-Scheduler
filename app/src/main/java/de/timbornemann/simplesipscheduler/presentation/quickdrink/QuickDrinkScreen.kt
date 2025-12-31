package de.timbornemann.simplesipscheduler.presentation.quickdrink

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.items
import de.timbornemann.simplesipscheduler.presentation.MainViewModel
import de.timbornemann.simplesipscheduler.presentation.manualinput.ManualInputScreen

@Composable
fun QuickDrinkScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val buttons by viewModel.buttonConfig.collectAsState()
    var showManualInput by remember { mutableStateOf(false) }

    if (showManualInput) {
        ManualInputScreen(
            viewModel = viewModel,
            onDismiss = { showManualInput = false }
        )
    }

    ScalingLazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Quick Drink",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colors.onSurface
            )
        }
        
        items(buttons) { amount ->
            Chip(
                label = { Text("$amount ml") },
                onClick = { viewModel.addDrink(amount) },
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        item {
            Chip(
                label = { Text("Manual Input") },
                onClick = { showManualInput = true },
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
