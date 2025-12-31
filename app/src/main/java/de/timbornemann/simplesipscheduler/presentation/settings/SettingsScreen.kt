package de.timbornemann.simplesipscheduler.presentation.settings

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Delete
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.items
import androidx.wear.compose.material.ScalingLazyListAnchorType

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Stepper
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import de.timbornemann.simplesipscheduler.presentation.MainViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val target by viewModel.dailyTarget.collectAsState()
    val reminderEnabled by viewModel.reminderEnabled.collectAsState()
    val reminderInterval by viewModel.reminderInterval.collectAsState()
    val quietHoursStart by viewModel.quietHoursStart.collectAsState()
    val quietHoursEnd by viewModel.quietHoursEnd.collectAsState()
    val reminderMode by viewModel.reminderMode.collectAsState()
    val buttonConfig by viewModel.buttonConfig.collectAsState()
    
    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        anchorType = ScalingLazyListAnchorType.ItemStart
    ) {
        item {
            Text(
                text = "Einstellungen",
                style = MaterialTheme.typography.title2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colors.onSurface
            )
        }

        // Daily Target
        item {
            Text(
                text = "Tagesziel",
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.secondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minus Button (links)
                Button(
                    onClick = { 
                        val newTarget = (target - 100).coerceAtLeast(500)
                        viewModel.updateDailyTarget(newTarget)
                    },
                    colors = ButtonDefaults.secondaryButtonColors(),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                }
                
                // Tagesziel in der Mitte
                Text(
                    text = "$target ml",
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                // Plus Button (rechts)
                Button(
                    onClick = { 
                        val newTarget = (target + 100).coerceAtMost(5000)
                        viewModel.updateDailyTarget(newTarget)
                    },
                    colors = ButtonDefaults.secondaryButtonColors(),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
                }
            }
        }
        
        // Reminder Settings
        item {
            Text("Erinnerungen", style = MaterialTheme.typography.caption1, color = MaterialTheme.colors.secondary, modifier = Modifier.padding(top = 8.dp))
        }
        item {
            ToggleChip(
                checked = reminderEnabled,
                onCheckedChange = { viewModel.toggleReminder(it) },
                label = { Text("Aktiviert") },
                toggleControl = {
                     androidx.wear.compose.material.Switch(
                         checked = reminderEnabled,
                         onCheckedChange = null
                     )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (reminderEnabled) {
            // Reminder Mode
            item {
                Text("Modus", style = MaterialTheme.typography.caption2, modifier = Modifier.padding(top = 8.dp))
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CompactChip(
                        onClick = { viewModel.setReminderMode(de.timbornemann.simplesipscheduler.data.repository.ReminderMode.ALWAYS) },
                        label = { Text("Immer") },
                        colors = if (reminderMode == de.timbornemann.simplesipscheduler.data.repository.ReminderMode.ALWAYS) 
                            ChipDefaults.primaryChipColors() 
                        else 
                            ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    CompactChip(
                        onClick = { viewModel.setReminderMode(de.timbornemann.simplesipscheduler.data.repository.ReminderMode.ONLY_UNDER_TARGET) },
                        label = { Text("Nur < Ziel") },
                        colors = if (reminderMode == de.timbornemann.simplesipscheduler.data.repository.ReminderMode.ONLY_UNDER_TARGET) 
                            ChipDefaults.primaryChipColors() 
                        else 
                            ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
            
            // Intervall Einstellung
            item {
                Text("Intervall", style = MaterialTheme.typography.caption2, modifier = Modifier.padding(top = 8.dp))
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minus Button (links)
                    Button(
                        onClick = { 
                            val newInterval = (reminderInterval - 30).coerceAtLeast(30)
                            viewModel.setReminderInterval(newInterval)
                        },
                        colors = ButtonDefaults.secondaryButtonColors(),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    
                    // Intervall in der Mitte
                    Text(
                        text = "$reminderInterval min",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    // Plus Button (rechts)
                    Button(
                        onClick = { 
                            val newInterval = (reminderInterval + 30).coerceAtMost(240)
                            viewModel.setReminderInterval(newInterval)
                        },
                        colors = ButtonDefaults.secondaryButtonColors(),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
                    }
                }
            }
            
            // Ruhezeit Einstellung
            item {
                Text("Ruhezeit", style = MaterialTheme.typography.caption2, modifier = Modifier.padding(top = 8.dp))
            }
            item {
                Text(
                    text = "$quietHoursStart - $quietHoursEnd Uhr",
                    style = MaterialTheme.typography.caption2,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Start Zeit
            item {
                Text("Start", style = MaterialTheme.typography.caption2)
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minus Button (links)
                    Button(
                        onClick = { 
                            val newStart = (quietHoursStart - 1).let { if (it < 0) 23 else it }
                            viewModel.setQuietHours(newStart, quietHoursEnd)
                        },
                        colors = ButtonDefaults.secondaryButtonColors(),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    
                    // Start Zeit in der Mitte
                    Text(
                        text = "${quietHoursStart} Uhr",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    // Plus Button (rechts)
                    Button(
                        onClick = { 
                            val newStart = (quietHoursStart + 1) % 24
                            viewModel.setQuietHours(newStart, quietHoursEnd)
                        },
                        colors = ButtonDefaults.secondaryButtonColors(),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
                    }
                }
            }
            
            // Ende Zeit
            item {
                Text("Ende", style = MaterialTheme.typography.caption2, modifier = Modifier.padding(top = 8.dp))
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minus Button (links)
                    Button(
                        onClick = { 
                            val newEnd = (quietHoursEnd - 1).let { if (it < 0) 23 else it }
                            viewModel.setQuietHours(quietHoursStart, newEnd)
                        },
                        colors = ButtonDefaults.secondaryButtonColors(),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    
                    // Ende Zeit in der Mitte
                    Text(
                        text = "${quietHoursEnd} Uhr",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    // Plus Button (rechts)
                    Button(
                        onClick = { 
                            val newEnd = (quietHoursEnd + 1) % 24
                            viewModel.setQuietHours(quietHoursStart, newEnd)
                        },
                        colors = ButtonDefaults.secondaryButtonColors(),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
                    }
                }
            }
        }

        // Button Config
        item {
            Text("Buttons", style = MaterialTheme.typography.caption1, color = MaterialTheme.colors.secondary, modifier = Modifier.padding(top = 8.dp))
        }
        
        items(buttonConfig) { amount ->
            val index = buttonConfig.indexOf(amount)
            if (index >= 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(amount) {
                            detectTapGestures(
                                onLongPress = {
                                    // Long press detected - delete button
                                    val newConfig = buttonConfig.toMutableList()
                                    val removeIndex = buttonConfig.indexOf(amount)
                                    if (removeIndex >= 0 && removeIndex < newConfig.size) {
                                        newConfig.removeAt(removeIndex)
                                        viewModel.updateButtonConfig(newConfig)
                                    }
                                }
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Minus Button (links)
                        Button(
                            onClick = { 
                                val newConfig = buttonConfig.toMutableList()
                                val currentIndex = buttonConfig.indexOf(amount)
                                if (currentIndex >= 0 && currentIndex < newConfig.size) {
                                    val newAmount = (newConfig[currentIndex] - 50).coerceAtLeast(50)
                                    newConfig[currentIndex] = newAmount
                                    viewModel.updateButtonConfig(newConfig)
                                }
                            },
                            colors = ButtonDefaults.secondaryButtonColors(),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        
                        // Zahl in der Mitte
                        Text(
                            text = "$amount ml",
                            style = MaterialTheme.typography.body1,
                            color = MaterialTheme.colors.primary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        // Plus Button (rechts)
                        Button(
                            onClick = { 
                                val newConfig = buttonConfig.toMutableList()
                                val currentIndex = buttonConfig.indexOf(amount)
                                if (currentIndex >= 0 && currentIndex < newConfig.size) {
                                    val newAmount = (newConfig[currentIndex] + 50).coerceAtMost(1000)
                                    newConfig[currentIndex] = newAmount
                                    viewModel.updateButtonConfig(newConfig)
                                }
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
        item {
            Chip(
                label = { Text("Button hinzufügen") },
                onClick = {
                    val newConfig = buttonConfig.toMutableList()
                    newConfig.add(250) // Add default 250ml
                    viewModel.updateButtonConfig(newConfig)
                },
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Add") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
