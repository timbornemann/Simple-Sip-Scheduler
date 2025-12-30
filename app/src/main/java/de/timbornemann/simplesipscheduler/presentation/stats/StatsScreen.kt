package de.timbornemann.simplesipscheduler.presentation.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Alert
import de.timbornemann.simplesipscheduler.data.database.DaySum
import de.timbornemann.simplesipscheduler.presentation.MainViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class StatsView {
    DAY, WEEK, MONTH
}

@Composable
fun StatsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var currentView by remember { mutableStateOf(StatsView.DAY) }
    val todayEntries by viewModel.todayEntries.collectAsState()
    val todayTotal by viewModel.todayProgress.collectAsState()
    val weekStats by viewModel.weekStats.collectAsState()
    val monthStats by viewModel.monthStats.collectAsState()

    var entryToEdit by remember { mutableStateOf<de.timbornemann.simplesipscheduler.data.database.DrinkEntry?>(null) }

    if (entryToEdit != null) {
        val entry = entryToEdit!!
        var currentAmount by remember(entry.id) { mutableStateOf(entry.amountMl) }
        
        androidx.wear.compose.material.dialog.Alert(
            title = { Text("Menge ändern") },
            positiveButton = { 
                Button(
                    onClick = { 
                        viewModel.updateEntry(entry, currentAmount)
                        entryToEdit = null 
                    }, 
                    colors = ButtonDefaults.primaryButtonColors()
                ) { 
                    Text("OK") 
                } 
            },
            negativeButton = {
                Button(
                    onClick = { 
                        currentAmount = entry.amountMl // Reset to original
                        entryToEdit = null 
                    }, 
                    colors = ButtonDefaults.secondaryButtonColors()
                ) { 
                    Text("Abbr") 
                }
            }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minus Button (links)
                    Button(
                        onClick = { 
                            currentAmount = (currentAmount - 50).coerceAtLeast(0)
                        },
                        colors = ButtonDefaults.secondaryButtonColors(),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    
                    // Menge in der Mitte
                    Text(
                        text = "$currentAmount ml",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    // Plus Button (rechts)
                    Button(
                        onClick = { 
                            currentAmount = (currentAmount + 50).coerceAtMost(2000)
                        },
                        colors = ButtonDefaults.secondaryButtonColors(),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
                    }
                }
            }
        }
    }

    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        anchorType = ScalingLazyListAnchorType.ItemStart
    ) {
        // ... (Header and Tabs remain same) ...
        item {
            Text(
                text = "Statistik",
                style = MaterialTheme.typography.title2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                color = MaterialTheme.colors.onSurface
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CompactChip(
                    onClick = { currentView = StatsView.DAY },
                    label = { Text("Tag") },
                    colors = if (currentView == StatsView.DAY) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                CompactChip(
                    onClick = { currentView = StatsView.WEEK },
                    label = { Text("Wo") },
                    colors = if (currentView == StatsView.WEEK) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                CompactChip(
                    onClick = { currentView = StatsView.MONTH },
                    label = { Text("Mo") },
                    colors = if (currentView == StatsView.MONTH) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        when (currentView) {
            StatsView.DAY -> {
                item {
                    Text(
                        text = "Heute: ${todayTotal ?: 0} ml",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (todayEntries.isEmpty()) {
                    item { Text("Keine Einträge", style = MaterialTheme.typography.caption1) }
                } else {
                    items(todayEntries) { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val time = Instant.ofEpochMilli(entry.timestamp)
                                .atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("HH:mm"))
                            
                            Text(
                                text = "$time - ${entry.amountMl}ml",
                                modifier = Modifier.weight(1f).padding(start = 8.dp),
                                style = MaterialTheme.typography.body1
                            )
                            
                            Button(
                                onClick = { entryToEdit = entry },
                                colors = ButtonDefaults.secondaryButtonColors(),
                                modifier = Modifier.size(40.dp).padding(end = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Bearbeiten",
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Button(
                                onClick = { viewModel.deleteEntry(entry) },
                                colors = ButtonDefaults.secondaryButtonColors(),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Löschen",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
            StatsView.WEEK -> {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Letzte 7 Tage", style = MaterialTheme.typography.caption1)
                        Spacer(modifier = Modifier.height(8.dp))
                        BarChart(data = weekStats, modifier = Modifier.fillMaxWidth().height(100.dp))
                    }
                }
            }
            StatsView.MONTH -> {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Dieser Monat", style = MaterialTheme.typography.caption1)
                        Spacer(modifier = Modifier.height(8.dp))
                        BarChart(data = monthStats, modifier = Modifier.fillMaxWidth().height(100.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun BarChart(data: List<DaySum>, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colors.primary
    val textColor = MaterialTheme.colors.onSurface
    
    if (data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Keine Daten", style = MaterialTheme.typography.caption2)
        }
        return
    }

    Canvas(modifier = modifier) {
        val maxVal = data.maxOfOrNull { it.total }?.toFloat() ?: 1f
        val barWidth = size.width / (data.size * 1.5f)
        val gap = barWidth * 0.5f
        
        data.forEachIndexed { index, daySum ->
            val x = index * (barWidth + gap)
            val barHeight = (daySum.total / maxVal) * size.height
            val y = size.height - barHeight
            
            drawRect(
                color = primaryColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )
        }
    }
}
