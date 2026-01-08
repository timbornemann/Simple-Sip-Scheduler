package de.timbornemann.simplesipscheduler.presentation.overview

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import de.timbornemann.simplesipscheduler.presentation.MainViewModel
import de.timbornemann.simplesipscheduler.presentation.components.AnimatedProgressRing
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OverviewScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val progress by viewModel.todayProgress.collectAsState()
    val target by viewModel.dailyTarget.collectAsState()

    val progressFraction = if (target > 0) progress?.toFloat()?.div(target) ?: 0f else 0f
    val clampedProgress = progressFraction.coerceIn(0f, 1f)
    
    // Water drop particles animation when goal is reached
    val goalReached = clampedProgress >= 1f
    var showParticles by remember { mutableStateOf(false) }
    
    LaunchedEffect(goalReached) {
        if (goalReached) {
            showParticles = true
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Use the shared AnimatedProgressRing component
        AnimatedProgressRing(
            progress = progress ?: 0,
            target = target,
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 6.dp,
            showCenterContent = true,
            compact = false
        )
        
        // Water drop particles when goal is reached
        if (showParticles && goalReached) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                repeat(8) { index ->
                    val angle = (index * 45f) * (Math.PI / 180f).toFloat()
                    val distance = 80.dp
                    val offsetX = cos(angle) * distance.value
                    val offsetY = sin(angle) * distance.value
                    
                    val particleAlpha by animateFloatAsState(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = 2000,
                            delayMillis = index * 100,
                            easing = LinearEasing
                        ),
                        label = "particle_$index"
                    )
                    
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        modifier = Modifier
                            .offset(x = offsetX.dp, y = offsetY.dp)
                            .size(16.dp)
                            .alpha(particleAlpha),
                        tint = Color(0xFF29B6F6)
                    )
                }
            }
        }
    }
}
