package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AppNavigator
import com.example.ui.screens.PinLockScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.VaultViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: VaultViewModel = viewModel()
            val isDark = when (viewModel.themeState) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }

            // Capture incoming share actions (like PDFs or images from WhatsApp)
            LaunchedEffect(intent) {
                if (intent?.action == Intent.ACTION_SEND) {
                    val incomingUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    if (incomingUri != null) {
                        viewModel.addNewDocument(
                            name = "Shared Doc ${System.currentTimeMillis() / 1000}",
                            category = "Miscellaneous",
                            selectedUri = incomingUri,
                            parentFolderId = null,
                            tags = "WhatsApp, Important",
                            notes = "Imported securely via Android Share Action.",
                            useAIOcrListner = false
                        ) { success, msg ->
                            val statusMsg = if (success) {
                                "File imported and locked in local vault!"
                            } else {
                                "Failed importing: $msg"
                            }
                            Toast.makeText(this@MainActivity, statusMsg, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            MyApplicationTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Crossfade(targetState = viewModel.isAppLocked, label = "AppLockCrossfade") { locked ->
                        if (locked) {
                            PinLockScreen(viewModel = viewModel)
                        } else {
                            AppNavigator(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
