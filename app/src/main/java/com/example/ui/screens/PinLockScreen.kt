package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.VaultViewModel

@Composable
fun PinLockScreen(viewModel: VaultViewModel) {
    var accumulatedPin by remember { mutableStateOf("") }
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "Clear", "0", "⌫")

    // Automatically trigger authentication when 4 digits are typed
    LaunchedEffect(accumulatedPin) {
        if (accumulatedPin.length == 4) {
            val authorized = viewModel.authenticatePin(accumulatedPin)
            if (!authorized) {
                accumulatedPin = ""
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Secure Vault Header
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(72.dp)
                    .padding(bottom = 12.dp)
            )

            Text(
                text = "Secured Sandbox Vault",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = viewModel.pinMessage,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
            )

            // Pin Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                repeat(4) { idx ->
                    val isFilled = idx < accumulatedPin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .border(2.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                }
            }

            // Lock keypad grid - LARGER lockpad
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.width(320.dp)
            ) {
                items(keys) { key ->
                    Box(
                        modifier = Modifier
                            .size(80.dp) // Enlarged key size
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                when (key) {
                                    "Clear" -> {
                                        accumulatedPin = ""
                                    }
                                    "⌫" -> {
                                        if (accumulatedPin.isNotEmpty()) {
                                            accumulatedPin = accumulatedPin.dropLast(1)
                                        }
                                    }
                                    else -> {
                                        if (accumulatedPin.length < 4) {
                                            accumulatedPin += key
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            fontSize = 22.sp, // Larger font size for premium legibility
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick bypass tip for first-time use
            val presetPin = viewModel.preferencesManager.appLockPin
            if (presetPin.isNotEmpty()) {
                Text(
                    text = "Hint: Type '$presetPin' to unlock vault.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 32.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
