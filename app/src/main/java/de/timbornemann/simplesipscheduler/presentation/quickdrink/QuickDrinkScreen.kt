package de.timbornemann.simplesipscheduler.presentation.quickdrink

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import de.timbornemann.simplesipscheduler.presentation.MainViewModel
import de.timbornemann.simplesipscheduler.presentation.components.AnimatedProgressRing
import de.timbornemann.simplesipscheduler.presentation.manualinput.ManualInputScreen

@Composable
fun QuickDrinkScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val buttons by viewModel.buttonConfig.collectAsState()
    val progress by viewModel.todayProgress.collectAsState()
    val target by viewModel.dailyTarget.collectAsState()
    var showManualInput by remember { mutableStateOf(false) }
    
    // Track the last added amount for animation
    var lastAddedAmount by remember { mutableStateOf<Int?>(null) }
    var animationKey by remember { mutableStateOf(0) }

    if (showManualInput) {
        ManualInputScreen(
            viewModel = viewModel,
            onDismiss = { showManualInput = false }
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Animated progress ring as background
        AnimatedProgressRing(
            progress = progress ?: 0,
            target = target,
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 6.dp,
            showCenterContent = false,
            onDrinkAdded = lastAddedAmount?.let { if (animationKey > 0) it else null }
        )
        
        // Content overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Progress display (compact)
            Text(
                text = "${progress ?: 0} / $target ml",
                style = MaterialTheme.typography.caption1,
                color = Color(0xFF29B6F6)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Quick drink buttons in a grid layout
            val chunkedButtons = buttons.chunked(3)
            chunkedButtons.forEach { rowButtons ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
                ) {
                    rowButtons.forEach { amount ->
                        CompactChip(
                            onClick = {
                                lastAddedAmount = amount
                                animationKey++
                                viewModel.addDrink(amount)
                            },
                            label = {
                                Text(
                                    text = "+$amount",
                                    style = MaterialTheme.typography.caption2
                                )
                            },
                            colors = ChipDefaults.primaryChipColors(
                                backgroundColor = Color(0xFF29B6F6).copy(alpha = 0.2f),
                                contentColor = Color(0xFF29B6F6)
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Manual input button
            CompactChip(
                onClick = { showManualInput = true },
                label = {
                    Text(
                        text = "Manual",
                        style = MaterialTheme.typography.caption2
                    )
                },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}
