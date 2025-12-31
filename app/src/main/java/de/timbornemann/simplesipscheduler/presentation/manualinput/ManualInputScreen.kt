package de.timbornemann.simplesipscheduler.presentation.manualinput

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Alert
import de.timbornemann.simplesipscheduler.presentation.MainViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove

@Composable
fun ManualInputScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    var selectedAmount by remember { mutableStateOf(250) }
    val minAmount = 50
    val maxAmount = 2000
    val step = 50

    Alert(
        title = { Text("Menge eingeben") },
        positiveButton = {
            Button(
                onClick = {
                    viewModel.addDrink(selectedAmount)
                    onDismiss()
                },
                colors = ButtonDefaults.primaryButtonColors()
            ) {
                Text("Hinzufügen")
            }
        },
        negativeButton = {
            Button(
                onClick = { onDismiss() },
                colors = ButtonDefaults.secondaryButtonColors()
            ) {
                Text("Abbrechen")
            }
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            // Display current amount
            Text(
                text = "$selectedAmount ml",
                style = MaterialTheme.typography.display2,
                color = MaterialTheme.colors.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Plus/Minus Buttons for amount selection
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minus Button (links)
                Button(
                    onClick = { 
                        selectedAmount = (selectedAmount - step).coerceAtLeast(minAmount)
                    },
                    colors = ButtonDefaults.secondaryButtonColors(),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                }
                
                // Plus Button (rechts)
                Button(
                    onClick = { 
                        selectedAmount = (selectedAmount + step).coerceAtMost(maxAmount)
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

