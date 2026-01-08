package de.timbornemann.simplesipscheduler.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated progress ring that shows drink tracking progress.
 * When a drink is added, an animation shows the added amount "sliding in"
 * from the empty side to join the existing progress.
 */
@Composable
fun AnimatedProgressRing(
    progress: Int,
    target: Int,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 6.dp,
    showCenterContent: Boolean = true,
    compact: Boolean = false,
    onDrinkAdded: Int? = null // Amount just added (triggers animation)
) {
    val progressFraction = if (target > 0) progress.toFloat() / target else 0f
    val clampedProgress = progressFraction.coerceIn(0f, 1f)
    
    // Main progress animation
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "progress_animation"
    )
    
    // Animation state for the "sliding in" effect
    var addAnimationActive by remember { mutableStateOf(false) }
    var addedFraction by remember { mutableStateOf(0f) }
    var slideProgress by remember { mutableStateOf(0f) }
    
    // Trigger animation when drink is added
    LaunchedEffect(onDrinkAdded, progress) {
        if (onDrinkAdded != null && onDrinkAdded > 0 && target > 0) {
            addedFraction = (onDrinkAdded.toFloat() / target).coerceIn(0f, 0.3f)
            addAnimationActive = true
            slideProgress = 0f
            
            // Animate the slide
            val startTime = System.currentTimeMillis()
            val duration = 600L
            
            while (System.currentTimeMillis() - startTime < duration) {
                val elapsed = System.currentTimeMillis() - startTime
                slideProgress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                delay(16) // ~60fps
            }
            slideProgress = 1f
            
            // Keep visible briefly then fade
            delay(200)
            addAnimationActive = false
        }
    }
    
    // Colors
    val waterBlue = Color(0xFF29B6F6)
    val waterCyan = Color(0xFF03DAC5)
    val backgroundColor = Color(0xFF1A1A1A)
    val addedColor = Color(0xFF4FC3F7) // Lighter blue for added portion
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Background ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val radius = (size.minDimension - stroke) / 2
            val center = Offset(size.width / 2, size.height / 2)
            
            drawArc(
                color = backgroundColor,
                startAngle = 290f,
                sweepAngle = 320f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        
        // Progress ring with gradient
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val radius = (size.minDimension - stroke) / 2
            val center = Offset(size.width / 2, size.height / 2)
            
            val sweepAngle = 320f * animatedProgress
            
            if (sweepAngle > 0) {
                val startAngleRad = Math.toRadians(290.0)
                val endAngleRad = Math.toRadians(290.0 + sweepAngle)
                
                val startX = center.x + radius * cos(startAngleRad).toFloat()
                val startY = center.y + radius * sin(startAngleRad).toFloat()
                val endX = center.x + radius * cos(endAngleRad).toFloat()
                val endY = center.y + radius * sin(endAngleRad).toFloat()
                
                val gradientBrush = Brush.linearGradient(
                    colors = listOf(waterBlue, waterCyan, Color(0xFF00BCD4)),
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
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        
        // "Slide in" animation for added drink
        if (addAnimationActive && addedFraction > 0) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = strokeWidth.toPx()
                val radius = (size.minDimension - stroke) / 2
                val center = Offset(size.width / 2, size.height / 2)
                
                // The added portion starts at the end position and slides toward the current progress
                val totalSweep = 320f
                val currentProgressAngle = totalSweep * clampedProgress
                val addedAngle = totalSweep * addedFraction
                
                // Calculate the sliding position
                // Start: at the far end (290 + 320 = 610 -> normalized to 250)
                // End: just after current progress (290 + currentProgressAngle)
                val endPosition = 290f + currentProgressAngle - addedAngle
                val startPosition = 290f + totalSweep - addedAngle
                
                // Interpolate position based on slide progress (using ease-out)
                val easedSlide = 1f - (1f - slideProgress) * (1f - slideProgress)
                val currentPosition = startPosition + (endPosition - startPosition) * easedSlide
                
                // Draw the sliding segment with glow effect
                // Outer glow
                drawArc(
                    color = addedColor.copy(alpha = 0.3f * (1f - slideProgress * 0.5f)),
                    startAngle = currentPosition,
                    sweepAngle = addedAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = stroke * 2f, cap = StrokeCap.Round)
                )
                
                // Main segment
                drawArc(
                    color = addedColor,
                    startAngle = currentPosition,
                    sweepAngle = addedAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        
        // Center content
        if (showCenterContent) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (compact) 8.dp else 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = "Water",
                    modifier = Modifier
                        .size(if (compact) 20.dp else 32.dp)
                        .alpha(0.8f),
                    tint = waterBlue
                )
                
                Spacer(modifier = Modifier.height(if (compact) 2.dp else 8.dp))
                
                Text(
                    text = "$progress",
                    style = if (compact) MaterialTheme.typography.title2 else MaterialTheme.typography.display1,
                    color = waterBlue
                )
                
                Text(
                    text = "/ $target ml",
                    style = if (compact) MaterialTheme.typography.caption3 else MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

