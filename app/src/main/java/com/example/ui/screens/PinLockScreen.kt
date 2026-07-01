package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.VaultViewModel

@Composable
fun PinLockScreen(viewModel: VaultViewModel) {
    var accumulatedPin by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "Clear", "0", "⌫")

    // Consume back clicks to prevent escaping the lock overlay
    BackHandler(enabled = true) {
        // Do nothing, lock is secure!
    }

    // Automatically trigger authentication when 4 digits are typed
    LaunchedEffect(accumulatedPin) {
        if (accumulatedPin.length == 4) {
            val authorized = viewModel.authenticatePin(accumulatedPin)
            if (!authorized) {
                accumulatedPin = ""
            }
        }
    }

    // Spring shake animation for incorrect PIN
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(viewModel.pinMessage) {
        if (viewModel.pinMessage.contains("Incorrect", ignoreCase = true)) {
            // High-frequency spring shake
            for (i in 0..5) {
                val target = if (i % 2 == 0) 18f else -18f
                shakeOffset.animateTo(
                    targetValue = target,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioHighBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            shakeOffset.animateTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.98f))
            .pointerInput(Unit) {} // Consume clicks so they don't fall through
            .testTag("pin_lock_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Security Shield Header
            Icon(
                Icons.Default.Security,
                contentDescription = "Security Active",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "Secure Sandbox Vault",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = viewModel.pinMessage,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (viewModel.pinMessage.contains("Incorrect", ignoreCase = true)) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(bottom = 24.dp),
                textAlign = TextAlign.Center
            )

            // Pin Indicator Dots with Shake Offset Modifier
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .graphicsLayer(translationX = shakeOffset.value)
                    .padding(bottom = 32.dp)
            ) {
                repeat(4) { idx ->
                    val isFilled = idx < accumulatedPin.length
                    val digit = if (isFilled && pinVisible) {
                        accumulatedPin[idx].toString()
                    } else {
                        ""
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                            .border(
                                2.dp,
                                if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFilled) {
                            if (pinVisible) {
                                Text(
                                    text = digit,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }

                // Show/Hide Visibility Button inside the row for convenience
                IconButton(
                    onClick = { pinVisible = !pinVisible },
                    modifier = Modifier
                        .size(36.dp)
                        .padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle PIN visibility",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Keyboard / Lock keypad grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.width(280.dp)
            ) {
                items(keys) { key ->
                    val isSpecial = key == "Clear" || key == "⌫"
                    Surface(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
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
                            }
                            .testTag("keypad_btn_$key"),
                        shape = CircleShape,
                        color = if (isSpecial) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = if (!isSpecial) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                        } else null
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (key == "⌫") {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Backspace",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = key,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSpecial) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Quick bypass tip for first-time use
            val presetPin = viewModel.preferencesManager.appLockPin
            if (presetPin.isNotEmpty()) {
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "Hint: Type '$presetPin' to unlock vault.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
