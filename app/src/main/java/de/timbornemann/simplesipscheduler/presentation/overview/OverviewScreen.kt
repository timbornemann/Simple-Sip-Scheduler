package de.timbornemann.simplesipscheduler.presentation.overview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import de.timbornemann.simplesipscheduler.presentation.MainViewModel

@Composable
fun OverviewScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val progress by viewModel.todayProgress.collectAsState()
    val target by viewModel.dailyTarget.collectAsState()

    val progressFraction = if (target > 0) progress?.toFloat()?.div(target) ?: 0f else 0f
    
    // Clamp fraction to 1.0 for the indicator if we want a full circle, or let it loop/fill.
    // Usually for daily goal, we clamp to 1f for the main ring, maybe show over-achievement differently.
    // For now, simple indicator.
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = progressFraction.coerceIn(0f, 1f),
            modifier = Modifier.fillMaxSize(),
            startAngle = 290f,
            endAngle = 250f,
            strokeWidth = 4.dp
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${progress ?: 0}",
                style = MaterialTheme.typography.display1,
                color = MaterialTheme.colors.primary
            )
            Text(
                text = "/ $target ml",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface
            )
        }
    }
}
