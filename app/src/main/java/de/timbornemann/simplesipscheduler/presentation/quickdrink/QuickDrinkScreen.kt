package de.timbornemann.simplesipscheduler.presentation.quickdrink

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.items
import de.timbornemann.simplesipscheduler.presentation.MainViewModel

@Composable
fun QuickDrinkScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val buttons by viewModel.buttonConfig.collectAsState()

    ScalingLazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Schnell-Trinken",
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
    }
}
