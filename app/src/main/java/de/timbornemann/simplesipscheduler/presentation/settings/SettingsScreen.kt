package de.timbornemann.simplesipscheduler.presentation.settings

import androidx.compose.foundation.layout.Arrangement
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
            Stepper(
                value = target.toFloat(),
                onValueChange = { viewModel.updateDailyTarget(it.toInt()) },
                valueRange = 500f..5000f,
                steps = 18, 
                increaseIcon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Increase") },
                decreaseIcon = { Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease") }
            ) {
               Text(text = "$target ml", style = MaterialTheme.typography.body1, color = MaterialTheme.colors.primary)
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
            item {
                Text("Intervall: $reminderInterval min", style = MaterialTheme.typography.caption2)
            }
            item {
                 Stepper(
                    value = reminderInterval.toFloat(),
                    onValueChange = { viewModel.setReminderInterval(it.toInt()) },
                    valueRange = 30f..240f,
                    steps = 6, // 30, 60, 90, 120... 
                    increaseIcon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Increase") },
                    decreaseIcon = { Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease") }
                ) {
                   Text(text = "${reminderInterval}m", style = MaterialTheme.typography.body1)
                }
            }
            item {
                Text("Ruhezeit: $quietHoursStart - $quietHoursEnd Uhr", style = MaterialTheme.typography.caption2)
            }
            item {
                // Simplified Quiet Hours: Start Time
                Stepper(
                    value = quietHoursStart.toFloat(),
                    onValueChange = { viewModel.setQuietHours(it.toInt(), quietHoursEnd) },
                    valueRange = 0f..23f,
                    steps = 22,
                    increaseIcon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Increase") },
                    decreaseIcon = { Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease") }
                ) {
                   Text(text = "Start: $quietHoursStart", style = MaterialTheme.typography.body1)
                }
            }
            item {
                 Stepper(
                    value = quietHoursEnd.toFloat(),
                    onValueChange = { viewModel.setQuietHours(quietHoursStart, it.toInt()) },
                    valueRange = 0f..23f,
                    steps = 22,
                    increaseIcon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Increase") },
                    decreaseIcon = { Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease") }
                ) {
                   Text(text = "Ende: $quietHoursEnd", style = MaterialTheme.typography.body1)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    
                    // Zahl in der Mitte
                    Text(
                        text = "$amount ml",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    // Plus Button (rechts, aber vor dem Löschen-Button)
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
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
                    }
                    
                    // Löschen Button (ganz rechts)
                    Button(
                        onClick = { 
                            val newConfig = buttonConfig.toMutableList()
                            val removeIndex = buttonConfig.indexOf(amount)
                            if (removeIndex >= 0 && removeIndex < newConfig.size) {
                                newConfig.removeAt(removeIndex)
                                viewModel.updateButtonConfig(newConfig)
                            }
                        },
                        colors = ButtonDefaults.secondaryButtonColors(),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove")
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
