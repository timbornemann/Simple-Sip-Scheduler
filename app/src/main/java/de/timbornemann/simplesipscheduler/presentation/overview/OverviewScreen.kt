package de.timbornemann.simplesipscheduler.presentation.overview

import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import de.timbornemann.simplesipscheduler.presentation.MainViewModel
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
    
    // Animated progress value for smooth transitions
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "progress_animation"
    )
    
    // Removed pulse animation for optimization
    
    // Water drop particles animation when goal is reached
    val goalReached = clampedProgress >= 1f
    var showParticles by remember { mutableStateOf(false) }
    
    LaunchedEffect(goalReached) {
        if (goalReached) {
            showParticles = true
        }
    }
    
    // Water-themed gradient colors (blue to cyan)
    val waterGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF29B6F6), // Water Blue
            Color(0xFF03DAC5), // Cyan
            Color(0xFF00BCD4)  // Light Blue
        )
    )
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Background ring (subtle)
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val strokeWidth = 6.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)
            
            // Background ring
            drawArc(
                color = Color(0xFF1A1A1A),
                startAngle = 290f,
                sweepAngle = 320f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        
        // Animated progress ring with gradient
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val strokeWidth = 6.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)
            
            // Progress ring with gradient
            val sweepAngle = 320f * animatedProgress
            
            // Create gradient brush along the arc
            val startAngleRad = Math.toRadians(290.0)
            val endAngleRad = Math.toRadians(290.0 + sweepAngle)
            
            val startX = center.x + radius * cos(startAngleRad).toFloat()
            val startY = center.y + radius * sin(startAngleRad).toFloat()
            val endX = center.x + radius * cos(endAngleRad).toFloat()
            val endY = center.y + radius * sin(endAngleRad).toFloat()
            
            val gradientBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF29B6F6),
                    Color(0xFF03DAC5),
                    Color(0xFF00BCD4)
                ),
                start = Offset(startX, startY),
                end = Offset(endX, endY)
            )
            
            drawArc(
                brush = gradientBrush,
                startAngle = 290f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        
        // Center content
        Column(
            modifier = Modifier
                .size(140.dp)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
                // Water drop icon
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = "Wasser",
                    modifier = Modifier
                        .size(32.dp)
                        .alpha(0.8f),
                    tint = Color(0xFF29B6F6)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress text
                Text(
                    text = "${progress ?: 0}",
                    style = MaterialTheme.typography.display1,
                    color = Color(0xFF29B6F6)
                )
                
                // Target text
                Text(
                    text = "/ $target ml",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

        
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
