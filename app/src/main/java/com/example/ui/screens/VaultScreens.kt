package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.focus.onFocusChanged
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.outlined.PushPin
import com.example.R
import androidx.compose.ui.res.painterResource
import com.example.data.CategoryEntity
import com.example.data.DocumentEntity
import com.example.data.StorageSaveStatus
import com.example.ui.theme.VerifiedGreen
import com.example.ui.theme.SyncingBlue
import com.example.ui.viewmodel.VaultViewModel
import com.example.ui.viewmodel.SearchScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// Top Navigation state
enum class VaultTab {
    HOME, DOCUMENTS, SCAN, TOOLS, FOLDERS, SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigator(viewModel: VaultViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(VaultTab.HOME) }
    var selectedPreviewDoc by remember { mutableStateOf<DocumentEntity?>(null) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var showBulkImportDialog by remember { mutableStateOf(false) }

    // Navigation controller wrap
    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_navigation")
            ) {
                NavigationBarItem(
                    selected = currentTab == VaultTab.HOME,
                    onClick = { currentTab = VaultTab.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = currentTab == VaultTab.DOCUMENTS,
                    onClick = { currentTab = VaultTab.DOCUMENTS },
                    icon = { Icon(Icons.Default.Description, contentDescription = "Documents") },
                    label = { Text("Documents") },
                    modifier = Modifier.testTag("nav_documents")
                )
                NavigationBarItem(
                    selected = currentTab == VaultTab.SCAN,
                    onClick = { currentTab = VaultTab.SCAN },
                    icon = { Icon(Icons.Default.PhotoCamera, contentDescription = "Scan") },
                    label = { Text("Scan") },
                    modifier = Modifier.testTag("nav_scan")
                )
                NavigationBarItem(
                    selected = currentTab == VaultTab.TOOLS,
                    onClick = { currentTab = VaultTab.TOOLS },
                    icon = { Icon(Icons.Default.Build, contentDescription = "Tools") },
                    label = { Text("Tools") },
                    modifier = Modifier.testTag("nav_tools")
                )
                NavigationBarItem(
                    selected = currentTab == VaultTab.FOLDERS,
                    onClick = { currentTab = VaultTab.FOLDERS },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Folders") },
                    label = { Text("Folders") },
                    modifier = Modifier.testTag("nav_folders")
                )
                NavigationBarItem(
                    selected = currentTab == VaultTab.SETTINGS,
                    onClick = { currentTab = VaultTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        },
        floatingActionButton = {
            if (currentTab == VaultTab.HOME || currentTab == VaultTab.DOCUMENTS || currentTab == VaultTab.FOLDERS) {
                FloatingActionButton(
                    onClick = { showUploadDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .testTag("fab_add_document")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Upload Document")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen switching with smooth transition
            Crossfade(
                targetState = currentTab,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                label = "screen_transition"
            ) { targetTab ->
                when (targetTab) {
                    VaultTab.HOME -> DashboardScreen(
                        viewModel = viewModel,
                        onViewDoc = { selectedPreviewDoc = it },
                        onNavigateToDocs = { currentTab = VaultTab.DOCUMENTS },
                        onAddDocumentClick = { showUploadDialog = true },
                        onBulkImportClick = { showBulkImportDialog = true },
                        onScanClick = { currentTab = VaultTab.SCAN }
                    )
                    VaultTab.DOCUMENTS -> DocumentsScreen(
                        viewModel = viewModel,
                        onViewDoc = { selectedPreviewDoc = it },
                        onAddDocumentClick = { showUploadDialog = true },
                        onBulkImportClick = { showBulkImportDialog = true },
                        onScanClick = { currentTab = VaultTab.SCAN }
                    )
                    VaultTab.SCAN -> ScanScreen(
                        viewModel = viewModel,
                        onCompleteScan = { currentTab = VaultTab.DOCUMENTS }
                    )
                    VaultTab.TOOLS -> ToolsScreen(
                        viewModel = viewModel,
                        onOpenBulkImport = { showBulkImportDialog = true }
                    )
                    VaultTab.FOLDERS -> FoldersScreen(
                        viewModel = viewModel,
                        onViewDoc = { selectedPreviewDoc = it }
                    )
                    VaultTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }

            // Storage Save Status Indicator Pill
            val saveStatus = viewModel.storageSaveStatus
            AnimatedVisibility(
                visible = saveStatus != StorageSaveStatus.IDLE,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = when (saveStatus) {
                        StorageSaveStatus.SAVING -> MaterialTheme.colorScheme.surfaceVariant
                        StorageSaveStatus.SAVED -> Color(0xFFE8F5E9)
                        StorageSaveStatus.ERROR -> Color(0xFFFFEBEE)
                        else -> MaterialTheme.colorScheme.surface
                    },
                    contentColor = when (saveStatus) {
                        StorageSaveStatus.SAVING -> MaterialTheme.colorScheme.onSurfaceVariant
                        StorageSaveStatus.SAVED -> Color(0xFF2E7D32)
                        StorageSaveStatus.ERROR -> Color(0xFFC62828)
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    border = BorderStroke(
                        1.dp,
                        when (saveStatus) {
                            StorageSaveStatus.SAVED -> Color(0xFFA5D6A7)
                            StorageSaveStatus.ERROR -> Color(0xFFEF9A9A)
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("storage_save_indicator"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (saveStatus) {
                            StorageSaveStatus.SAVING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Saving to local storage...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            StorageSaveStatus.SAVED -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Saved successfully",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = "Saved to local storage",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            StorageSaveStatus.ERROR -> {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Save failed",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFFC62828)
                                )
                                Text(
                                    text = "Failed to save changes",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }

            // Upload prompt Dialog Overlay
            if (showUploadDialog) {
                UploadDocumentDialog(
                    viewModel = viewModel,
                    onDismiss = { showUploadDialog = false },
                    onComplete = {
                        showUploadDialog = false
                    },
                    onSwitchToBulkImport = {
                        showUploadDialog = false
                        showBulkImportDialog = true
                    }
                )
            }

            // Bulk Smart Import Dialog Overlay
            if (showBulkImportDialog) {
                BulkImportDialog(
                    viewModel = viewModel,
                    onDismiss = { showBulkImportDialog = false },
                    onComplete = {
                        showBulkImportDialog = false
                    }
                )
            }

            // Preview Sheet overlay with smooth animation transitions
            AnimatedVisibility(
                visible = selectedPreviewDoc != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                var lastNonNullDoc by remember { mutableStateOf<DocumentEntity?>(null) }
                LaunchedEffect(selectedPreviewDoc) {
                    if (selectedPreviewDoc != null) {
                        lastNonNullDoc = selectedPreviewDoc
                    }
                }
                lastNonNullDoc?.let { doc ->
                    DocumentDetailOverlay(
                        doc = doc,
                        viewModel = viewModel,
                        onOpenInAppViewer = { docToView ->
                            selectedPreviewDoc = null
                            viewModel.activeInAppViewerDoc = docToView
                        },
                        onDismiss = { selectedPreviewDoc = null }
                    )
                }
            }

            // In-App Document Viewer Dialog
            viewModel.activeInAppViewerDoc?.let { doc ->
                InAppDocumentViewerDialog(
                    doc = doc,
                    viewModel = viewModel,
                    onDismiss = { viewModel.activeInAppViewerDoc = null }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: VaultViewModel,
    onViewDoc: (DocumentEntity) -> Unit,
    onNavigateToDocs: () -> Unit,
    onAddDocumentClick: () -> Unit,
    onBulkImportClick: () -> Unit,
    onScanClick: () -> Unit
) {
    val documents by viewModel.allActiveDocuments.collectAsState()

    if (documents.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top profile Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Arkiv",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = {
                        val nextTheme = if (viewModel.themeState == "Dark") "Light" else "Dark"
                        viewModel.switchTheme(nextTheme)
                    },
                    modifier = Modifier.testTag("dashboard_empty_theme_toggle")
                ) {
                    val isDark = viewModel.themeState == "Dark"
                    Icon(
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Theme Style",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            FriendlyEmptyState(
                onAddDocumentClick = onAddDocumentClick,
                onBulkImportClick = onBulkImportClick,
                onScanClick = onScanClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        val scrollState = rememberScrollState()
        var searchQuery by remember { mutableStateOf(viewModel.globalSearchQuery) }
        LaunchedEffect(searchQuery) {
            viewModel.globalSearchQuery = searchQuery
        }

        // Aggregate statistics dynamically for Bento metrics
        val totalSize = documents.sumOf { it.fileSize }
        val totalCount = documents.size
        val favoriteCount = documents.count { it.isFavorite }
        val recentCount = documents.count { System.currentTimeMillis() - it.addedDate < 24 * 3600 * 1000 }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Top profile Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Arkiv",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = {
                        val nextTheme = if (viewModel.themeState == "Dark") "Light" else "Dark"
                        viewModel.switchTheme(nextTheme)
                    },
                    modifier = Modifier.testTag("dashboard_theme_toggle")
                ) {
                    val isDark = viewModel.themeState == "Dark"
                    Icon(
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Theme Style",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Quick Search Bar (With real-time scope filter and tag suggestions)
            RealtimeSearchInputComponent(
                viewModel = viewModel,
                placeholderText = "Quick search documents...",
                onViewDoc = onViewDoc,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .testTag("dashboard_search_bar")
            )

        if (searchQuery.isNotEmpty()) {
            Text(
                text = "Search Results",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            val filteredSearchDocs = documents.filter { doc ->
                when (viewModel.globalSearchScope) {
                    SearchScope.TITLE -> {
                        doc.name.contains(searchQuery, ignoreCase = true) ||
                                doc.fileName.contains(searchQuery, ignoreCase = true)
                    }
                    SearchScope.TAGS -> {
                        doc.tags.contains(searchQuery, ignoreCase = true)
                    }
                    SearchScope.ALL -> {
                        doc.name.contains(searchQuery, ignoreCase = true) ||
                                doc.fileName.contains(searchQuery, ignoreCase = true) ||
                                doc.ocrText.contains(searchQuery, ignoreCase = true) ||
                                doc.tags.contains(searchQuery, ignoreCase = true) ||
                                doc.notes.contains(searchQuery, ignoreCase = true) ||
                                getFileTypeDescription(doc.mimeType).contains(searchQuery, ignoreCase = true)
                    }
                    SearchScope.SEMANTIC -> {
                        val matchedIds = viewModel.semanticMatchedIds
                        matchedIds != null && doc.id in matchedIds
                    }
                }
            }
            if (filteredSearchDocs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No matching documents found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    Column {
                        filteredSearchDocs.forEachIndexed { index, doc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onViewDoc(doc) }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    DocumentThumbnail(
                                        doc = doc,
                                        size = 38.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = doc.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${getFileTypeDescription(doc.mimeType)} • ${doc.category}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.Visibility,
                                    contentDescription = "View",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (index < filteredSearchDocs.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 1. Bento Box Hero Card Saved status
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$totalCount Documents Saved",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Storage used: ${viewModel.formatFileSize(totalSize)}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // Bento Stats counters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Favorites", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$favoriteCount Items", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Recents", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("+$recentCount Added", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        // Storage Usage Analytic Visualizer
        StorageUsageVisualizer(viewModel = viewModel, documents = documents)

        // 2. Quick Access Section
        Text(
            text = "Quick Access",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Show horizontal scrollable or grid of explicitly pinned documents (max 3)
        val quickAccessItems = documents.filter { it.isPinned }.take(3)
        if (quickAccessItems.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pin Files",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Pin Your Critical Files",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Access your Aadhaar, PAN or Driving License instantly! Tap any file in the list below and select the Pin icon to lock up to 3 of your most important files here.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                quickAccessItems.forEach { doc ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onViewDoc(doc) },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DocumentThumbnail(
                                    doc = doc,
                                    size = 36.dp
                                )
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pinned",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = doc.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = VerifiedGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Verified", fontSize = 11.sp, color = VerifiedGreen)
                            }
                        }
                    }
                }
                // Fill up the row if there are fewer than 3 items to preserve visual column styling!
                if (quickAccessItems.size < 3) {
                    repeat(3 - quickAccessItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // 3. Recent Activity Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Activity",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onNavigateToDocs) {
                Text("View All")
            }
        }

        val recents = documents.sortedByDescending { it.addedDate }.take(5)
        if (recents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("No activities recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column {
                    recents.forEachIndexed { index, doc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onViewDoc(doc) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                    DocumentThumbnail(
                                        doc = doc,
                                        size = 38.dp
                                    )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = doc.fileName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${doc.category} • ${formatDateDifference(doc.addedDate)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = "View",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (index < recents.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        }
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DocumentsScreen(
    viewModel: VaultViewModel,
    onViewDoc: (DocumentEntity) -> Unit,
    onAddDocumentClick: () -> Unit,
    onBulkImportClick: () -> Unit,
    onScanClick: () -> Unit
) {
    val documents by viewModel.allActiveDocuments.collectAsState()

    if (documents.isEmpty()) {
        FriendlyEmptyState(
            onAddDocumentClick = onAddDocumentClick,
            onBulkImportClick = onBulkImportClick,
            onScanClick = onScanClick,
            modifier = Modifier.fillMaxSize().padding(16.dp)
        )
    } else {
        val categories by viewModel.allCategories.collectAsState()

        var searchQuery by remember { mutableStateOf(viewModel.globalSearchQuery) }
    LaunchedEffect(searchQuery) {
        viewModel.globalSearchQuery = searchQuery
    }
    var currentSortBy by remember { mutableStateOf("Date Added") }
    var selectedDocIds by remember { mutableStateOf(setOf<Long>()) }
    var showExportBackupDialog by remember { mutableStateOf(false) }
    var backupDocsToExport by remember { mutableStateOf<List<DocumentEntity>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var drawerMode by remember { mutableStateOf("Filter") } // "Filter" or "Manage"
    var tagToRename by remember { mutableStateOf<String?>(null) }
    var tagToDelete by remember { mutableStateOf<String?>(null) }
    var newTagNameInput by remember { mutableStateOf("") }

    // Extract dynamic active tags
    val activeTags = remember(documents) {
        documents.flatMap { doc ->
            doc.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }.distinct().sorted()
    }

    val tagCounts = remember(documents) {
        val counts = mutableMapOf<String, Int>()
        documents.forEach { doc ->
            doc.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { tag ->
                    counts[tag] = counts.getOrDefault(tag, 0) + 1
                }
        }
        counts
    }

    // Filters and tabs - Root categories
    val tabs = listOf("All") + categories.filter { it.parentId == null }.map { it.name }.distinct()

    // Query match filter logic (Filename, Tag, Category status, OCR search) - No subfolder check so we show all files!
    val filteredDocs = documents.filter { doc ->
        val matchesCategory = if (viewModel.selectedCategoryTab == "All") {
            true
        } else {
            doc.category == viewModel.selectedCategoryTab
        }

        val matchesSearch = if (searchQuery.isEmpty()) {
            true
        } else {
            when (viewModel.globalSearchScope) {
                SearchScope.TITLE -> {
                    doc.name.contains(searchQuery, ignoreCase = true) ||
                            doc.fileName.contains(searchQuery, ignoreCase = true)
                }
                SearchScope.TAGS -> {
                    doc.tags.contains(searchQuery, ignoreCase = true)
                }
                SearchScope.ALL -> {
                    val folderName = categories.find { it.id == doc.parentFolderId }?.name ?: ""
                    doc.name.contains(searchQuery, ignoreCase = true) ||
                            doc.fileName.contains(searchQuery, ignoreCase = true) ||
                            doc.ocrText.contains(searchQuery, ignoreCase = true) ||
                            doc.tags.contains(searchQuery, ignoreCase = true) ||
                            doc.notes.contains(searchQuery, ignoreCase = true) ||
                            doc.category.contains(searchQuery, ignoreCase = true) ||
                            folderName.contains(searchQuery, ignoreCase = true) ||
                            getFileTypeDescription(doc.mimeType).contains(searchQuery, ignoreCase = true)
                }
                SearchScope.SEMANTIC -> {
                    val matchedIds = viewModel.semanticMatchedIds
                    matchedIds != null && doc.id in matchedIds
                }
            }
        }

        val matchesTag = if (viewModel.selectedTagsFilter.isEmpty()) {
            true
        } else {
            viewModel.selectedTagsFilter.all { filterTag ->
                doc.tags.split(",")
                    .map { it.trim().lowercase() }
                    .contains(filterTag.lowercase())
            }
        }

        val matchesFavorite = if (viewModel.favoriteOnlyFilter) {
            doc.isFavorite
        } else {
            true
        }

        val matchesDocType = when (viewModel.selectedDocTypeFilter) {
            "PDF" -> doc.mimeType.contains("pdf", ignoreCase = true)
            "Image" -> doc.mimeType.startsWith("image/", ignoreCase = true)
            "Word" -> doc.mimeType.contains("docx", ignoreCase = true) || doc.mimeType.contains("doc", ignoreCase = true)
            "Excel" -> doc.mimeType.contains("xlsx", ignoreCase = true) || doc.mimeType.contains("xls", ignoreCase = true)
            else -> true
        }

        matchesCategory && matchesSearch && matchesTag && matchesFavorite && matchesDocType
    }

    // Apply sorting selection natively
    val sortedDocs = remember(filteredDocs, currentSortBy) {
        when (currentSortBy) {
            "Date Added" -> filteredDocs.sortedByDescending { it.addedDate }
            "Alphabetical" -> filteredDocs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            "File Type" -> filteredDocs.sortedBy { getFileTypeDescription(it.mimeType) }
            else -> filteredDocs
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerTonalElevation = 4.dp
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Label,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (drawerMode == "Filter") "Filter by Tags" else "Manage Tags",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { scope.launch { drawerState.close() } }) {
                        Icon(Icons.Default.Close, contentDescription = "Close Drawer")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isFilter = drawerMode == "Filter"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isFilter) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { drawerMode = "Filter" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Filter Mode",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isFilter) FontWeight.Bold else FontWeight.Normal,
                            color = if (isFilter) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isFilter) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { drawerMode = "Manage" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Manage Mode",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (!isFilter) FontWeight.Bold else FontWeight.Normal,
                            color = if (!isFilter) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 8.dp))
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (drawerMode == "Filter") {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        color = if (viewModel.selectedTagsFilter.isEmpty()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else Color.Transparent
                                    )
                                    .clickable {
                                        viewModel.selectedTagsFilter = emptySet()
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ClearAll,
                                    contentDescription = null,
                                    tint = if (viewModel.selectedTagsFilter.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "All Documents (No tag filter)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (viewModel.selectedTagsFilter.isEmpty()) FontWeight.Bold else FontWeight.Normal,
                                    color = if (viewModel.selectedTagsFilter.isEmpty()) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        items(activeTags) { tag ->
                            val isSelected = viewModel.selectedTagsFilter.contains(tag)
                            val count = tagCounts[tag] ?: 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else Color.Transparent
                                    )
                                    .clickable {
                                        viewModel.selectedTagsFilter = if (isSelected) {
                                            viewModel.selectedTagsFilter - tag
                                        } else {
                                            viewModel.selectedTagsFilter + tag
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Label,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = count.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = "Manage and edit tags globally. Renaming or deleting tags will update all active documents immediately.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        
                        if (activeTags.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No tags found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else {
                            items(activeTags) { tag ->
                                val count = tagCounts[tag] ?: 0
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Label,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "#$tag",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = count.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    
                                    Row {
                                        IconButton(
                                            onClick = {
                                                tagToRename = tag
                                                newTagNameInput = tag
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Rename Tag",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                tagToDelete = tag
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Tag",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
    
            // 1. Search Bar & Tag Sidebar Toggle Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RealtimeSearchInputComponent(
                    viewModel = viewModel,
                    placeholderText = "Search documents by title or tags...",
                    onViewDoc = onViewDoc,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("document_search_bar")
                )
                IconButton(
                    onClick = {
                        scope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                        .testTag("tags_sidebar_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Label,
                        contentDescription = "Open Tags Sidebar",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

        // Drag active overlay banner
        AnimatedVisibility(
            visible = viewModel.draggingDocId != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val draggedDoc = documents.find { it.id == viewModel.draggingDocId }
            draggedDoc?.let { doc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DragIndicator, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Moving document: ${doc.name}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "Tap on any category folder below to drop & move it:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(tabs.filter { it != "All" }) { tab ->
                                Button(
                                    onClick = {
                                        viewModel.moveDocumentToCategory(doc.id, tab)
                                        viewModel.draggingDocId = null
                                        Toast.makeText(context, "Successfully moved to $tab category!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.SnippetFolder, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(tab, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Categories Tabs
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs) { tab ->
                val selected = viewModel.selectedCategoryTab == tab
                FilterChip(
                    selected = selected,
                    onClick = {
                        viewModel.selectedCategoryTab = tab
                    },
                    label = { Text(tab) },
                    modifier = Modifier.testTag("tab_chip_$tab")
                )
            }
        }

        // 2.5 Dynamic tag filters
        if (activeTags.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter by Tags (Multi-select):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (viewModel.selectedTagsFilter.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.selectedTagsFilter = emptySet() },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("Clear All", fontSize = 11.sp)
                    }
                }
            }
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = viewModel.selectedTagsFilter.isEmpty(),
                        onClick = { viewModel.selectedTagsFilter = emptySet() },
                        label = { Text("All Tags") },
                        modifier = Modifier.testTag("tag_chip_all")
                    )
                }
                items(activeTags) { tag ->
                    val isSelected = viewModel.selectedTagsFilter.contains(tag)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.selectedTagsFilter = if (isSelected) {
                                viewModel.selectedTagsFilter - tag
                            } else {
                                viewModel.selectedTagsFilter + tag
                            }
                        },
                        label = { Text("#$tag") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Label,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        modifier = Modifier.testTag("tag_chip_$tag")
                    )
                }
            }
        }

        // Dynamic filters (Tags, Favorites, Expiry status, Sort Option)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Favorites chip
            FilterChip(
                selected = viewModel.favoriteOnlyFilter,
                onClick = { viewModel.favoriteOnlyFilter = !viewModel.favoriteOnlyFilter },
                label = { Text("Favorites") },
                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp)) }
            )

            // Dynamic mime type chip dropdown selector
            var showTypeMenu by remember { mutableStateOf(false) }
            Box {
                FilterChip(
                    selected = viewModel.selectedDocTypeFilter != null,
                    onClick = { showTypeMenu = true },
                    label = { Text(viewModel.selectedDocTypeFilter ?: "File Type") },
                    trailingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                DropdownMenu(expanded = showTypeMenu, onDismissRequest = { showTypeMenu = false }) {
                    val types = listOf("All Sizes", "PDF", "Image", "Word", "Excel")
                    types.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t) },
                            onClick = {
                                viewModel.selectedDocTypeFilter = if (t == "All Sizes") null else t
                                showTypeMenu = false
                            }
                        )
                    }
                }
            }

            // Interactive Sorting Options Chip
            var showSortOptionMenu by remember { mutableStateOf(false) }
            Box {
                FilterChip(
                    selected = true,
                    onClick = { showSortOptionMenu = true },
                    label = { Text("Sort: $currentSortBy") },
                    leadingIcon = { Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                DropdownMenu(expanded = showSortOptionMenu, onDismissRequest = { showSortOptionMenu = false }) {
                    val sorts = listOf("Date Added", "Alphabetical", "File Type")
                    sorts.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s) },
                            onClick = {
                                currentSortBy = s
                                showSortOptionMenu = false
                            }
                        )
                    }
                }
            }
        }

        // 3.5 Selection / Batch Action panel
        if (selectedDocIds.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selectedDocIds.size == sortedDocs.size,
                            onCheckedChange = { checked ->
                                selectedDocIds = if (checked) {
                                    sortedDocs.map { it.id }.toSet()
                                } else {
                                    emptySet()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${selectedDocIds.size} Selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Export button
                        Button(
                            onClick = {
                                backupDocsToExport = sortedDocs.filter { selectedDocIds.contains(it.id) }
                                showExportBackupDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export Backup", fontSize = 11.sp)
                        }

                        // Batch Move to Trash
                        Button(
                            onClick = {
                                val docsToDelete = sortedDocs.filter { selectedDocIds.contains(it.id) }
                                scope.launch {
                                    docsToDelete.forEach { doc ->
                                        viewModel.moveToTrash(doc)
                                    }
                                    Toast.makeText(context, "Successfully moved ${docsToDelete.size} documents to trash.", Toast.LENGTH_SHORT).show()
                                    selectedDocIds = emptySet()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 4. Documents Grid / List list items
        if (sortedDocs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No matching documents",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "We couldn't find any documents fitting your search query or active filter tags.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            searchQuery = ""
                            viewModel.selectedCategoryTab = "All"
                            viewModel.selectedTagsFilter = emptySet()
                            viewModel.favoriteOnlyFilter = false
                            viewModel.selectedDocTypeFilter = null
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("reset_filters_button")
                    ) {
                        Text("Reset All Filters")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedDocs, key = { it.id }) { doc ->
                    val isSelected = selectedDocIds.contains(doc.id)
                    var dragOffset by remember { mutableStateOf(Offset.Zero) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                            .pointerInput(doc.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        viewModel.draggingDocId = doc.id
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount
                                    },
                                    onDragEnd = {
                                        val dragY = dragOffset.y
                                        val itemHeightPx = 80.dp.toPx()
                                        if (Math.abs(dragY) > itemHeightPx / 2) {
                                            val steps = (dragY / itemHeightPx).roundToInt()
                                            val currentIndex = sortedDocs.indexOf(doc)
                                            val targetIndex = (currentIndex + steps).coerceIn(0, sortedDocs.lastIndex)
                                            if (targetIndex != currentIndex) {
                                                val targetDoc = sortedDocs[targetIndex]
                                                viewModel.reorderDocuments(doc.id, targetDoc.id)
                                                Toast.makeText(context, "Documents Reordered", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        viewModel.draggingDocId = null
                                        dragOffset = Offset.Zero
                                    },
                                    onDragCancel = {
                                        viewModel.draggingDocId = null
                                        dragOffset = Offset.Zero
                                    }
                                )
                            }
                            .combinedClickable(
                                onClick = {
                                    if (selectedDocIds.isNotEmpty()) {
                                        selectedDocIds = if (isSelected) {
                                            selectedDocIds - doc.id
                                        } else {
                                            selectedDocIds + doc.id
                                        }
                                    } else {
                                        onViewDoc(doc)
                                    }
                                },
                                onLongClick = {
                                    selectedDocIds = if (isSelected) {
                                        selectedDocIds - doc.id
                                    } else {
                                        selectedDocIds + doc.id
                                    }
                                }
                            )
                            .testTag("document_card_${doc.id}"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Drag Handle",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                DocumentThumbnail(
                                    doc = doc,
                                    modifier = Modifier.testTag("document_thumbnail_${doc.id}")
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = doc.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Category: ${doc.category} • Modified: ${formatDate(doc.addedDate)} • ${viewModel.formatFileSize(doc.fileSize)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (doc.tags.isNotEmpty()) {
                                        Text(
                                            text = "Tags: ${doc.tags}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Light,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { onViewDoc(doc) }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Actions")
                            }
                        }
                    }
                }
            }
        }
    }
    }

    if (showExportBackupDialog && backupDocsToExport.isNotEmpty()) {
        ExportBackupDialog(
            documents = backupDocsToExport,
            onDismiss = {
                showExportBackupDialog = false
                backupDocsToExport = emptyList()
            },
            onExport = { password, exportAsJson ->
                showExportBackupDialog = false
                viewModel.exportSelectedDocumentsBackup(backupDocsToExport, password, exportAsJson) { success, result ->
                    if (success) {
                        Toast.makeText(context, "Backup exported successfully: $result", Toast.LENGTH_LONG).show()
                        selectedDocIds = emptySet()
                    } else {
                        Toast.makeText(context, "Backup failed: $result", Toast.LENGTH_LONG).show()
                    }
                    backupDocsToExport = emptyList()
                }
            }
        )
    }

    // Tag Rename Dialog
    tagToRename?.let { oldTag ->
        AlertDialog(
            onDismissRequest = { tagToRename = null },
            title = { Text("Rename Tag: #$oldTag") },
            text = {
                Column {
                    Text(
                        text = "This will rename '#$oldTag' to the new name across all associated documents.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = newTagNameInput,
                        onValueChange = { newTagNameInput = it },
                        label = { Text("Tag Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newTagNameInput.trim()
                        if (trimmed.isNotEmpty() && !trimmed.equals(oldTag, ignoreCase = true)) {
                            viewModel.renameTag(oldTag, trimmed)
                            Toast.makeText(context, "Tag renamed successfully!", Toast.LENGTH_SHORT).show()
                        }
                        tagToRename = null
                    },
                    enabled = newTagNameInput.trim().isNotEmpty()
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Tag Delete Dialog
    tagToDelete?.let { tag ->
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text("Delete Tag: #$tag") },
            text = {
                Text(
                    text = "Are you sure you want to delete tag '#$tag' from all associated documents? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTag(tag)
                        Toast.makeText(context, "Tag deleted successfully!", Toast.LENGTH_SHORT).show()
                        tagToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    }
}

@Composable
fun LazyHorizontalGridOfFolders(
    folders: List<CategoryEntity>,
    onClick: (Long) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(folders) { folder ->
            ElevatedCard(
                onClick = { onClick(folder.id) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .width(130.dp)
                    .height(68.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = folder.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FoldersScreen(
    viewModel: VaultViewModel,
    onViewDoc: (DocumentEntity) -> Unit
) {
    val categories by viewModel.allCategories.collectAsState()
    val documents by viewModel.allActiveDocuments.collectAsState()

    var searchQuery by remember { mutableStateOf(viewModel.globalSearchQuery) }
    LaunchedEffect(searchQuery) {
        viewModel.globalSearchQuery = searchQuery
    }
    var showCustomCategoryDialog by remember { mutableStateOf(false) }
    var showManageCategoryDialog by remember { mutableStateOf(false) }
    var selectedDocIds by remember { mutableStateOf(setOf<Long>()) }
    var showExportBackupDialog by remember { mutableStateOf(false) }
    var backupDocsToExport by remember { mutableStateOf<List<DocumentEntity>>(emptyList()) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val currentSubfolderId = viewModel.currentCategoryFolderId
    val currentFolder = categories.find { it.id == currentSubfolderId }

    // Folder history path for Breadcrumbs traversal
    val folderHistory = remember(currentSubfolderId, categories) {
        val history = mutableListOf<CategoryEntity>()
        var curr = categories.find { it.id == currentSubfolderId }
        while (curr != null) {
            history.add(0, curr)
            curr = categories.find { it.id == curr.parentId }
        }
        history
    }

    // Load folders of current level: root-level folders if null, else subdirectories
    val displayFolders = remember(currentSubfolderId, categories, searchQuery) {
        val rawList = categories.filter { it.parentId == currentSubfolderId }
        if (searchQuery.isNotEmpty()) {
            rawList.filter { it.name.contains(searchQuery, ignoreCase = true) }
        } else {
            rawList
        }
    }

    // Load files belonging to current directory
    val folderDocs = remember(currentSubfolderId, categories, documents) {
        if (currentSubfolderId != null) {
            val rootCat = categories.find { it.id == currentSubfolderId && it.parentId == null }
            if (rootCat != null) {
                // If it's a primary category root folder, map files by main category Name, or by exact subfolder ID
                documents.filter { it.parentFolderId == currentSubfolderId || (it.category.equals(rootCat.name, ignoreCase = true) && it.parentFolderId == null) }
            } else {
                documents.filter { it.parentFolderId == currentSubfolderId }
            }
        } else {
            emptyList()
        }
    }

    // Filter documents inside directories by search
    val filteredFolderDocs = remember(folderDocs, searchQuery, viewModel.globalSearchScope, viewModel.semanticMatchedIds) {
        if (searchQuery.isEmpty()) {
            folderDocs
        } else {
            folderDocs.filter { doc ->
                when (viewModel.globalSearchScope) {
                    SearchScope.TITLE -> {
                        doc.name.contains(searchQuery, ignoreCase = true) ||
                                doc.fileName.contains(searchQuery, ignoreCase = true)
                    }
                    SearchScope.TAGS -> {
                        doc.tags.contains(searchQuery, ignoreCase = true)
                    }
                    SearchScope.ALL -> {
                        doc.name.contains(searchQuery, ignoreCase = true) ||
                                doc.fileName.contains(searchQuery, ignoreCase = true) ||
                                doc.ocrText.contains(searchQuery, ignoreCase = true) ||
                                doc.tags.contains(searchQuery, ignoreCase = true) ||
                                getFileTypeDescription(doc.mimeType).contains(searchQuery, ignoreCase = true)
                    }
                    SearchScope.SEMANTIC -> {
                        val matchedIds = viewModel.semanticMatchedIds
                        matchedIds != null && doc.id in matchedIds
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 1. Directory Search Input
        RealtimeSearchInputComponent(
            viewModel = viewModel,
            placeholderText = if (currentSubfolderId == null) "Search folders..." else "Search files in this folder...",
            onViewDoc = onViewDoc,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("folder_search_bar")
        )

        // Drag active overlay banner
        AnimatedVisibility(
            visible = viewModel.draggingDocId != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val draggedDoc = documents.find { it.id == viewModel.draggingDocId }
            draggedDoc?.let { doc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DragIndicator, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Moving document: ${doc.name}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "Tap on any folder directory below to move it there:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val activeCategories = categories.filter { it.id != currentSubfolderId }
                            items(activeCategories) { folder ->
                                Button(
                                    onClick = {
                                        viewModel.moveDocumentToFolderAndCategory(doc.id, folder.name, folder.id)
                                        viewModel.draggingDocId = null
                                        Toast.makeText(context, "Successfully moved to folder: ${folder.name}!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.SnippetFolder, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(folder.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Traversal Breadcrumbs Navigation Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.currentCategoryFolderId = null },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FolderSpecial,
                    contentDescription = "Root Directories",
                    tint = if (currentSubfolderId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "Root",
                fontSize = 13.sp,
                color = if (currentSubfolderId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { viewModel.currentCategoryFolderId = null }
            )

            folderHistory.forEachIndexed { idx, f ->
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                Text(
                    f.name,
                    fontSize = 13.sp,
                    fontWeight = if (idx == folderHistory.lastIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (idx == folderHistory.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable { viewModel.currentCategoryFolderId = f.id }
                )
            }
        }

        // Folder actions banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (currentFolder == null) "Root Folders" else "${currentFolder.name} Directories",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Manage folder settings
                OutlinedButton(
                    onClick = { showManageCategoryDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Manage Directories", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manage", fontSize = 11.sp)
                }

                // Add sub-folder button
                ElevatedButton(
                    onClick = { showCustomCategoryDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New folder", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Folder", fontSize = 11.sp, maxLines = 1)
                }
            }
        }

        // Display folders in grid if present
        if (displayFolders.isNotEmpty()) {
            Text("Directories", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayFolders) { folder ->
                    val docsInThisFolder = documents.count { it.parentFolderId == folder.id && !it.isTrash }
                    ElevatedCard(
                        onClick = { viewModel.currentCategoryFolderId = folder.id },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .width(140.dp)
                            .height(72.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = folder.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$docsInThisFolder files",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Interactive Documents Inside Selected Folder list
        if (currentSubfolderId == null) {
            // Advise traversing
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Default.SnippetFolder,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Click any directory above to browse its secure documents and nested subfolders.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Inside a directory: show files!
            Text(
                "Documents in Directory",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Batch action panel inside Folder
            if (selectedDocIds.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedDocIds.size == filteredFolderDocs.size,
                                onCheckedChange = { checked ->
                                    selectedDocIds = if (checked) {
                                        filteredFolderDocs.map { it.id }.toSet()
                                    } else {
                                        emptySet()
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${selectedDocIds.size} Selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Multiple File Exports
                            Button(
                                onClick = {
                                    backupDocsToExport = filteredFolderDocs.filter { selectedDocIds.contains(it.id) }
                                    showExportBackupDialog = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export Backup", fontSize = 11.sp)
                            }

                            // Multiple File Deletes
                            Button(
                                onClick = {
                                    val docsToDelete = filteredFolderDocs.filter { selectedDocIds.contains(it.id) }
                                    scope.launch {
                                        docsToDelete.forEach { doc ->
                                            viewModel.moveToTrash(doc)
                                        }
                                        Toast.makeText(context, "Moved ${docsToDelete.size} files to Trash.", Toast.LENGTH_SHORT).show()
                                        selectedDocIds = emptySet()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            if (filteredFolderDocs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SnippetFolder,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("This directory has no documents stored.", fontSize = 13.sp, color = Color.Gray)
                        Text("Tapp '+' at bottom-right to upload directly here!", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredFolderDocs, key = { it.id }) { doc ->
                        val isSelected = selectedDocIds.contains(doc.id)
                        var dragOffset by remember { mutableStateOf(Offset.Zero) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                                .pointerInput(doc.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            viewModel.draggingDocId = doc.id
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount
                                        },
                                        onDragEnd = {
                                            val dragY = dragOffset.y
                                            val itemHeightPx = 80.dp.toPx()
                                            if (Math.abs(dragY) > itemHeightPx / 2) {
                                                val steps = (dragY / itemHeightPx).roundToInt()
                                                val currentIndex = filteredFolderDocs.indexOf(doc)
                                                val targetIndex = (currentIndex + steps).coerceIn(0, filteredFolderDocs.lastIndex)
                                                if (targetIndex != currentIndex) {
                                                    val targetDoc = filteredFolderDocs[targetIndex]
                                                    viewModel.reorderDocuments(doc.id, targetDoc.id)
                                                    Toast.makeText(context, "Documents Reordered", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            viewModel.draggingDocId = null
                                            dragOffset = Offset.Zero
                                        },
                                        onDragCancel = {
                                            viewModel.draggingDocId = null
                                            dragOffset = Offset.Zero
                                        }
                                    )
                                }
                                .combinedClickable(
                                    onClick = {
                                        if (selectedDocIds.isNotEmpty()) {
                                            selectedDocIds = if (isSelected) {
                                                selectedDocIds - doc.id
                                            } else {
                                                selectedDocIds + doc.id
                                            }
                                        } else {
                                            onViewDoc(doc)
                                        }
                                    },
                                    onLongClick = {
                                        selectedDocIds = if (isSelected) {
                                            selectedDocIds - doc.id
                                        } else {
                                            selectedDocIds + doc.id
                                        }
                                    }
                                )
                                .testTag("document_card_${doc.id}"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Drag Handle",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    DocumentThumbnail(
                                        doc = doc,
                                        size = 40.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = doc.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Modified: ${formatDate(doc.addedDate)} • ${viewModel.formatFileSize(doc.fileSize)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Quick direct In-App eye visual preview button
                                    IconButton(
                                        onClick = { viewModel.activeInAppViewerDoc = doc },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = "Direct In-App Preview",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onViewDoc(doc) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Edit Actions", modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Overlay folder creation dialog
        if (showCustomCategoryDialog) {
            CustomFolderDialog(
                parentId = currentSubfolderId,
                viewModel = viewModel,
                onDismiss = { showCustomCategoryDialog = false }
            )
        }

        // Overlay folder management dialog
        if (showManageCategoryDialog) {
            CategoryManagementDialog(
                viewModel = viewModel,
                onDismiss = { showManageCategoryDialog = false }
            )
        }

        if (showExportBackupDialog && backupDocsToExport.isNotEmpty()) {
            ExportBackupDialog(
                documents = backupDocsToExport,
                onDismiss = {
                    showExportBackupDialog = false
                    backupDocsToExport = emptyList()
                },
                onExport = { password, exportAsJson ->
                    showExportBackupDialog = false
                    viewModel.exportSelectedDocumentsBackup(backupDocsToExport, password, exportAsJson) { success, result ->
                        if (success) {
                            Toast.makeText(context, "Backup exported successfully: $result", Toast.LENGTH_LONG).show()
                            selectedDocIds = emptySet()
                        } else {
                            Toast.makeText(context, "Backup failed: $result", Toast.LENGTH_LONG).show()
                        }
                        backupDocsToExport = emptyList()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppDocumentViewerDialog(
    doc: DocumentEntity,
    viewModel: VaultViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activeFilter by remember { mutableStateOf("Default") }
    var scale by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }
    var textFontSize by remember { mutableStateOf(13) }
    var searchTextQuery by remember { mutableStateOf(viewModel.globalSearchQuery) }
    var currentPageIndex by remember { mutableStateOf(0) }

    val file = remember(doc) { com.example.utils.AESCryptUtils.getDecryptedFile(context, doc.localUri) }

    val isRealPdf = remember(file) {
        try {
            if (file.exists() && file.length() >= 4) {
                val bytes = ByteArray(4)
                file.inputStream().use { it.read(bytes) }
                val header = String(bytes)
                header.startsWith("%PDF")
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    val isRealImg = remember(file) {
        try {
            if (file.exists()) {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(file.absolutePath, options)
                options.outWidth > 0 && options.outHeight > 0
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    val pdfRenderer = remember(file, isRealPdf) {
        if (isRealPdf) {
            try {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                PdfRenderer(pfd)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    DisposableEffect(pdfRenderer) {
        onDispose {
            try {
                pdfRenderer?.close()
            } catch (e: Exception) {
                // Ignore
            }
            com.example.utils.AESCryptUtils.clearDecryptedCache(context)
        }
    }

    // Read plaintext / code document content securely from internal files
    val fileTextContent = remember(doc) {
        try {
            if (file.exists() && !isRealPdf && !isRealImg && !doc.mimeType.contains("docx", ignoreCase = true) && !doc.mimeType.contains("excel", ignoreCase = true) && !doc.mimeType.contains("spreadsheetml", ignoreCase = true)) {
                file.readText()
            } else {
                doc.ocrText
            }
        } catch (e: Exception) {
            doc.ocrText
        }
    }

    // Split text into simulated multipage pages of 480 chars to simulate pdf reader visually inside app
    val pageSize = 480
    val simulatedPages = remember(fileTextContent) {
        val cleanText = fileTextContent.ifEmpty { doc.ocrText.ifEmpty { "No indexed text or OCR content available for this document." } }
        val list = mutableListOf<String>()
        var start = 0
        while (start < cleanText.length) {
            val end = minOf(cleanText.length, start + pageSize)
            list.add(cleanText.substring(start, end))
            start += pageSize
        }
        if (list.isEmpty()) list.add("Empty index/text details.")
        list
    }

    val pageCount = remember(pdfRenderer, simulatedPages) {
        pdfRenderer?.pageCount ?: simulatedPages.size
    }

    // Ensure we don't index Out of Bounds if we toggle between docs or types
    LaunchedEffect(pageCount) {
        if (currentPageIndex >= pageCount) {
            currentPageIndex = maxOf(0, pageCount - 1)
        }
    }

    val pdfPageBitmap = remember(pdfRenderer, currentPageIndex) {
        pdfRenderer?.let { renderer ->
            try {
                if (currentPageIndex in 0 until renderer.pageCount) {
                    renderer.openPage(currentPageIndex).use { page ->
                        // Render with high resolution (e.g. width * 2, height * 2)
                        val width = page.width * 2
                        val height = page.height * 2
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        
                        val canvas = android.graphics.Canvas(bitmap)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Polished dynamic header bar
                Surface(
                    tonalElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Close Preview")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = doc.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Category: ${doc.category} • Size: ${viewModel.formatFileSize(doc.fileSize)} • Type: ${getFileTypeDescription(doc.mimeType)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isPinned = doc.isPinned
                            val isFavorite = doc.isFavorite
                            IconButton(onClick = { viewModel.togglePin(doc) }) {
                                Icon(
                                    imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = "Pin Status Toggle",
                                    tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { viewModel.toggleFavorite(doc) }) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Favorite Status Toggle",
                                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // High fidelity file rendering canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    if (doc.mimeType.startsWith("image/", ignoreCase = true) || isRealImg) {
                        if (isRealImg) {
                            // Image Scanner filters setup
                            val grayscaleMatrix = ColorMatrix().apply { setToSaturation(0f) }
                            val scannerMatrix = ColorMatrix(floatArrayOf(
                                1.6f, 0f, 0f, 0f, -50f,
                                0f, 1.6f, 0f, 0f, -50f,
                                0f, 0f, 1.6f, 0f, -50f,
                                0f, 0f, 0f, 1.0f, 0f
                            ))
                            val invertMatrix = ColorMatrix(floatArrayOf(
                                -1f, 0f, 0f, 0f, 255f,
                                0f, -1f, 0f, 0f, 255f,
                                0f, 0f, -1f, 0f, 255f,
                                0f, 0f, 0f, 1f, 0f
                            ))

                            val customColorFilter = when (activeFilter) {
                                "Grayscale" -> ColorFilter.colorMatrix(grayscaleMatrix)
                                "Scanner Filter" -> ColorFilter.colorMatrix(scannerMatrix)
                                "Invert Scan" -> ColorFilter.colorMatrix(invertMatrix)
                                else -> null
                            }

                            Column(modifier = Modifier.fillMaxSize()) {
                                // Enhancement row options
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(vertical = 4.dp, horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val filtersList = listOf("Default", "Grayscale", "Scanner Filter", "Invert Scan")
                                    LazyRow(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(filtersList) { f ->
                                            ElevatedFilterChip(
                                                selected = activeFilter == f,
                                                onClick = { activeFilter = f },
                                                label = { Text(f, fontSize = 11.sp) }
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = { scale = maxOf(0.4f, scale - 0.2f) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                                        }
                                        IconButton(
                                            onClick = { scale += 0.2f },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Zoom In")
                                        }
                                        IconButton(
                                            onClick = { rotation = (rotation + 90f) % 360f },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.RotateRight, contentDescription = "Rotate Image")
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = file,
                                        contentDescription = "In-App SECURE PHOTO PREVIEW",
                                        colorFilter = customColorFilter,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer(
                                                scaleX = scale,
                                                scaleY = scale,
                                                rotationZ = rotation
                                            ),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        } else {
                            // Fallback for mock images (display plaintext description & OCR beautifully)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .align(Alignment.CenterHorizontally)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Simulated Digital Scan",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = fileTextContent.ifEmpty { doc.ocrText.ifEmpty { "Empty content details." } },
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else if (isRealPdf) {
                        // High-fidelity Real PDF page reader
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(vertical = 4.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { scale = maxOf(0.4f, scale - 0.2f) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                                    }
                                    IconButton(
                                        onClick = { scale += 0.2f },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Zoom In")
                                    }
                                    IconButton(
                                        onClick = { rotation = (rotation + 90f) % 360f },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate Image")
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(4.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = 8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(18.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = doc.fileName,
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "PAGE ${currentPageIndex + 1} OF $pageCount",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (pdfPageBitmap != null) {
                                                Image(
                                                    bitmap = pdfPageBitmap.asImageBitmap(),
                                                    contentDescription = "PDF Page ${currentPageIndex + 1}",
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .graphicsLayer(
                                                            scaleX = scale,
                                                            scaleY = scale,
                                                            rotationZ = rotation
                                                        ),
                                                    contentScale = ContentScale.Fit
                                                )
                                            } else {
                                                CircularProgressIndicator()
                                            }
                                        }

                                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                                                enabled = currentPageIndex > 0
                                            ) {
                                                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Prev Page", tint = if (currentPageIndex > 0) MaterialTheme.colorScheme.primary else Color.LightGray)
                                            }

                                            Text(
                                                text = "Page ${currentPageIndex + 1} / $pageCount",
                                                fontSize = 12.sp,
                                                color = Color.Gray,
                                                fontWeight = FontWeight.Bold
                                            )

                                            IconButton(
                                                onClick = { if (currentPageIndex < pageCount - 1) currentPageIndex++ },
                                                enabled = currentPageIndex < pageCount - 1
                                            ) {
                                                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next Page", tint = if (currentPageIndex < pageCount - 1) MaterialTheme.colorScheme.primary else Color.LightGray)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (doc.mimeType.contains("text", ignoreCase = true) || doc.mimeType.contains("json", ignoreCase = true) || doc.fileName.endsWith(".txt") || doc.fileName.endsWith(".json") || doc.fileName.endsWith(".csv")) {
                        // Plaintext/JSON visual document reader
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = searchTextQuery,
                                    onValueChange = { searchTextQuery = it },
                                    placeholder = { Text("Find text in document...", fontSize = 11.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    textStyle = TextStyle(fontSize = 12.sp),
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    trailingIcon = {
                                        if (searchTextQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchTextQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { textFontSize = maxOf(8, textFontSize - 1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Font Down", modifier = Modifier.size(16.dp))
                                    }
                                    Text("${textFontSize}sp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = { textFontSize = minOf(30, textFontSize + 1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Font Up", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                val highlightedSource = buildAnnotatedString {
                                    val src = fileTextContent.ifEmpty { doc.ocrText.ifEmpty { "Empty content recorded." } }
                                    if (searchTextQuery.isEmpty()) {
                                        append(src)
                                    } else {
                                        var startIdx = 0
                                        while (startIdx < src.length) {
                                            val matchIdx = src.indexOf(searchTextQuery, startIdx, ignoreCase = true)
                                            if (matchIdx == -1) {
                                                append(src.substring(startIdx))
                                                break
                                            } else {
                                                append(src.substring(startIdx, matchIdx))
                                                withStyle(style = SpanStyle(background = Color(0xFFFBC02D), color = Color.Black, fontWeight = FontWeight.Bold)) {
                                                    append(src.substring(matchIdx, matchIdx + searchTextQuery.length))
                                                }
                                                startIdx = matchIdx + searchTextQuery.length
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = highlightedSource,
                                    fontSize = textFontSize.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = (textFontSize + 6).sp
                                )
                            }
                        }
                    } else if (doc.mimeType == "application/pdf") {
                        // Simulated PDF visual reader (fallback for preloaded mock PDFs)
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = searchTextQuery,
                                    onValueChange = { searchTextQuery = it },
                                    placeholder = { Text("Find text in indexed pages...", fontSize = 11.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    textStyle = TextStyle(fontSize = 12.sp),
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    trailingIcon = {
                                        if (searchTextQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchTextQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { textFontSize = maxOf(8, textFontSize - 1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    Text("${textFontSize}sp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = { textFontSize = minOf(30, textFontSize + 1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(4.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = 8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(18.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = doc.fileName,
                                                fontSize = 10.sp,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "DIGITAL EXTRACT: PAGE ${currentPageIndex + 1} OF ${simulatedPages.size}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            val currentTextContentOfPage = simulatedPages[currentPageIndex]
                                            val annotatedExtract = buildAnnotatedString {
                                                if (searchTextQuery.isEmpty()) {
                                                    append(currentTextContentOfPage)
                                                } else {
                                                    var startIdx = 0
                                                    while (startIdx < currentTextContentOfPage.length) {
                                                        val matchIdx = currentTextContentOfPage.indexOf(searchTextQuery, startIdx, ignoreCase = true)
                                                        if (matchIdx == -1) {
                                                            append(currentTextContentOfPage.substring(startIdx))
                                                            break
                                                        } else {
                                                            append(currentTextContentOfPage.substring(startIdx, matchIdx))
                                                            withStyle(style = SpanStyle(background = Color(0xFFFBC02D), color = Color.Black, fontWeight = FontWeight.Bold)) {
                                                                append(currentTextContentOfPage.substring(matchIdx, matchIdx + searchTextQuery.length))
                                                            }
                                                            startIdx = matchIdx + searchTextQuery.length
                                                        }
                                                    }
                                                }
                                            }

                                            Text(
                                                text = annotatedExtract,
                                                fontSize = textFontSize.sp,
                                                color = Color.DarkGray,
                                                lineHeight = (textFontSize + 6).sp,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                                                enabled = currentPageIndex > 0
                                            ) {
                                                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Prev Page", tint = if (currentPageIndex > 0) MaterialTheme.colorScheme.primary else Color.LightGray)
                                            }

                                            Text(
                                                text = "Page ${currentPageIndex + 1} / ${simulatedPages.size}",
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                fontWeight = FontWeight.Bold
                                            )

                                            IconButton(
                                                onClick = { if (currentPageIndex < simulatedPages.lastIndex) currentPageIndex++ },
                                                enabled = currentPageIndex < simulatedPages.lastIndex
                                            ) {
                                                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next Page", tint = if (currentPageIndex < simulatedPages.lastIndex) MaterialTheme.colorScheme.primary else Color.LightGray)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Generic offline secure document/binary formats (Word, Excel, PPT, Zip, and original mock documents)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    DocumentThumbnail(
                                        doc = doc,
                                        size = 72.dp
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = doc.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = doc.fileName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 16.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                    )

                                    Text(
                                        text = "Secure Document Formats (.docx, .xlsx, .zip, etc.) are protected offline to ensure end-to-end sandbox privacy.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Button(
                                        onClick = { viewDocumentExternally(context, doc) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Open file in external application", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Polished actions tray at bottom
                Surface(
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Download/backup
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val res = viewModel.repository.downloadDocumentToPublicDownloads(doc)
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Exported file to Downloads folder!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Could not export: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", fontSize = 11.sp, maxLines = 1)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Export as PDF
                        OutlinedButton(
                            onClick = { exportDocumentAsPdf(context, doc) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF", fontSize = 11.sp, maxLines = 1)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Share
                        OutlinedButton(
                            onClick = { shareDocument(context, doc) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 11.sp, maxLines = 1)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Open Externally
                        Button(
                            onClick = { viewDocumentExternally(context, doc) },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ext View", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomFolderDialog(
    parentId: Long?,
    viewModel: VaultViewModel,
    onDismiss: () -> Unit
) {
    var subName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Create New Directory", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = subName,
                    onValueChange = { subName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (subName.isNotEmpty()) {
                                scope.launch {
                                    viewModel.repository.insertCategory(
                                        CategoryEntity(
                                            name = subName,
                                            parentId = parentId
                                        )
                                    )
                                    onDismiss()
                                }
                            }
                        }
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementDialog(
    viewModel: VaultViewModel,
    onDismiss: () -> Unit
) {
    val categories by viewModel.allCategories.collectAsState()
    val documents by viewModel.allActiveDocuments.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Folders, 1 = Tags
    val context = LocalContext.current

    // For Category Creation
    var newCatName by remember { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf<Long?>(null) }
    var expandParentDropdown by remember { mutableStateOf(false) }

    // For editing/renaming an existing folder/category
    var editingFolderId by remember { mutableStateOf<Long?>(null) }
    var editingFolderName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Category & Tag Manager",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text("Folders", modifier = Modifier.padding(vertical = 10.dp))
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text("Tags", modifier = Modifier.padding(vertical = 10.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Crossfade(
                    targetState = selectedTab,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "category_tag_manager_tab"
                ) { tab ->
                    if (tab == 0) {
                        // Folders/Categories management panel
                        Column {
                            Text("Create new folder:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newCatName,
                                    onValueChange = { newCatName = it },
                                    placeholder = { Text("Folder Name") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.3f)
                                )

                                // Parent folder selector
                                Box(modifier = Modifier.weight(1f)) {
                                    val parentName = categories.find { it.id == selectedParentId }?.name ?: "Root"
                                    OutlinedButton(
                                        onClick = { expandParentDropdown = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(horizontal = 6.dp)
                                    ) {
                                        Text(parentName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                    DropdownMenu(
                                        expanded = expandParentDropdown,
                                        onDismissRequest = { expandParentDropdown = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("None (Root)") },
                                            onClick = {
                                                selectedParentId = null
                                                expandParentDropdown = false
                                            }
                                        )
                                        categories.filter { it.parentId == null }.forEach { folder ->
                                            DropdownMenuItem(
                                                text = { Text(folder.name) },
                                                onClick = {
                                                    selectedParentId = folder.id
                                                    expandParentDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (newCatName.trim().isEmpty()) {
                                            Toast.makeText(context, "Set folder name first.", Toast.LENGTH_SHORT).show()
                                            return@IconButton
                                        }
                                        viewModel.createCategory(newCatName.trim(), selectedParentId)
                                        newCatName = ""
                                        Toast.makeText(context, "Folder created!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape).size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Folders list view
                            Text("Active folders & paths:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            LazyColumn(modifier = Modifier.height(200.dp)) {
                                items(categories) { folder ->
                                    val parentName = categories.find { it.id == folder.parentId }?.name
                                    val folderPath = if (parentName != null) "$parentName > ${folder.name}" else folder.name
                                    val isDefault = folder.isDefault

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        if (editingFolderId == folder.id) {
                                            OutlinedTextField(
                                                value = editingFolderName,
                                                onValueChange = { editingFolderName = it },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = {
                                                    if (editingFolderName.trim().isNotEmpty()) {
                                                        viewModel.renameCategory(folder, editingFolderName.trim())
                                                        editingFolderId = null
                                                        Toast.makeText(context, "Renamed successfully!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = "Save", tint = VerifiedGreen)
                                            }
                                            IconButton(onClick = { editingFolderId = null }) {
                                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                                            }
                                        } else {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = folderPath,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 13.sp,
                                                    color = if (isDefault) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isDefault) {
                                                    Text("Default folder (Protected)", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                                }
                                            }
                                            if (!isDefault) {
                                                IconButton(
                                                    onClick = {
                                                        editingFolderId = folder.id
                                                        editingFolderName = folder.name
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        viewModel.deleteCategory(folder)
                                                        Toast.makeText(context, "Folder deleted.", Toast.LENGTH_SHORT).show()
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Custom Tags management panel
                        val activeTags = remember(documents) {
                            documents.flatMap { doc ->
                                doc.tags.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                            }.distinct()
                        }

                        Column {
                            Text(
                                "Tap the delete icon to globally delete custom tags across all your secured documents.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            if (activeTags.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No custom tags active in current files.", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                                }
                            } else {
                                LazyColumn(modifier = Modifier.height(200.dp)) {
                                    items(activeTags) { tagName ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(tagName, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                            }
                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        documents.forEach { doc ->
                                                            val list = doc.tags.split(",")
                                                                .map { it.trim() }
                                                                .filter { it.isNotEmpty() }
                                                            if (list.any { it.equals(tagName, ignoreCase = true) }) {
                                                                val updatedTags = list.filter { !it.equals(tagName, ignoreCase = true) }
                                                                    .joinToString(", ")
                                                                viewModel.repository.updateDocument(doc.copy(tags = updatedTags))
                                                            }
                                                        }
                                                        Toast.makeText(context, "Tag '$tagName' deleted globally.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete tag", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close Manager")
                    }
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadDocumentDialog(
    viewModel: VaultViewModel,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    onSwitchToBulkImport: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var documentName by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf("Identity") }
    var customTagsInput by remember { mutableStateOf("") }
    var docNotes by remember { mutableStateOf("") }
    var expandCatDropdown by remember { mutableStateOf(false) }

    val categories by viewModel.allCategories.collectAsState()
    val documents by viewModel.allActiveDocuments.collectAsState(initial = emptyList())
    val allExistingTags = remember(documents) {
        documents.flatMap { doc ->
            doc.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }.distinct().sorted()
    }

    // Native Activity pickers for file uploading
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            val resolvedName = viewModel.getFileNameFromUri(uri) ?: uri.lastPathSegment ?: "File"
            // Set the visual name automatically (excluding extension for clean display name)
            documentName = resolvedName.substringBeforeLast(".")
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Upload Document to Local Vault",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                TextButton(
                    onClick = onSwitchToBulkImport,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Switch to Smart Bulk Import Utility", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Picker Launcher target
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { filePickerLauncher.launch("*/*") }
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedUri == null) "Choose from Gallery / File System" else "Selected: ${viewModel.getFileNameFromUri(selectedUri!!)?.take(25) ?: "Loaded File"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = documentName,
                    onValueChange = { documentName = it },
                    label = { Text("Document ID / Visual Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // AI Assist Suggestion Button
                Button(
                    onClick = {
                        if (documentName.trim().isEmpty()) {
                            Toast.makeText(context, "Please enter a name first so AI has some context.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.suggestCategoryAndTags(
                            documentName = documentName,
                            notes = docNotes,
                            selectedUri = selectedUri
                        ) { category, suggestedTags ->
                            selectedCat = category
                            customTagsInput = suggestedTags.joinToString(", ")
                            Toast.makeText(context, "AI suggested Category ($category) & Tags successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    enabled = !viewModel.isSuggesting
                ) {
                    if (viewModel.isSuggesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Suggesting...", fontSize = 13.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Suggest",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Auto-Suggest Category & Tags", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandCatDropdown = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Category: $selectedCat")
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = expandCatDropdown,
                        onDismissRequest = { expandCatDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        val nonNestedCats = categories.filter { it.parentId == null }.map { it.name }.distinct()
                        nonNestedCats.forEach { catName ->
                            DropdownMenuItem(
                                text = { Text(catName) },
                                onClick = {
                                    selectedCat = catName
                                    expandCatDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customTagsInput,
                    onValueChange = { customTagsInput = it },
                    label = { Text("Tags (comma separated, e.g. Tax, Medical)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                TagSuggestionsRow(
                    allExistingTags = allExistingTags,
                    currentTagsString = customTagsInput,
                    onTagsChanged = { customTagsInput = it }
                )

                // Interactive dynamic Tag Chips renderer
                if (customTagsInput.isNotEmpty()) {
                    val tagsList = customTagsInput.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    if (tagsList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(tagsList) { tag ->
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Label,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = tag,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove tag",
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable {
                                                    customTagsInput = tagsList.filter { it != tag }.joinToString(", ")
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = docNotes,
                    onValueChange = { docNotes = it },
                    label = { Text("Additional Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (documentName.isEmpty()) {
                                Toast.makeText(context, "Please set a name.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch {
                                viewModel.addNewDocument(
                                    name = documentName,
                                    category = selectedCat,
                                    selectedUri = selectedUri,
                                    parentFolderId = viewModel.currentCategoryFolderId,
                                    tags = customTagsInput,
                                    notes = docNotes,
                                    useAIOcrListner = false
                                ) { success, warningMsg ->
                                    if (success) {
                                        if (warningMsg != null) {
                                            Toast.makeText(context, warningMsg, Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Document Saved Successfully!", Toast.LENGTH_SHORT).show()
                                        }
                                        onComplete()
                                    } else {
                                        Toast.makeText(context, "Upload failed: $warningMsg", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Add to Vault")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailOverlay(
    doc: DocumentEntity,
    viewModel: VaultViewModel,
    onOpenInAppViewer: (DocumentEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(doc.name) }
    var editCategory by remember { mutableStateOf(doc.category) }
    var editTags by remember { mutableStateOf(doc.tags) }
    var editNotes by remember { mutableStateOf(doc.notes) }
    var expandCatEditDropdown by remember { mutableStateOf(false) }

    val categories by viewModel.allCategories.collectAsState()
    val documents by viewModel.allActiveDocuments.collectAsState(initial = emptyList())
    val allExistingTags = remember(documents) {
        documents.flatMap { doc ->
            doc.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }.distinct().sorted()
    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var nameFocused by remember { mutableStateOf(false) }
    var tagsFocused by remember { mutableStateOf(false) }
    var notesFocused by remember { mutableStateOf(false) }
    var autoSaveStatus by remember { mutableStateOf("") }

    val hasChanges = remember(editNotes, editName, editCategory, editTags, doc.notes, doc.name, doc.category, doc.tags) {
        editNotes != doc.notes || editName != doc.name || editCategory != doc.category || editTags != doc.tags
    }

    LaunchedEffect(editNotes, editName, editCategory, editTags) {
        if (isEditing && hasChanges) {
            autoSaveStatus = "Saving..."
            delay(1500)
            if (editName.isNotEmpty()) {
                viewModel.repository.updateDocument(
                    doc.copy(
                        name = editName,
                        category = editCategory,
                        tags = editTags,
                        notes = editNotes
                    )
                )
                autoSaveStatus = "Changes saved automatically"
            } else {
                autoSaveStatus = "Error: Title cannot be empty"
            }
        }
    }

    LaunchedEffect(doc.id) {
        viewModel.generateAndSaveSummaryIfNeeded(doc)
    }

    androidx.activity.compose.BackHandler(enabled = true) {
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Edit Details" else doc.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!isEditing) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Details")
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isEditing) {
                    // Editable Details block
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Document ID / Visual Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (nameFocused && !focusState.isFocused) {
                                        if (hasChanges && editName.isNotEmpty()) {
                                            autoSaveStatus = "Saving..."
                                            scope.launch {
                                                viewModel.repository.updateDocument(
                                                    doc.copy(
                                                        name = editName,
                                                        category = editCategory,
                                                        tags = editTags,
                                                        notes = editNotes
                                                    )
                                                )
                                                autoSaveStatus = "Changes saved automatically"
                                            }
                                        }
                                    }
                                    nameFocused = focusState.isFocused
                                },
                            singleLine = true
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandCatEditDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Category: $editCategory")
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.MoreVert, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = expandCatEditDropdown,
                                onDismissRequest = { expandCatEditDropdown = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val nonNestedCats = categories.filter { it.parentId == null }.map { it.name }.distinct()
                                nonNestedCats.forEach { catName ->
                                    DropdownMenuItem(
                                        text = { Text(catName) },
                                        onClick = {
                                            editCategory = catName
                                            expandCatEditDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = editTags,
                            onValueChange = { editTags = it },
                            label = { Text("Tags (comma separated)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (tagsFocused && !focusState.isFocused) {
                                        if (hasChanges && editName.isNotEmpty()) {
                                            autoSaveStatus = "Saving..."
                                            scope.launch {
                                                viewModel.repository.updateDocument(
                                                    doc.copy(
                                                        name = editName,
                                                        category = editCategory,
                                                        tags = editTags,
                                                        notes = editNotes
                                                    )
                                                )
                                                autoSaveStatus = "Changes saved automatically"
                                            }
                                        }
                                    }
                                    tagsFocused = focusState.isFocused
                                },
                            singleLine = true
                        )

                        TagSuggestionsRow(
                            allExistingTags = allExistingTags,
                            currentTagsString = editTags,
                            onTagsChanged = { editTags = it }
                        )

                        // Interactive dynamic test/edit tag chips renderer
                        if (editTags.isNotEmpty()) {
                            val tagsList = editTags.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                            if (tagsList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(tagsList) { tag ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Label,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = tag,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove tag",
                                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable {
                                                            editTags = tagsList.filter { it != tag }.joinToString(", ")
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = editNotes,
                            onValueChange = { editNotes = it },
                            label = { Text("Additional Notes (Optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (notesFocused && !focusState.isFocused) {
                                        if (hasChanges && editName.isNotEmpty()) {
                                            autoSaveStatus = "Saving..."
                                            scope.launch {
                                                viewModel.repository.updateDocument(
                                                    doc.copy(
                                                        name = editName,
                                                        category = editCategory,
                                                        tags = editTags,
                                                        notes = editNotes
                                                    )
                                                )
                                                autoSaveStatus = "Changes saved automatically"
                                            }
                                        }
                                    }
                                    notesFocused = focusState.isFocused
                                },
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (autoSaveStatus.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (autoSaveStatus == "Saving...") Icons.Default.Refresh else Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (autoSaveStatus == "Saving...") MaterialTheme.colorScheme.primary else VerifiedGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = autoSaveStatus,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (autoSaveStatus.startsWith("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isEditing = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Close Editor")
                            }
                            Button(
                                onClick = {
                                    if (editName.isEmpty()) {
                                        Toast.makeText(context, "Please set a name.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    scope.launch {
                                        viewModel.repository.updateDocument(
                                            doc.copy(
                                                name = editName,
                                                category = editCategory,
                                                tags = editTags,
                                                notes = editNotes
                                            )
                                        )
                                        isEditing = false
                                        Toast.makeText(context, "Saved Successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save & Exit")
                            }
                        }
                    }
                } else {
                    // Document Visual Preview Container - Instantly viewable, bypassed security lock
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.inverseOnSurface)
                    ) {
                        if (doc.mimeType.startsWith("image/", ignoreCase = true)) {
                            // Show the actual image securely!
                            val previewImageFile = remember(doc) { com.example.utils.AESCryptUtils.getDecryptedFile(context, doc.localUri) }
                            AsyncImage(
                                model = previewImageFile,
                                contentDescription = "Visual Document Preview",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        viewDocumentExternally(context, doc)
                                    }
                                    .testTag("unlocked_image_preview"),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Column(modifier = Modifier.padding(14.dp).fillMaxSize()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        viewDocumentExternally(context, doc)
                                    }
                                ) {
                                    Icon(
                                        imageVector = getIconForMime(doc.mimeType),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        doc.fileName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "View externally",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                    HighlightedText(
                                        text = doc.ocrText.ifEmpty { "No index contents saved in database. Click here or use View button to open." },
                                        query = viewModel.globalSearchQuery,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (doc.mimeType.startsWith("image/", ignoreCase = true) || doc.fileName.endsWith(".png", ignoreCase = true) || doc.fileName.endsWith(".jpg", ignoreCase = true) || doc.fileName.endsWith(".jpeg", ignoreCase = true)) {
                        var isOcrInProgress by remember { mutableStateOf(false) }
                        Button(
                            onClick = {
                                scope.launch {
                                    isOcrInProgress = true
                                    try {
                                        viewModel.performOcrOnDocument(doc.id)
                                        Toast.makeText(context, "AI OCR completed! Text indexed successfully.", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "OCR failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                    isOcrInProgress = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("run_ocr_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            enabled = !isOcrInProgress
                        ) {
                            if (isOcrInProgress) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onTertiaryContainer, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing Scan details with AI...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (doc.ocrText.isEmpty()) "Extract Searchable OCR Text" else "Run AI OCR Scanner Again")
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Detail Metrics (Showing simplified File Type instead of MIME type, security pin row completely removed)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DetailRow(label = "Category", value = doc.category, highlightQuery = viewModel.globalSearchQuery)
                        DetailRow(label = "File Type", value = getFileTypeDescription(doc.mimeType))
                        DetailRow(label = "File Size", value = viewModel.formatFileSize(doc.fileSize))
                        if (doc.summary.isNotEmpty()) {
                            DetailRow(label = "AI Summary", value = doc.summary, highlightQuery = viewModel.globalSearchQuery)
                        }
                        if (doc.tags.isNotEmpty()) {
                            DetailRow(label = "Tags", value = doc.tags, highlightQuery = viewModel.globalSearchQuery)
                        }
                        if (doc.notes.isNotEmpty()) {
                            DetailRow(label = "Personal Notes", value = doc.notes, highlightQuery = viewModel.globalSearchQuery)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Ask Arkiv AI Assistant Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ask Document AI Assistant",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            val chatMessages = viewModel.docChatMessagesMap[doc.id] ?: emptyList()
                            if (chatMessages.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    chatMessages.forEach { msg ->
                                        val isUser = msg.first == "User"
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                                        ) {
                                            Text(
                                                text = if (isUser) "You" else "AI Assistant",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                            )
                                            Surface(
                                                color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                shape = RoundedCornerShape(
                                                    topStart = 8.dp,
                                                    topEnd = 8.dp,
                                                    bottomStart = if (isUser) 8.dp else 0.dp,
                                                    bottomEnd = if (isUser) 0.dp else 8.dp
                                                ),
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = msg.second,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                    
                                    TextButton(
                                        onClick = { viewModel.clearDocChat(doc.id) },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Clear Chat", fontSize = 11.sp)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            var questionInput by remember { mutableStateOf("") }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = questionInput,
                                    onValueChange = { questionInput = it },
                                    placeholder = { Text("Ask when it expires, what is the total, etc...", fontSize = 11.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("doc_ai_question_input"),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        if (questionInput.isNotBlank()) {
                                            viewModel.askAiAboutDocument(doc, questionInput)
                                            questionInput = ""
                                        }
                                    },
                                    modifier = Modifier.testTag("doc_ai_ask_button"),
                                    enabled = !viewModel.isDocumentAnswering && questionInput.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    if (viewModel.isDocumentAnswering) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Ask", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                     // Row 1: Direct View & Share actions (Primary user intent)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onOpenInAppViewer(doc) },
                            modifier = Modifier.weight(1.1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Viewer", fontSize = 11.sp, maxLines = 1)
                        }

                        Button(
                            onClick = { exportDocumentAsPdf(context, doc) },
                            modifier = Modifier.weight(1.1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF Export", fontSize = 11.sp, maxLines = 1)
                        }

                        Button(
                            onClick = { shareDocument(context, doc) },
                            modifier = Modifier.weight(0.9f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 2: Secondary / Utility actions (Favorite, Download, Delete)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Favorite Toggle indicator
                        IconButton(
                            onClick = { viewModel.toggleFavorite(doc) },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (doc.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite Toggle",
                                tint = if (doc.isFavorite) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Pin Toggle indicator
                        IconButton(
                            onClick = {
                                viewModel.togglePin(doc) {
                                    Toast.makeText(context, "Maximum of 3 pinned documents reached. Please unpin a document first.", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pin Toggle",
                                tint = if (doc.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Save to Downloads folder
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    val result = viewModel.repository.downloadDocumentToPublicDownloads(doc)
                                    result.onSuccess {
                                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                                    }.onFailure {
                                        Toast.makeText(context, "Download failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save File", fontSize = 11.sp, maxLines = 1)
                        }

                        // Move to trash bin bin
                        Button(
                            onClick = {
                                viewModel.moveToTrash(doc)
                                onDismiss()
                                Toast.makeText(context, "Moved to Trash Bin.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Trash", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HighlightedText(
    text: String,
    query: String,
    fontSize: TextUnit = 13.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontFamily: FontFamily? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    modifier: Modifier = Modifier
) {
    val annotatedString = buildAnnotatedString {
        if (query.isEmpty()) {
            append(text)
        } else {
            var startIdx = 0
            while (startIdx < text.length) {
                val matchIdx = text.indexOf(query, startIdx, ignoreCase = true)
                if (matchIdx == -1) {
                    append(text.substring(startIdx))
                    break
                } else {
                    append(text.substring(startIdx, matchIdx))
                    withStyle(style = SpanStyle(background = Color(0xFFFBC02D), color = Color.Black, fontWeight = FontWeight.Bold)) {
                        append(text.substring(matchIdx, matchIdx + query.length))
                    }
                    startIdx = matchIdx + query.length
                }
            }
        }
    }
    Text(
        text = annotatedString,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        color = color,
        lineHeight = lineHeight,
        modifier = modifier
    )
}

@Composable
fun DetailRow(label: String, value: String, highlightQuery: String = "") {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (highlightQuery.isNotEmpty() && (label == "AI Summary" || label == "Tags" || label == "Personal Notes" || label == "Category")) {
            HighlightedText(
                text = value,
                query = highlightQuery,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun TagSuggestionsRow(
    allExistingTags: List<String>,
    currentTagsString: String,
    onTagsChanged: (String) -> Unit
) {
    val currentTags = remember(currentTagsString) {
        currentTagsString.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.lowercase() }
    }
    
    val unusedTags = allExistingTags.filter { it.lowercase() !in currentTags }
    
    if (unusedTags.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(
                text = "Tap to add existing tag:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(unusedTags) { tag ->
                    SuggestionChip(
                        onClick = {
                            val trimmed = currentTagsString.trim()
                            val newString = if (trimmed.isEmpty()) {
                                tag
                            } else if (trimmed.endsWith(",")) {
                                "$trimmed $tag"
                            } else {
                                "$trimmed, $tag"
                            }
                            onTagsChanged(newString)
                        },
                        label = { Text("#$tag") }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: VaultViewModel) {
    var pinSetupInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val trashDocuments by viewModel.trashedDocuments.collectAsState()

    var showTrashDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("App Lock & Vault Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // 1. PIN app lock activation
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "App security Lock & PIN",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (viewModel.preferencesManager.isAppLockEnabled) "STATUS: ACTIVE Security App PIN active." else "STATUS: UNENCRYPTED. PIN setup recommended.",
                    fontSize = 12.sp,
                    color = if (viewModel.preferencesManager.isAppLockEnabled) Color.Green else MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (!viewModel.preferencesManager.isAppLockEnabled) {
                    OutlinedTextField(
                        value = pinSetupInput,
                        onValueChange = { pinSetupInput = it },
                        label = { Text("Set 4-Digit PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (pinSetupInput.length >= 4) {
                                viewModel.setupPin(pinSetupInput)
                                pinSetupInput = ""
                                Toast.makeText(context, "PIN Enabled successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "PIN must be at least 4 digits.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lock Vault with PIN")
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.disableAppLock()
                            Toast.makeText(context, "PIN Disabled successfully!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Disable App PIN Lock")
                    }
                }
            }
        }

        // 2. Auto empty Trash settings
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Automatic Trash Emptying Cycles",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(" prunes files securely to conserve space.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val daysOptions = listOf(7, 30)
                    daysOptions.forEach { days ->
                        val active = viewModel.preferencesManager.trashAutoEmptyDays == days
                        ElevatedButton(
                            onClick = {
                                viewModel.updateAutoEmptySettings(days)
                                Toast.makeText(context, "Auto-delete set to $days days.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("$days Days", color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        // 3. Vault Trash Container Drawer
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Vault Trash Container", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("${trashDocuments.size} files in Trash", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = { showTrashDialog = true }) {
                        Text("Open Trash")
                    }
                }
            }
        }

        // 4. Dark/Light settings toggles
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Visual Interface Styles", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val styles = listOf("Light", "Dark")
                    styles.forEach { style ->
                        val selected = viewModel.themeState == style
                        OutlinedButton(
                            onClick = { viewModel.switchTheme(style) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(style)
                        }
                    }
                }
            }
        }

        // 5. Database Secure Export
        val activeDocs by viewModel.allActiveDocuments.collectAsState(initial = emptyList())
        var showExportConfirmDialog by remember { mutableStateOf(false) }
        var showExportPasswordDialog by remember { mutableStateOf(false) }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Database Content Backup",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Export all ${activeDocs.size} active document(s) as an encrypted JSON file for backup or transfer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = { showExportConfirmDialog = true },
                        enabled = activeDocs.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export")
                    }
                }
            }
        }

        if (showExportConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showExportConfirmDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm Data Download")
                    }
                },
                text = {
                    Text(
                        text = "Do you want to download all database content as an encrypted JSON file? This backup file will contain all active documents, their category details, tags, OCR extracted content, and secure summaries. You will need to set a password to encrypt this file securely.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showExportConfirmDialog = false
                            showExportPasswordDialog = true
                        }
                    ) {
                        Text("Confirm & Continue")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showExportPasswordDialog) {
            ExportBackupDialog(
                documents = activeDocs,
                onDismiss = { showExportPasswordDialog = false },
                onExport = { password, exportAsJson ->
                    showExportPasswordDialog = false
                    viewModel.exportSelectedDocumentsBackup(activeDocs, password, exportAsJson) { success, result ->
                        if (success) {
                            Toast.makeText(context, "Database backup downloaded successfully: $result", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Backup download failed: $result", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }
    }

    if (showTrashDialog) {
        TrashBinDialog(
            viewModel = viewModel,
            onDismiss = { showTrashDialog = false }
        )
    }
}

@Composable
fun TrashBinDialog(
    viewModel: VaultViewModel,
    onDismiss: () -> Unit
) {
    val trashDocs by viewModel.trashedDocuments.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vault Trash Bin", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (trashDocs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Trash is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        items(trashDocs) { doc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(doc.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Deleted size: ${viewModel.formatFileSize(doc.fileSize)}", fontSize = 11.sp)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            viewModel.restoreFromTrash(doc)
                                            Toast.makeText(context, "Restored successfully.", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Restore", tint = VerifiedGreen)
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.permanentlyDelete(doc)
                                            Toast.makeText(context, "Permanently Deleted.", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = "Delete Forever", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getIconForMime(mime: String): ImageVector {
    return when {
        mime.contains("pdf", ignoreCase = true) -> Icons.Default.PictureAsPdf
        mime.startsWith("image/", ignoreCase = true) -> Icons.Default.Image
        mime.contains("doc", ignoreCase = true) -> Icons.Default.Description
        mime.contains("xls", ignoreCase = true) -> Icons.Default.GridOn
        else -> Icons.Default.Article
    }
}

private fun getFileTypeDescription(mime: String): String {
    return when {
        mime.contains("pdf", ignoreCase = true) -> "PDF Document"
        mime.startsWith("image/", ignoreCase = true) -> "Image File"
        mime.contains("doc", ignoreCase = true) -> "Word Document"
        mime.contains("xls", ignoreCase = true) -> "Excel Sheet"
        mime.contains("text", ignoreCase = true) -> "Text Document"
        else -> "Document Archive"
    }
}

private fun formatDate(ms: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(Date(ms))
}

private fun formatDateDifference(ms: Long): String {
    val diff = System.currentTimeMillis() - ms
    val hours = diff / (3600 * 1000)
    return when {
        hours < 1 -> "Added just now"
        hours < 24 -> "Added $hours hours ago"
        else -> "Added ${hours / 24} days ago"
    }
}

private fun shareDocument(context: Context, doc: DocumentEntity) {
    val file = com.example.utils.AESCryptUtils.getDecryptedFile(context, doc.localUri)
    if (!file.exists()) {
        try {
            val secureFolder = File(context.filesDir, "secure_vault")
            if (!secureFolder.exists()) secureFolder.mkdirs()
            file.writeText(doc.ocrText.ifEmpty { "Arkiv Saved File content for: ${doc.name}" })
        } catch (ignored: Exception) {}
    }

    try {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = doc.mimeType.ifEmpty { "*/*" }
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Sharing Secure Document: ${doc.name}")
            putExtra(Intent.EXTRA_TEXT, "Here is '${doc.fileName}' shared from Secure Arkiv.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Document via:"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun viewDocumentExternally(context: Context, doc: DocumentEntity) {
    val file = com.example.utils.AESCryptUtils.getDecryptedFile(context, doc.localUri)
    if (!file.exists()) {
        try {
            val secureFolder = File(context.filesDir, "secure_vault")
            if (!secureFolder.exists()) secureFolder.mkdirs()
            file.writeText(doc.ocrText.ifEmpty { "Arkiv Saved File content for: ${doc.name}" })
        } catch (ignored: Exception) {}
    }

    try {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, doc.mimeType.ifEmpty { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(viewIntent, "Open with:"))
    } catch (e: Exception) {
        Toast.makeText(context, "No default viewer for ${doc.mimeType}. You can share or download instead!", Toast.LENGTH_LONG).show()
    }
}

private fun exportDocumentAsPdf(context: Context, doc: DocumentEntity) {
    val pdfDocument = android.graphics.pdf.PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 12f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    }

    val titlePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 18f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    val headerPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#3F51B5")
        textSize = 22f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    val subTitlePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.GRAY
        textSize = 10f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
    }

    val labelPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#1A237E")
        textSize = 12f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    
    var pageNumber = 1
    var page = pdfDocument.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    var canvas = page.canvas
    
    val headerBarPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#E8EAF6")
    }
    canvas.drawRect(20f, 20f, (pageWidth - 20).toFloat(), 80f, headerBarPaint)
    
    canvas.drawText("SECURE ARKIV REPORT", 40f, 56f, headerPaint)
    
    var yPosition = 120f
    
    fun checkNewPage() {
        if (yPosition > pageHeight - 80) {
            pdfDocument.finishPage(page)
            pageNumber++
            page = pdfDocument.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            yPosition = 50f
            
            val p = android.graphics.Paint().apply {
                color = android.graphics.Color.LTGRAY
                strokeWidth = 1f
            }
            canvas.drawLine(20f, 35f, (pageWidth - 20).toFloat(), 35f, p)
            canvas.drawText("Secure Arkiv - ${doc.name} (Cont.) - Page $pageNumber", 20f, 30f, subTitlePaint)
            yPosition = 60f
        }
    }
    
    // Header Info
    canvas.drawText("Document ID:", 40f, yPosition, labelPaint)
    canvas.drawText(doc.name, 160f, yPosition, textPaint.apply { typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD) })
    yPosition += 25f
    
    canvas.drawText("Original File:", 40f, yPosition, labelPaint)
    canvas.drawText(doc.fileName, 160f, yPosition, textPaint.apply { typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL) })
    yPosition += 25f
    
    canvas.drawText("Category:", 40f, yPosition, labelPaint)
    canvas.drawText(doc.category, 160f, yPosition, textPaint)
    yPosition += 25f
    
    canvas.drawText("Date Added:", 40f, yPosition, labelPaint)
    canvas.drawText(formatDate(doc.addedDate), 160f, yPosition, textPaint)
    yPosition += 25f

    canvas.drawText("File Size:", 40f, yPosition, labelPaint)
    val formattedSize = when {
        doc.fileSize >= 1024 * 1024 -> String.format("%.1f MB", doc.fileSize.toDouble() / (1024 * 1024))
        doc.fileSize >= 1024 -> String.format("%.1f KB", doc.fileSize.toDouble() / 1024)
        else -> "${doc.fileSize} Bytes"
    }
    canvas.drawText(formattedSize, 160f, yPosition, textPaint)
    yPosition += 25f

    canvas.drawText("Tags:", 40f, yPosition, labelPaint)
    canvas.drawText(doc.tags.ifEmpty { "None" }, 160f, yPosition, textPaint)
    yPosition += 35f
    
    val linePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#C5CAE9")
        strokeWidth = 1.5f
    }
    canvas.drawLine(40f, yPosition, (pageWidth - 40).toFloat(), yPosition, linePaint)
    yPosition += 30f
    
    fun drawParagraph(title: String, bodyText: String) {
        checkNewPage()
        canvas.drawText(title, 40f, yPosition, labelPaint.apply { textSize = 13f })
        yPosition += 22f
        
        val words = bodyText.split(Regex("\\s+"))
        var currentLine = java.lang.StringBuilder()
        val textWidthLimit = pageWidth - 80
        
        val regularTextPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10.5f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        }
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else currentLine.toString() + " " + word
            val testWidth = regularTextPaint.measureText(testLine)
            if (testWidth > textWidthLimit) {
                checkNewPage()
                canvas.drawText(currentLine.toString(), 40f, yPosition, regularTextPaint)
                yPosition += 16f
                currentLine = java.lang.StringBuilder(word)
            } else {
                currentLine.append(if (currentLine.isEmpty()) word else " $word")
            }
        }
        
        if (currentLine.isNotEmpty()) {
            checkNewPage()
            canvas.drawText(currentLine.toString(), 40f, yPosition, regularTextPaint)
            yPosition += 20f
        }
        yPosition += 10f
    }
    
    if (doc.notes.trim().isNotEmpty()) {
        drawParagraph("Personal Notes:", doc.notes)
    }
    
    if (doc.ocrText.trim().isNotEmpty()) {
        drawParagraph("Extracted Text & Content:", doc.ocrText)
    } else {
        drawParagraph("Extracted Text & Content:", "No extracted text found for this document.")
    }

    pdfDocument.finishPage(page)
    
    val outputDir = context.getExternalFilesDir(null) ?: context.cacheDir
    val safeName = doc.name.replace(Regex("[^a-zA-Z0-9]"), "_")
    val pdfFile = java.io.File(outputDir, "${safeName}_SecureExport.pdf")
    try {
        pdfFile.outputStream().use { out ->
            pdfDocument.writeTo(out)
        }
        Toast.makeText(context, "Successfully formatted PDF!", Toast.LENGTH_SHORT).show()
        
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = androidx.core.content.FileProvider.getUriForFile(context, authority, pdfFile)
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(viewIntent, "View / Print PDF via:"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_LONG).show()
    } finally {
        pdfDocument.close()
    }
}

private fun createMockInvoiceBitmap(): Bitmap {
    val b = Bitmap.createBitmap(800, 1000, Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(b)
    c.drawColor(android.graphics.Color.WHITE)
    
    val p = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        isAntiAlias = true
    }
    
    val borderPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.DKGRAY
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 6f
    }
    c.drawRect(20f, 20f, 780f, 980f, borderPaint)
    
    // Header
    p.textSize = 36f
    p.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    c.drawText("TAX INVOICE / RECEIPT", 60f, 100f, p)
    
    p.textSize = 20f
    p.typeface = android.graphics.Typeface.DEFAULT
    c.drawText("Merchant: Secure Coffee & Books Ltd.", 60f, 150f, p)
    c.drawText("Date: June 23, 2026 13:30", 60f, 190f, p)
    c.drawText("Invoice ID: #INV-2026-0681", 60f, 230f, p)
    
    // Line
    c.drawLine(60f, 270f, 740f, 270f, p)
    
    // Items
    c.drawText("Description", 60f, 310f, p)
    c.drawText("Qty", 480f, 310f, p)
    c.drawText("Price", 620f, 310f, p)
    
    c.drawLine(60f, 330f, 740f, 330f, p)
    
    val items = listOf(
        Triple("1. Double Espresso Macchiato", "2", "$9.50"),
        Triple("2. Almond Croissant", "1", "$4.20"),
        Triple("3. 'Modern Android Architecture' Book", "1", "$45.00"),
        Triple("4. Secure Backup Cloud Service", "1", "$15.00")
    )
    
    var y = 370f
    for (item in items) {
        c.drawText(item.first, 60f, y, p)
        c.drawText(item.second, 480f, y, p)
        c.drawText(item.third, 620f, y, p)
        y += 50f
    }
    
    c.drawLine(60f, y, 740f, y, p)
    y += 50f
    
    p.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    c.drawText("Subtotal:", 380f, y, p)
    c.drawText("$73.70", 620f, y, p)
    y += 45f
    
    c.drawText("Local Tax (8.5%):", 380f, y, p)
    c.drawText("$6.26", 620f, y, p)
    y += 50f
    
    p.textSize = 26f
    c.drawText("TOTAL AMOUNT DUE:", 300f, y, p)
    c.drawText("$79.96", 620f, y, p)
    y += 80f
    
    p.textSize = 18f
    p.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
    c.drawText("No refunds after 14 days. Thank you for scanning securely with Secure Arkiv!", 100f, y, p)
    
    return b
}

private fun processScannedImage(
    srcBitmap: Bitmap,
    cropLeftPct: Float,
    cropTopPct: Float,
    cropRightPct: Float,
    cropBottomPct: Float,
    rotation: Float,
    bwBoost: Boolean
): Bitmap {
    // 1. Rotate
    val rotatedBitmap = if (rotation != 0f) {
        val matrix = android.graphics.Matrix().apply { postRotate(rotation) }
        Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.width, srcBitmap.height, matrix, true)
    } else {
        srcBitmap
    }

    // 2. Crop
    val width = rotatedBitmap.width
    val height = rotatedBitmap.height
    
    val left = (width * cropLeftPct).toInt().coerceIn(0, width - 1)
    val right = (width * (1f - cropRightPct)).toInt().coerceIn(left + 1, width)
    val top = (height * cropTopPct).toInt().coerceIn(0, height - 1)
    val bottom = (height * (1f - cropBottomPct)).toInt().coerceIn(top + 1, height)
    
    val cropWidth = (right - left).coerceAtLeast(1)
    val cropHeight = (bottom - top).coerceAtLeast(1)
    
    var finalBitmap = Bitmap.createBitmap(rotatedBitmap, left, top, cropWidth, cropHeight)
    
    if (bwBoost) {
        val bwBitmap = Bitmap.createBitmap(cropWidth, cropHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bwBitmap)
        val paint = android.graphics.Paint()
        
        val colorMatrix = android.graphics.ColorMatrix().apply {
            setSaturation(0f)
        }
        
        val contrastFactor = 1.8f
        val offset = -100f
        val contrastMatrix = floatArrayOf(
            contrastFactor, 0f, 0f, 0f, offset,
            0f, contrastFactor, 0f, 0f, offset,
            0f, 0f, contrastFactor, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        )
        
        val matrixObj = android.graphics.ColorMatrix(contrastMatrix)
        matrixObj.postConcat(colorMatrix)
        
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(matrixObj)
        canvas.drawBitmap(finalBitmap, 0f, 0f, paint)
        finalBitmap = bwBitmap
    }
    
    return finalBitmap
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(viewModel: VaultViewModel, onCompleteScan: () -> Unit) {
    val context = LocalContext.current
    
    // Loaded scan photo state
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // Cropping & rotation parameters
    var cropLeft by remember { mutableStateOf(0f) }
    var cropTop by remember { mutableStateOf(0f) }
    var cropRight by remember { mutableStateOf(0f) }
    var cropBottom by remember { mutableStateOf(0f) }
    var currentRotation by remember { mutableStateOf(0f) }
    var isBwBoostEnabled by remember { mutableStateOf(false) }

    val processedBitmap = remember(sourceBitmap, cropLeft, cropTop, cropRight, cropBottom, currentRotation, isBwBoostEnabled) {
        val active = sourceBitmap
        if (active != null) {
            processScannedImage(
                active,
                cropLeft,
                cropTop,
                cropRight,
                cropBottom,
                currentRotation,
                isBwBoostEnabled
            )
        } else {
            null
        }
    }
    
    // Metadata forms
    var docName by remember { mutableStateOf("Scanned_Document") }
    var selectedCat by remember { mutableStateOf("Identity") }
    var tagsInput by remember { mutableStateOf("Scan, PDF") }
    var scanNotes by remember { mutableStateOf("Scanned on ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}") }
    
    // Dropdown helpers
    val categories by viewModel.allCategories.collectAsState()
    val documents by viewModel.allActiveDocuments.collectAsState(initial = emptyList())
    val allExistingTags = remember(documents) {
        documents.flatMap { doc ->
            doc.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }.distinct().sorted()
    }
    var expandDropdown by remember { mutableStateOf(false) }
    
    // Temp photo file handler
    var tempFileForCamera by remember { mutableStateOf<java.io.File?>(null) }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempFileForCamera?.let { file ->
                val loaded = viewModel.loadBitmapFromUri(android.net.Uri.fromFile(file))
                if (loaded != null) {
                    sourceBitmap = loaded
                    docName = "Scan_Cam_${System.currentTimeMillis() / 1000}"
                    cropLeft = 0f
                    cropTop = 0f
                    cropRight = 0f
                    cropBottom = 0f
                    currentRotation = 0f
                } else {
                    Toast.makeText(context, "Failed to load captured photo.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val loaded = viewModel.loadBitmapFromUri(uri)
            if (loaded != null) {
                sourceBitmap = loaded
                docName = "Scan_Gallery_${System.currentTimeMillis() / 1000}"
                cropLeft = 0f
                cropTop = 0f
                cropRight = 0f
                cropBottom = 0f
                currentRotation = 0f
            } else {
                Toast.makeText(context, "Failed to load image from gallery.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    var isSaving by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("scan_screen_container"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "AI Intelligent Scanner",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Capture paper docs, crop, apply high-contrast scan filters & save to PDF",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (sourceBitmap == null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "No Document Loaded",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Select a source to capture or load physical documents to begin cropping and PDF conversion.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                val file = java.io.File(context.cacheDir, "camera_capture_temp.jpg")
                                if (file.exists()) file.delete()
                                file.createNewFile()
                                tempFileForCamera = file
                                val authority = "${context.packageName}.fileprovider"
                                val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
                                cameraLauncher.launch(uri)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error starting camera: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("btn_camera_capture"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Capture Photo with Camera")
                    }
                    
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("btn_gallery_import"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Load Photo from Library / Gallery")
                    }
                    
                    Button(
                        onClick = {
                            sourceBitmap = createMockInvoiceBitmap()
                            docName = "Scanned_Invoice_Sample"
                            cropLeft = 0.02f
                            cropTop = 0.02f
                            cropRight = 0.02f
                            cropBottom = 0.02f
                            currentRotation = 0f
                            Toast.makeText(context, "Simulated document coffee receipt loaded!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("btn_mock_document"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("1-Click Simulated Paper Receipt (Test Scan)")
                    }
                }
            }
        } else {
            val finalBitmap = processedBitmap!!
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Interactive Document Processing",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(
                        onClick = { sourceBitmap = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Scan", fontSize = 12.sp)
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = finalBitmap.asImageBitmap(),
                            contentDescription = "Cropped Live Preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                        
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Preview size: ${finalBitmap.width} x ${finalBitmap.height}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Precision Margin Cropping Controls",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Crop Left", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${(cropLeft * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = cropLeft,
                                onValueChange = { cropLeft = it },
                                valueRange = 0f..0.45f,
                                modifier = Modifier.height(24.dp)
                            )
                        }
                        
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Crop Right", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${(cropRight * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = cropRight,
                                onValueChange = { cropRight = it },
                                valueRange = 0f..0.45f,
                                modifier = Modifier.height(24.dp)
                            )
                        }
                        
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Crop Top", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${(cropTop * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = cropTop,
                                onValueChange = { cropTop = it },
                                valueRange = 0f..0.45f,
                                modifier = Modifier.height(24.dp)
                            )
                        }
                        
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Crop Bottom", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${(cropBottom * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = cropBottom,
                                onValueChange = { cropBottom = it },
                                valueRange = 0f..0.45f,
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            currentRotation = (currentRotation + 90f) % 360f
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rotate 90°", fontSize = 11.sp, maxLines = 1)
                    }
                    
                    OutlinedIconButton(
                        onClick = {
                            cropLeft = 0f
                            cropTop = 0f
                            cropRight = 0f
                            cropBottom = 0f
                            currentRotation = 0f
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                    }
                    
                    Surface(
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isBwBoostEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable { isBwBoostEnabled = !isBwBoostEnabled }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("B&W Ink Boost", fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Save Information",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        OutlinedTextField(
                            value = docName,
                            onValueChange = { docName = it },
                            label = { Text("Document File Name") },
                            modifier = Modifier.fillMaxWidth().testTag("scan_title_input"),
                            singleLine = true
                        )
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Category: $selectedCat")
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose Category")
                                }
                            }
                            
                            DropdownMenu(
                                expanded = expandDropdown,
                                onDismissRequest = { expandDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            selectedCat = cat.name
                                            expandDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        Button(
                            onClick = {
                                if (docName.trim().isEmpty()) {
                                    Toast.makeText(context, "Provide a name first for AI scanning.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.suggestCategoryAndTags(
                                    documentName = docName,
                                    notes = scanNotes,
                                    selectedUri = null
                                ) { category, suggestedTags ->
                                    selectedCat = category
                                    tagsInput = suggestedTags.joinToString(", ")
                                    Toast.makeText(context, "AI suggested Category ($category) & Tags successfully!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            enabled = !viewModel.isSuggesting
                        ) {
                            if (viewModel.isSuggesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing Scan...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI Auto-Analyze Crop Details", fontSize = 12.sp)
                            }
                        }
                        
                        OutlinedTextField(
                            value = tagsInput,
                            onValueChange = { tagsInput = it },
                            label = { Text("Tags (comma separated)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        TagSuggestionsRow(
                            allExistingTags = allExistingTags,
                            currentTagsString = tagsInput,
                            onTagsChanged = { tagsInput = it }
                        )
                        
                        OutlinedTextField(
                            value = scanNotes,
                            onValueChange = { scanNotes = it },
                            label = { Text("Personal Notes") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    }
                }
            }
            
            item {
                Button(
                    onClick = {
                        if (docName.trim().isEmpty()) {
                            Toast.makeText(context, "Please provide a file name before saving.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSaving = true
                        
                        viewModel.saveScannedPdf(
                            context = context,
                            fileName = docName.trim(),
                            name = docName.trim(),
                            category = selectedCat,
                            tags = tagsInput,
                            ocrText = "Scanned document text indexed automatically. Document: $docName",
                            notes = scanNotes,
                            bitmap = finalBitmap
                        ) { success, msg ->
                            isSaving = false
                            if (success) {
                                Toast.makeText(context, msg ?: "PDF compiled and exported successfully!", Toast.LENGTH_LONG).show()
                                sourceBitmap = null
                                onCompleteScan()
                            } else {
                                Toast.makeText(context, "Error saving scanned document: $msg", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("btn_export_scanned_pdf"),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compiling PDF & Exporting...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SAVE TO SECURE DB & PHONE STORAGE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// SECURE SYSTEM TOOLS SUITE (MODIFY, OPTIMIZE, SECURE, CONVERT)
// ==========================================

@Composable
fun DocumentPickerDialog(
    documents: List<DocumentEntity>,
    onDismiss: () -> Unit,
    onSelected: (DocumentEntity) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Target Document", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                if (documents.isEmpty()) {
                    Text("No documents found in vault. Upload or scan first.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(documents) { doc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelected(doc) }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val icon = if (doc.mimeType == "application/pdf") Icons.Default.PictureAsPdf else Icons.Default.Description
                                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(doc.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${doc.fileName} • ${doc.category}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun MultiDocumentPickerDialog(
    documents: List<DocumentEntity>,
    selectedIds: Set<Long>,
    onDismiss: () -> Unit,
    onToggleSelect: (DocumentEntity) -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Target Documents", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                if (documents.isEmpty()) {
                    Text("No documents loaded.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(documents) { doc ->
                            val isSelected = selectedIds.contains(doc.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleSelect(doc) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onToggleSelect(doc) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val icon = if (doc.mimeType == "application/pdf") Icons.Default.PictureAsPdf else Icons.Default.Description
                                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(doc.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(doc.fileName, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.align(Alignment.End)) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onConfirm, enabled = selectedIds.isNotEmpty()) {
                        Text("Select (${selectedIds.size})")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(viewModel: VaultViewModel, onOpenBulkImport: () -> Unit) {
    val context = LocalContext.current
    val documents by viewModel.allActiveDocuments.collectAsState(initial = emptyList())

    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("Modify", "Optimize", "Secure", "Convert", "Ask AI")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column {
                Text(
                    text = "Secure Tools Suite",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Professional locally-run tools to modify, secure, and convert records",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Tab Selector Row
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    modifier = Modifier.testTag("tools_tab_$index"),
                    text = {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content Area Scroll list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("tools_bulk_import_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Bulk Smart Import Utility",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Select multiple files at once. Auto-applies category & tags.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onOpenBulkImport,
                            modifier = Modifier.testTag("tools_bulk_import_launch_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Launch", fontSize = 11.sp)
                        }
                    }
                }
            }

            item {
                Crossfade(
                    targetState = activeTab,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "tools_tab_transition"
                ) { tab ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when (tab) {
                            0 -> { // MODIFY TAB
                                ModifyTabMergeSection(documents, viewModel, context)
                                ModifyTabSplitSection(documents, viewModel, context)
                                ModifyTabRotateSection(documents, viewModel, context)
                                ModifyTabRearrangeSection(documents, viewModel, context)
                            }
                            1 -> { // OPTIMIZE TAB
                                OptimizeTabSection(documents, viewModel, context)
                            }
                            2 -> { // SECURE TAB
                                SecureTabEncryptSection(documents, viewModel, context)
                                SecureTabDecryptSection(documents, viewModel, context)
                                SecureTabShredSection(documents, viewModel, context)
                            }
                            3 -> { // CONVERT TAB
                                ConvertTabSection(documents, viewModel, context)
                            }
                            4 -> { // ASK AI TAB
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth().testTag("tools_ai_chat_card"),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    "Ask Arkiv Central AI",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    "Analyze, query, or search across all documents",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        val chatMessages = viewModel.vaultChatMessages
                                        if (chatMessages.isEmpty()) {
                                            // Empty Chat State
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    "Ask questions about your files like:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                val suggestions = listOf(
                                                    "\"Summarize my total active documents.\"",
                                                    "\"List all tags present in my vault.\"",
                                                    "\"Do I have any tax files or ID documents?\""
                                                )
                                                suggestions.forEach { suggestion ->
                                                    Text(
                                                        text = suggestion,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                        modifier = Modifier
                                                            .padding(vertical = 4.dp)
                                                            .clickable {
                                                                viewModel.askAiAboutVault(suggestion.removeSurrounding("\""))
                                                            }
                                                    )
                                                }
                                            }
                                        } else {
                                            // Chat History Area
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(12.dp)
                                            ) {
                                                chatMessages.forEach { msg ->
                                                    val isUser = msg.first == "User"
                                                    Column(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isUser) Icons.Default.Person else Icons.Default.AutoAwesome,
                                                                contentDescription = null,
                                                                tint = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = if (isUser) "You" else "Arkiv AI",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                                            )
                                                        }
                                                        Surface(
                                                            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                            contentColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            shape = RoundedCornerShape(
                                                                topStart = 12.dp,
                                                                topEnd = 12.dp,
                                                                bottomStart = if (isUser) 12.dp else 0.dp,
                                                                bottomEnd = if (isUser) 0.dp else 12.dp
                                                            ),
                                                            modifier = Modifier.padding(vertical = 4.dp)
                                                        ) {
                                                            Text(
                                                                text = msg.second,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Context active: ${documents.size} documents",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                    TextButton(
                                                        onClick = { viewModel.clearVaultChat() }
                                                    ) {
                                                        Text("Reset Assistant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        var vaultQuestionInput by remember { mutableStateOf("") }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = vaultQuestionInput,
                                                onValueChange = { vaultQuestionInput = it },
                                                placeholder = { Text("Ask about any document or search terms...", fontSize = 12.sp) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("vault_ai_question_input"),
                                                singleLine = true,
                                                textStyle = MaterialTheme.typography.bodyMedium
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    if (vaultQuestionInput.isNotBlank()) {
                                                        viewModel.askAiAboutVault(vaultQuestionInput)
                                                        vaultQuestionInput = ""
                                                    }
                                                },
                                                modifier = Modifier.testTag("vault_ai_ask_button"),
                                                enabled = !viewModel.isVaultAnswering && vaultQuestionInput.isNotBlank()
                                            ) {
                                                if (viewModel.isVaultAnswering) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(18.dp),
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    Text("Ask AI", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. MODIFY TAB DETAILS
// ==========================================

@Composable
fun ModifyTabMergeSection(
    documents: List<DocumentEntity>,
    viewModel: VaultViewModel,
    context: Context
) {
    var showPickerDialog by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var newFileName by remember { mutableStateOf("merged_documents") }
    var isMerging by remember { mutableStateOf(false) }

    val selectedDocs = documents.filter { selectedIds.contains(it.id) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Merge Documents", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                "Combine multiple files (PDFs, images, text) into a single optimized PDF.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Current selections summary
            if (selectedDocs.isNotEmpty()) {
                Text(
                    "Selected Files (${selectedDocs.size}):",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        selectedDocs.forEachIndexed { i, doc ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Text("${i + 1}. ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    Text(doc.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(
                                    onClick = { selectedIds = selectedIds - doc.id },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { showPickerDialog = true },
                modifier = Modifier.fillMaxWidth().testTag("btn_pick_merge_docs"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Text(if (selectedDocs.isEmpty()) "Choose Documents to Merge" else "Modify Selection")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = newFileName,
                onValueChange = { newFileName = it },
                label = { Text("Output PDF Name") },
                modifier = Modifier.fillMaxWidth().testTag("input_merge_filename"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    isMerging = true
                    viewModel.mergeDocuments(context, selectedDocs, newFileName) { ok, msg ->
                        isMerging = false
                        Toast.makeText(context, msg ?: (if (ok) "Succeeded" else "Failed"), Toast.LENGTH_LONG).show()
                        if (ok) {
                            selectedIds = emptySet()
                            newFileName = "merged_documents"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("btn_execute_merge"),
                enabled = selectedDocs.size >= 2 && !isMerging
            ) {
                if (isMerging) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("MERGE MATCHING FILES")
                }
            }
        }
    }

    if (showPickerDialog) {
        val selectable = documents.filter { !it.isTrash && (it.mimeType == "application/pdf" || it.mimeType.startsWith("image/") || it.mimeType == "text/plain") }
        MultiDocumentPickerDialog(
            documents = selectable,
            selectedIds = selectedIds,
            onDismiss = { showPickerDialog = false },
            onToggleSelect = { doc ->
                selectedIds = if (selectedIds.contains(doc.id)) selectedIds - doc.id else selectedIds + doc.id
            },
            onConfirm = { showPickerDialog = false }
        )
    }
}

@Composable
fun ModifyTabSplitSection(
    documents: List<DocumentEntity>,
    viewModel: VaultViewModel,
    context: Context
) {
    var showPickerDialog by remember { mutableStateOf(false) }
    var selectedDoc by remember { mutableStateOf<DocumentEntity?>(null) }
    var pageInput by remember { mutableStateOf("1") }
    var splitName by remember { mutableStateOf("split_extracted_page") }
    var isSplitting by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ContentCut, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Split PDF Pages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                "Extract any individual page from a target multi-page PDF document.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDoc?.let { "Selected: ${it.name}" } ?: "No PDF Selected",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = { showPickerDialog = true },
                    modifier = Modifier.testTag("btn_pick_split_doc")
                ) {
                    Text("Select PDF")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { pageInput = it },
                    label = { Text("Page Number (1-based)") },
                    modifier = Modifier.weight(1f).testTag("input_split_page_num"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = splitName,
                    onValueChange = { splitName = it },
                    label = { Text("Split Output Name") },
                    modifier = Modifier.weight(1.5f).testTag("input_split_docname"),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val pageIndex = pageInput.toIntOrNull()?.let { it - 1 }
                    if (pageIndex == null || pageIndex < 0) {
                        Toast.makeText(context, "Please enter a valid page number.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    selectedDoc?.let { doc ->
                        isSplitting = true
                        viewModel.splitDocumentPage(context, doc, pageIndex, splitName) { ok, msg ->
                            isSplitting = false
                            Toast.makeText(context, msg ?: (if (ok) "Extraction Succeeded" else "Extraction Failed"), Toast.LENGTH_LONG).show()
                            if (ok) {
                                selectedDoc = null
                                pageInput = "1"
                                splitName = "split_extracted_page"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("btn_execute_split"),
                enabled = selectedDoc != null && !isSplitting
            ) {
                if (isSplitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("EXTRACT & SPLIT PAGE")
                }
            }
        }
    }

    if (showPickerDialog) {
        val pdfs = documents.filter { !it.isTrash && it.mimeType == "application/pdf" }
        DocumentPickerDialog(
            documents = pdfs,
            onDismiss = { showPickerDialog = false },
            onSelected = {
                selectedDoc = it
                showPickerDialog = false
            }
        )
    }
}

@Composable
fun ModifyTabRotateSection(
    documents: List<DocumentEntity>,
    viewModel: VaultViewModel,
    context: Context
) {
    var showPickerDialog by remember { mutableStateOf(false) }
    var selectedDoc by remember { mutableStateOf<DocumentEntity?>(null) }
    var rotateAngle by remember { mutableStateOf(90f) }
    var rotatedName by remember { mutableStateOf("rotated_record") }
    var isRotating by remember { mutableStateOf(false) }

    val angles = listOf(90f, 180f, 270f)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Rotate Document Pages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                "Rotate PDFs or image scans locally (90°, 180°, or 270° clockwise).",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDoc?.let { "Selected: ${it.name}" } ?: "No File Selected",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = { showPickerDialog = true },
                    modifier = Modifier.testTag("btn_pick_rotate_doc")
                ) {
                    Text("Select File")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Select Rotation Angle:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                angles.forEach { angle ->
                    val isSelected = rotateAngle == angle
                    FilterChip(
                        selected = isSelected,
                        onClick = { rotateAngle = angle },
                        label = { Text("${angle.toInt()}° Clockwise") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = rotatedName,
                onValueChange = { rotatedName = it },
                label = { Text("Rotated Output Name") },
                modifier = Modifier.fillMaxWidth().testTag("input_rotate_docname"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    selectedDoc?.let { doc ->
                        isRotating = true
                        viewModel.rotateDocument(context, doc, rotateAngle, rotatedName) { ok, msg ->
                            isRotating = false
                            Toast.makeText(context, msg ?: (if (ok) "Succeeded" else "Failed"), Toast.LENGTH_LONG).show()
                            if (ok) {
                                selectedDoc = null
                                rotatedName = "rotated_record"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("btn_execute_rotate"),
                enabled = selectedDoc != null && !isRotating
            ) {
                if (isRotating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("ROTATE PAGES")
                }
            }
        }
    }

    if (showPickerDialog) {
        val rotatable = documents.filter { !it.isTrash && (it.mimeType == "application/pdf" || it.mimeType.startsWith("image/")) }
        DocumentPickerDialog(
            documents = rotatable,
            onDismiss = { showPickerDialog = false },
            onSelected = {
                selectedDoc = it
                showPickerDialog = false
            }
        )
    }
}

@Composable
fun ModifyTabRearrangeSection(
    documents: List<DocumentEntity>,
    viewModel: VaultViewModel,
    context: Context
) {
    var showPickerDialog by remember { mutableStateOf(false) }
    var selectedDoc by remember { mutableStateOf<DocumentEntity?>(null) }
    var sequenceInput by remember { mutableStateOf("1, 2") }
    var outputName by remember { mutableStateOf("rearranged_record") }
    var isRearranging by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CompareArrows, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Rearrange PDF Pages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                "Change page sequences in seconds. Example: '3, 1, 2' reorders pages.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDoc?.let { "Selected: ${it.name}" } ?: "No PDF Selected",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = { showPickerDialog = true },
                    modifier = Modifier.testTag("btn_pick_rearrange_doc")
                ) {
                    Text("Select PDF")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = sequenceInput,
                onValueChange = { sequenceInput = it },
                label = { Text("Set Custom Order Sequence (e.g. 2, 1, 3)") },
                placeholder = { Text("Page indices separated by commas") },
                modifier = Modifier.fillMaxWidth().testTag("input_rearrange_seq"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = outputName,
                onValueChange = { outputName = it },
                label = { Text("Rearranged Output Name") },
                modifier = Modifier.fillMaxWidth().testTag("input_rearrange_docname"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val parsedSequence = sequenceInput.split(",")
                        .mapNotNull { it.trim().toIntOrNull()?.let { idx -> idx - 1 } }
                    if (parsedSequence.isEmpty()) {
                        Toast.makeText(context, "Invalid format. Use comma separated numbers like '2, 1, 3'.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    selectedDoc?.let { doc ->
                        isRearranging = true
                        viewModel.rearrangeDocument(context, doc, parsedSequence, outputName) { ok, msg ->
                            isRearranging = false
                            Toast.makeText(context, msg ?: (if (ok) "Rearranging Succeeded" else "Rearranging Failed"), Toast.LENGTH_LONG).show()
                            if (ok) {
                                selectedDoc = null
                                sequenceInput = "1, 2"
                                outputName = "rearranged_record"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("btn_execute_rearrange"),
                enabled = selectedDoc != null && !isRearranging
            ) {
                if (isRearranging) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("REARRANGE & COMPILE PDF")
                }
            }
        }
    }

    if (showPickerDialog) {
        val pdfs = documents.filter { !it.isTrash && it.mimeType == "application/pdf" }
        DocumentPickerDialog(
            documents = pdfs,
            onDismiss = { showPickerDialog = false },
            onSelected = {
                selectedDoc = it
                showPickerDialog = false
            }
        )
    }
}

// ==========================================
// 2. OPTIMIZE TAB DETAILS
// ==========================================

@Composable
fun OptimizeTabSection(
    documents: List<DocumentEntity>,
    viewModel: VaultViewModel,
    context: Context
) {
    var showPickerDialog by remember { mutableStateOf(false) }
    var selectedDoc by remember { mutableStateOf<DocumentEntity?>(null) }
    var qualityPreset by remember { mutableStateOf("MEDIUM") }
    var outputName by remember { mutableStateOf("compressed_record") }
    var isOptimizing by remember { mutableStateOf(false) }

    val presets = listOf("LOW", "MEDIUM", "HIGH")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DownloadForOffline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reduce File Size (Compressor)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                "Optimize memory footprints instantly for PDF documents and image scans.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDoc?.let { "Selected: ${it.name} (${viewModel.formatFileSize(it.fileSize)})" } ?: "No File Selected",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = { showPickerDialog = true },
                    modifier = Modifier.testTag("btn_pick_optimize_doc")
                ) {
                    Text("Select File")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Compression Level Preset:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { prs ->
                    val isSelected = qualityPreset == prs
                    val desc = when(prs) {
                        "LOW" -> "Low (Max Comp.)"
                        "MEDIUM" -> "Medium (Balanced)"
                        else -> "High (Low Comp.)"
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { qualityPreset = prs },
                        label = { Text(desc) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = outputName,
                onValueChange = { outputName = it },
                label = { Text("Optimized File Save Name") },
                modifier = Modifier.fillMaxWidth().testTag("input_optimize_docname"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    selectedDoc?.let { doc ->
                        isOptimizing = true
                        viewModel.optimizeDocument(context, doc, qualityPreset, outputName) { ok, msg ->
                            isOptimizing = false
                            Toast.makeText(context, msg ?: (if (ok) "Compression Succeeded" else "Compression Failed"), Toast.LENGTH_LONG).show()
                            if (ok) {
                                selectedDoc = null
                                outputName = "compressed_record"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("btn_execute_optimize"),
                enabled = selectedDoc != null && !isOptimizing
            ) {
                if (isOptimizing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("OPTIMIZE & COMPRESS RECORD")
                }
            }
        }
    }

    if (showPickerDialog) {
        val optimizable = documents.filter { !it.isTrash && (it.mimeType == "application/pdf" || it.mimeType.startsWith("image/")) }
        DocumentPickerDialog(
            documents = optimizable,
            onDismiss = { showPickerDialog = false },
            onSelected = {
                selectedDoc = it
                showPickerDialog = false
            }
        )
    }
}

// ==========================================
// 3. SECURE TAB DETAILS
// ==========================================

@Composable
fun SecureTabEncryptSection(
    documents: List<DocumentEntity>,
    viewModel: VaultViewModel,
    context: Context
) {
    var showPickerDialog by remember { mutableStateOf(false) }
    var selectedDoc by remember { mutableStateOf<DocumentEntity?>(null) }
    var password by remember { mutableStateOf("") }
    var outputName by remember { mutableStateOf("secure_encrypted_file") }
    var isEncrypting by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Encrypt File (AES-256)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                "Secure files with custom passwords. Results are cryptographically locked (.enc) local files.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDoc?.let { "Selected: ${it.name}" } ?: "No File Selected",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = { showPickerDialog = true },
                    modifier = Modifier.testTag("btn_pick_encrypt_doc")
                ) {
                    Text("Select File")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Encryption Password") },
                modifier = Modifier.fillMaxWidth().testTag("input_encrypt_password"),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = outputName,
                onValueChange = { outputName = it },
                label = { Text("Encrypted Save Name") },
                modifier = Modifier.fillMaxWidth().testTag("input_encrypt_docname"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (password.length < 4) {
                        Toast.makeText(context, "Password must be at least 4 characters long.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    selectedDoc?.let { doc ->
                        isEncrypting = true
                        viewModel.encryptDocument(context, doc, password, outputName) { ok, msg ->
                            isEncrypting = false
                            Toast.makeText(context, msg ?: (if (ok) "Secured" else "Failed"), Toast.LENGTH_LONG).show()
                            if (ok) {
                                selectedDoc = null
                                password = ""
                                outputName = "secure_encrypted_file"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("btn_execute_encrypt"),
                enabled = selectedDoc != null && !isEncrypting && password.isNotEmpty()
            ) {
                if (isEncrypting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("ENCRYPT FILE (AES-256)")
                }
            }
        }
    }

    if (showPickerDialog) {
        val encryptable = documents.filter { !it.isTrash && it.mimeType != "application/octet-stream" }
        DocumentPickerDialog(
            documents = encryptable,
            onDismiss = { showPickerDialog = false },
            onSelected = {
                selectedDoc = it
                showPickerDialog = false
            }
        )
    }
}

@Composable
fun SecureTabDecryptSection(
    documents: List<DocumentEntity>,
    viewModel: VaultViewModel,
    context: Context
) {
    var showPickerDialog by remember { mutableStateOf(false) }
    var selectedDoc by remember { mutableStateOf<DocumentEntity?>(null) }
    var password by remember { mutableStateOf("") }
    var decryptedName by remember { mutableStateOf("decrypted_document") }
    var targetExt by remember { mutableStateOf("pdf") }
    var isDecrypting by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val extensions = listOf("pdf", "jpg", "png", "txt")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LockOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Decrypt Local File", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                "Unlock password-locked records (.enc) safely back to original extensions.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDoc?.let { "Selected: ${it.name}" } ?: "No Encrypted File Selected",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = { showPickerDialog = true },
                    modifier = Modifier.testTag("btn_pick_decrypt_doc")
                ) {
                    Text("Select Encrypted")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Decryption Password") },
                modifier = Modifier.fillMaxWidth().testTag("input_decrypt_password"),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Restore Format As:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                extensions.forEach { ext ->
                    val isSelected = targetExt == ext
                    FilterChip(
                        selected = isSelected,
                        onClick = { targetExt = ext },
                        label = { Text(ext.uppercase()) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = decryptedName,
                onValueChange = { decryptedName = it },
                label = { Text("Restored Decrypted Save Name") },
                modifier = Modifier.fillMaxWidth().testTag("input_decrypt_docname"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    selectedDoc?.let { doc ->
                        isDecrypting = true
                        viewModel.decryptDocument(context, doc, password, decryptedName, targetExt) { ok, msg ->
                            isDecrypting = false
                            Toast.makeText(context, msg ?: (if (ok) "Unlocked" else "Failed"), Toast.LENGTH_LONG).show()
                            if (ok) {
                                selectedDoc = null
                                password = ""
                                decryptedName = "decrypted_document"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("btn_execute_decrypt"),
                enabled = selectedDoc != null && !isDecrypting && password.isNotEmpty()
            ) {
                if (isDecrypting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("DECRYPT & RESTORE FILE")
                }
            }
        }
    }

    if (showPickerDialog) {
        val encryptedFiles = documents.filter { !it.isTrash && (it.mimeType == "application/octet-stream" || it.fileName.endsWith(".enc")) }
        DocumentPickerDialog(
            documents = encryptedFiles,
            onDismiss = { showPickerDialog = false },
            onSelected = {
                selectedDoc = it
                showPickerDialog = false
            }
        )
    }
}

@Composable
fun SecureTabShredSection(
    documents: List<DocumentEntity>,
    viewModel: VaultViewModel,
    context: Context
) {
    var showPickerDialog by remember { mutableStateOf(false) }
    var selectedDoc by remember { mutableStateOf<DocumentEntity?>(null) }
    var isShredding by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Secure Digital Shredder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Text(
                "Warning: Overwrites raw document bytes with zero padding on disk before deletion, completely making content recovery impossible.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDoc?.let { "Target: ${it.name}" } ?: "No File Marked",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = { showPickerDialog = true },
                    modifier = Modifier.testTag("btn_pick_shred_doc"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Mark File")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    selectedDoc?.let { doc ->
                        isShredding = true
                        viewModel.secureShredDocument(doc) { ok, msg ->
                            isShredding = false
                            Toast.makeText(context, msg ?: (if (ok) "Shredded" else "Failed"), Toast.LENGTH_LONG).show()
                            if (ok) {
                                selectedDoc = null
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("btn_execute_shred"),
                enabled = selectedDoc != null && !isShredding,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                if (isShredding) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SECURE SHRED & ERASE PERMANENTLY")
                }
            }
        }
    }

    if (showPickerDialog) {
        val targetDocs = documents.filter { !it.isTrash }
        DocumentPickerDialog(
            documents = targetDocs,
            onDismiss = { showPickerDialog = false },
            onSelected = {
                selectedDoc = it
                showPickerDialog = false
            }
        )
    }
}

// ==========================================
// 4. CONVERT TAB DETAILS
// ==========================================

@Composable
fun ConvertTabSection(
    documents: List<DocumentEntity>,
    viewModel: VaultViewModel,
    context: Context
) {
    var showPickerDialog by remember { mutableStateOf(false) }
    var selectedDoc by remember { mutableStateOf<DocumentEntity?>(null) }
    var convertType by remember { mutableStateOf("PDF_TO_IMAGE") }
    var outputName by remember { mutableStateOf("converted_record") }
    var isConverting by remember { mutableStateOf(false) }

    val options = listOf(
        "PDF_TO_IMAGE" to "PDF to Image (JPEG)",
        "IMAGE_TO_PDF" to "Image to PDF",
        "TO_PLAIN_TEXT" to "To Plain Text (.txt)"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ChangeCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Format Converter Suite", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                "Convert between standard PDF format, JPEG image element layers, or plain text indexes.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDoc?.let { "Selected: ${it.name}" } ?: "No File Selected",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = { showPickerDialog = true },
                    modifier = Modifier.testTag("btn_pick_convert_doc")
                ) {
                    Text("Select File")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Select Conversion Format Target:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                options.forEach { (type, label) ->
                    val isSelected = convertType == type
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                onClick = { convertType = type }
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { convertType = type }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = outputName,
                onValueChange = { outputName = it },
                label = { Text("Converted Output Name") },
                modifier = Modifier.fillMaxWidth().testTag("input_convert_docname"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    selectedDoc?.let { doc ->
                        isConverting = true
                        viewModel.convertDocument(context, doc, convertType, outputName) { ok, msg ->
                            isConverting = false
                            Toast.makeText(context, msg ?: (if (ok) "Conversion Completed" else "Conversion Failed"), Toast.LENGTH_LONG).show()
                            if (ok) {
                                selectedDoc = null
                                outputName = "converted_record"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("btn_execute_convert"),
                enabled = selectedDoc != null && !isConverting
            ) {
                if (isConverting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("CONVERT FORMAT")
                }
            }
        }
    }

    if (showPickerDialog) {
        val targetDocs = documents.filter { !it.isTrash }
        DocumentPickerDialog(
            documents = targetDocs,
            onDismiss = { showPickerDialog = false },
            onSelected = {
                selectedDoc = it
                showPickerDialog = false
            }
        )
    }
}

@Composable
fun ExportBackupDialog(
    documents: List<DocumentEntity>,
    onDismiss: () -> Unit,
    onExport: (String, Boolean) -> Unit // password, exportAsJson
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var exportAsJson by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Secure Backup Export")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "You are exporting ${documents.size} selected document(s) as an encrypted off-device backup.",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Select format
                Text("Select Format:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { exportAsJson = true }
                    ) {
                        RadioButton(selected = exportAsJson, onClick = { exportAsJson = true })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Encrypted JSON", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { exportAsJson = false }
                    ) {
                        RadioButton(selected = !exportAsJson, onClick = { exportAsJson = false })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Encrypted PDF", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Encryption Password") },
                    placeholder = { Text("Password to decrypt later") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    }
                )
                Text(
                    "This password is required to decrypt the backup file. Keep it safe!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onExport(password, exportAsJson) },
                enabled = password.isNotEmpty()
            ) {
                Text("Export Encrypted")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

data class StorageSlice(
    val label: String,
    val size: Long,
    val percentage: Float,
    val color: Color
)

@Composable
fun StorageUsageVisualizer(
    viewModel: VaultViewModel,
    documents: List<DocumentEntity>
) {
    var viewByCategories by remember { mutableStateOf(true) }
    val totalSize = documents.sumOf { it.fileSize }
    val virtualLimit = 512 * 1024 * 1024L // 512 MB virtual limit

    val getColorForCategory: (String) -> Color = { cat ->
        when (cat.lowercase(Locale.ROOT).trim()) {
            "identity" -> Color(0xFF42A5F5) // Blue
            "financial" -> Color(0xFF66BB6A) // Green
            "health" -> Color(0xFFEF5350) // Red
            "education" -> Color(0xFFAB47BC) // Purple
            "legal" -> Color(0xFFFFCA28) // Orange/Amber
            else -> Color(0xFF8D6E63) // Brown/Grey
        }
    }

    val getColorForFileType: (String) -> Color = { mime ->
        val m = mime.lowercase(Locale.ROOT)
        when {
            m.startsWith("image/") -> Color(0xFF42A5F5) // Blue
            m.contains("pdf") -> Color(0xFFEF5350) // Red
            m.contains("text") -> Color(0xFF66BB6A) // Green
            m.contains("word") || m.contains("docx") -> Color(0xFFAB47BC) // Purple
            m.contains("spreadsheet") || m.contains("excel") || m.contains("xls") -> Color(0xFFFFCA28) // Amber
            else -> Color(0xFF78909C) // Blue Grey
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
            .testTag("storage_usage_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = "Storage Analytics",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Storage Analytics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (viewByCategories) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { viewByCategories = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Categories",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewByCategories) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (!viewByCategories) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { viewByCategories = false }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "File Types",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!viewByCategories) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val limitStr = viewModel.formatFileSize(virtualLimit)
            val usedStr = viewModel.formatFileSize(totalSize)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "$usedStr of $limitStr Used",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val percentUsed = if (virtualLimit > 0) ((totalSize.toFloat() / virtualLimit) * 100).coerceAtMost(100f) else 0f
                Text(
                    text = String.format("%.1f%%", percentUsed),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val slices = remember(documents, viewByCategories) {
                if (documents.isEmpty()) {
                    emptyList()
                } else if (viewByCategories) {
                    val grouped = documents.groupBy { it.category }
                    grouped.map { (cat, docs) ->
                        val size = docs.sumOf { it.fileSize }
                        val pct = if (totalSize > 0) size.toFloat() / totalSize else 0f
                        StorageSlice(
                            label = cat,
                            size = size,
                            percentage = pct,
                            color = getColorForCategory(cat)
                        )
                    }.sortedByDescending { it.size }
                } else {
                    val grouped = documents.groupBy { getFileTypeDescription(it.mimeType) }
                    grouped.map { (type, docs) ->
                        val size = docs.sumOf { it.fileSize }
                        val pct = if (totalSize > 0) size.toFloat() / totalSize else 0f
                        StorageSlice(
                            label = type,
                            size = size,
                            percentage = pct,
                            color = getColorForFileType(docs.firstOrNull()?.mimeType ?: "")
                        )
                    }.sortedByDescending { it.size }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (slices.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        )
                    } else {
                        slices.forEach { slice ->
                            if (slice.percentage > 0.005f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(slice.percentage)
                                        .background(slice.color)
                                )
                                Spacer(modifier = Modifier.width(1.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (slices.isEmpty()) {
                Text(
                    text = "No documents available for storage analysis.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    slices.forEach { slice ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(slice.color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = slice.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = viewModel.formatFileSize(slice.size),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = String.format("(%.1f%%)", slice.percentage * 100),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkImportDialog(
    viewModel: VaultViewModel,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedFiles by remember { mutableStateOf<List<BulkImportFile>>(emptyList()) }
    var isImporting by remember { mutableStateOf(false) }
    var currentImportIndex by remember { mutableStateOf(0) }
    var currentImportingName by remember { mutableStateOf("") }
    
    val categories by viewModel.allCategories.collectAsState()
    
    val multipleFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newList = uris.map { uri ->
                val resolvedName = viewModel.getFileNameFromUri(uri) ?: "document_${System.currentTimeMillis()}.txt"
                val cleanName = resolvedName.substringBeforeLast(".")
                val (inferredCat, inferredTags) = inferCategoryAndTagsFromUri(uri, resolvedName)
                
                BulkImportFile(
                    uri = uri,
                    originalName = resolvedName,
                    name = cleanName,
                    category = inferredCat,
                    tags = inferredTags,
                    notes = ""
                )
            }
            selectedFiles = selectedFiles + newList
        }
    }
    
    Dialog(
        onDismissRequest = { if (!isImporting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Smart Bulk Import",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (!isImporting) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Select multiple files from your device. Arkiv will automatically detect categories and tags using smart naming conventions & path folder rules.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { multipleFilePickerLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth().testTag("bulk_select_files_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    enabled = !isImporting
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Local Files")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isImporting) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Importing file ${currentImportIndex + 1} of ${selectedFiles.size}...",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = currentImportingName,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (selectedFiles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No files selected yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize().testTag("bulk_import_list")
                        ) {
                            items(selectedFiles) { fileItem ->
                                var docName by remember { mutableStateOf(fileItem.name) }
                                var docCat by remember { mutableStateOf(fileItem.category) }
                                var docTags by remember { mutableStateOf(fileItem.tags) }
                                var docNotes by remember { mutableStateOf(fileItem.notes) }
                                var isExpanded by remember { mutableStateOf(false) }
                                var expandCatDrop by remember { mutableStateOf(false) }
                                
                                LaunchedEffect(docName, docCat, docTags, docNotes) {
                                    fileItem.name = docName
                                    fileItem.category = docCat
                                    fileItem.tags = docTags
                                    fileItem.notes = docNotes
                                }
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("bulk_item_${fileItem.originalName}"),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(
                                                    imageVector = Icons.Default.Description,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = fileItem.originalName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        SuggestionChip(
                                                            onClick = {},
                                                            label = { Text(docCat, fontSize = 9.sp) },
                                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                            ),
                                                            modifier = Modifier.height(20.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "Click Edit to customize",
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            Row {
                                                IconButton(onClick = { isExpanded = !isExpanded }) {
                                                    Icon(
                                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.Edit,
                                                        contentDescription = "Expand/Edit details",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { selectedFiles = selectedFiles.filter { it != fileItem } },
                                                    enabled = !isImporting
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Remove from list",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                        
                                        if (isExpanded) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            OutlinedTextField(
                                                value = docName,
                                                onValueChange = { docName = it },
                                                label = { Text("Display Name") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                enabled = !isImporting
                                            )
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                OutlinedButton(
                                                    onClick = { expandCatDrop = true },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    enabled = !isImporting
                                                ) {
                                                    Text("Category: $docCat")
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                                }
                                                DropdownMenu(
                                                    expanded = expandCatDrop,
                                                    onDismissRequest = { expandCatDrop = false }
                                                ) {
                                                    val nonNestedCats = categories.filter { it.parentId == null }.map { it.name }.distinct()
                                                    nonNestedCats.forEach { catName ->
                                                        DropdownMenuItem(
                                                            text = { Text(catName) },
                                                            onClick = {
                                                                docCat = catName
                                                                expandCatDrop = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            OutlinedTextField(
                                                value = docTags,
                                                onValueChange = { docTags = it },
                                                label = { Text("Tags (comma separated)") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                enabled = !isImporting
                                            )
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            OutlinedTextField(
                                                value = docNotes,
                                                onValueChange = { docNotes = it },
                                                label = { Text("Notes (Optional)") },
                                                modifier = Modifier.fillMaxWidth(),
                                                maxLines = 2,
                                                enabled = !isImporting
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isImporting
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedFiles.isEmpty()) {
                                Toast.makeText(context, "No files selected to import.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isImporting = true
                            scope.launch {
                                for (i in selectedFiles.indices) {
                                    currentImportIndex = i
                                    val f = selectedFiles[i]
                                    currentImportingName = f.name
                                    
                                    val doneChannel = kotlinx.coroutines.CompletableDeferred<Boolean>()
                                    viewModel.addNewDocument(
                                        name = f.name,
                                        category = f.category,
                                        selectedUri = f.uri,
                                        parentFolderId = viewModel.currentCategoryFolderId,
                                        tags = f.tags,
                                        notes = f.notes,
                                        useAIOcrListner = false
                                    ) { success, _ ->
                                        doneChannel.complete(success)
                                    }
                                    doneChannel.await()
                                }
                                isImporting = false
                                Toast.makeText(context, "Successfully bulk imported ${selectedFiles.size} records!", Toast.LENGTH_LONG).show()
                                onComplete()
                            }
                        },
                        enabled = selectedFiles.isNotEmpty() && !isImporting,
                        modifier = Modifier.testTag("bulk_import_submit_button")
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import All (${selectedFiles.size})")
                    }
                }
            }
        }
    }
}

data class BulkImportFile(
    val uri: Uri,
    val originalName: String,
    var name: String,
    var category: String,
    var tags: String,
    var notes: String
)

fun inferCategoryAndTagsFromFileName(fileName: String): Pair<String, String> {
    val f = fileName.lowercase(Locale.ROOT)
    return when {
        f.contains("invoice") || f.contains("receipt") || f.contains("bank") || f.contains("tax") || 
        f.contains("bill") || f.contains("statement") || f.contains("salary") || f.contains("payment") || 
        f.contains("transaction") || f.contains("expense") || f.contains("salarystatement") -> {
            Pair("Financial", "Finance, Statement, BulkImport")
        }
        f.contains("aadhaar") || f.contains("pan") || f.contains("passport") || f.contains("id") || 
        f.contains("license") || f.contains("voter") || f.contains("visa") || f.contains("uidai") -> {
            Pair("Identity", "Identity, ID, BulkImport")
        }
        f.contains("health") || f.contains("medical") || f.contains("prescription") || f.contains("report") || 
        f.contains("lab") || f.contains("hospital") || f.contains("doctor") || f.contains("vaccine") -> {
            Pair("Health", "Medical, Health, BulkImport")
        }
        f.contains("resume") || f.contains("cert") || f.contains("degree") || f.contains("diploma") || 
        f.contains("mark") || f.contains("academic") || f.contains("school") || f.contains("college") -> {
            Pair("Education", "Education, Academic, BulkImport")
        }
        f.contains("agreement") || f.contains("contract") || f.contains("lease") || f.contains("legal") || 
        f.contains("policy") || f.contains("will") || f.contains("deed") || f.contains("affidavit") -> {
            Pair("Legal", "Legal, Contract, BulkImport")
        }
        else -> {
            Pair("Miscellaneous", "Unsorted, BulkImport")
        }
    }
}

fun inferCategoryAndTagsFromUri(uri: Uri, fileName: String): Pair<String, String> {
    val path = uri.path?.lowercase(Locale.ROOT) ?: ""
    val (nameCat, nameTags) = inferCategoryAndTagsFromFileName(fileName)
    if (nameCat != "Miscellaneous") {
        return Pair(nameCat, nameTags)
    }
    return when {
        path.contains("finance") || path.contains("bank") || path.contains("tax") || path.contains("salary") -> 
            Pair("Financial", "Finance, Statement, PathImport")
        path.contains("id") || path.contains("identity") || path.contains("passport") || path.contains("license") -> 
            Pair("Identity", "Identity, ID, PathImport")
        path.contains("health") || path.contains("medical") || path.contains("prescription") -> 
            Pair("Health", "Medical, Health, PathImport")
        path.contains("education") || path.contains("academic") || path.contains("school") -> 
            Pair("Education", "Education, Academic, PathImport")
        path.contains("legal") || path.contains("agreement") || path.contains("contract") -> 
            Pair("Legal", "Legal, Contract, PathImport")
        else -> 
            Pair("Miscellaneous", "Unsorted, PathImport")
    }
}

@Composable
fun DocumentThumbnail(
    doc: DocumentEntity,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 46.dp
) {
    val context = LocalContext.current
    var pdfBitmap by remember(doc.localUri) { mutableStateOf<Bitmap?>(null) }
    
    val isPdf = doc.mimeType == "application/pdf"
    val isImage = doc.mimeType.startsWith("image/", ignoreCase = true)
    
    if (isPdf) {
        LaunchedEffect(doc.localUri) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val file = com.example.utils.AESCryptUtils.getDecryptedFile(context, doc.localUri)
                    if (file.exists()) {
                        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                        val renderer = PdfRenderer(pfd)
                        if (renderer.pageCount > 0) {
                            renderer.openPage(0).use { page ->
                                // For thumbnail, keep width and height small (e.g. 120px) to conserve memory
                                val width = 120
                                val height = (page.height.toFloat() / page.width.toFloat() * width).toInt().coerceIn(80, 180)
                                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(bitmap)
                                canvas.drawColor(android.graphics.Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                pdfBitmap = bitmap
                            }
                        }
                        renderer.close()
                    }
                } catch (e: Exception) {
                    // fallback to PDF icon
                }
            }
        }
    }
    
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(if (size > 50.dp) 12.dp else 8.dp))
            .background(
                when {
                    isImage -> MaterialTheme.colorScheme.surfaceVariant
                    isPdf -> Color(0xFFFFEBEE) // PDF red tint background
                    doc.mimeType.contains("word") || doc.mimeType.contains("officedocument") -> Color(0xFFE3F2FD) // Word blue tint
                    doc.mimeType.contains("excel") || doc.mimeType.contains("spreadsheet") -> Color(0xFFE8F5E9) // Excel green tint
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            isImage -> {
                val imageFile = remember(doc) { com.example.utils.AESCryptUtils.getDecryptedFile(context, doc.localUri) }
                AsyncImage(
                    model = imageFile,
                    contentDescription = "Image thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            isPdf && pdfBitmap != null -> {
                Image(
                    bitmap = pdfBitmap!!.asImageBitmap(),
                    contentDescription = "PDF thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                // Fallback standard icon
                Icon(
                    imageVector = getIconForMime(doc.mimeType),
                    contentDescription = null,
                    tint = when {
                        isPdf -> Color(0xFFC62828) // red PDF icon
                        doc.mimeType.contains("word") || doc.mimeType.contains("officedocument") -> Color(0xFF1565C0) // blue Word icon
                        doc.mimeType.contains("excel") || doc.mimeType.contains("spreadsheet") -> Color(0xFF2E7D32) // green Excel icon
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(if (size > 50.dp) 32.dp else if (size < 40.dp) 18.dp else 24.dp)
                )
            }
        }
    }
}

@Composable
fun FriendlyEmptyState(
    onAddDocumentClick: () -> Unit,
    onBulkImportClick: () -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Image Hero Visual
        Card(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(28.dp))
                .testTag("empty_state_hero_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_vault),
                contentDescription = "Secure Empty Vault",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Headline
        Text(
            text = "Welcome to your Secure Vault",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Explanation body text
        Text(
            text = "Arkiv is your completely offline, highly secure digital archive. Digitize and index your critical files—like identity cards, tax forms, receipts, or medical records—completely local on your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Quick Actions Grid (Cards)
        Text(
            text = "How would you like to start?",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
        )

        // Upload File Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAddDocumentClick() }
                .testTag("empty_state_upload_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add document",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Add a Document File",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Import a single PDF, word, image, or text file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Smart Bulk Import Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBulkImportClick() }
                .testTag("empty_state_bulk_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Bulk smart import",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Smart Bulk Import",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Import multiple files. Auto-detect categories & tags.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scanner Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onScanClick() }
                .testTag("empty_state_scan_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Scan with camera",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scan with Camera",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "Use your camera to capture physical documents.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealtimeSearchInputComponent(
    viewModel: VaultViewModel,
    placeholderText: String = "Search documents by title or tags...",
    onViewDoc: (DocumentEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf(viewModel.globalSearchQuery) }
    
    // Sync local state with ViewModel global state
    LaunchedEffect(viewModel.globalSearchQuery) {
        if (viewModel.globalSearchQuery != searchQuery) {
            searchQuery = viewModel.globalSearchQuery
        }
    }
    
    LaunchedEffect(searchQuery, viewModel.globalSearchScope) {
        viewModel.globalSearchQuery = searchQuery
        if (viewModel.globalSearchScope == SearchScope.SEMANTIC) {
            kotlinx.coroutines.delay(400)
            viewModel.performSemanticSearch(searchQuery)
        }
    }

    val documents by viewModel.allActiveDocuments.collectAsState()

    // Get matching suggestions
    val matchingTitles = remember(searchQuery, documents) {
        if (searchQuery.isBlank()) emptyList<DocumentEntity>()
        else {
            documents.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.fileName.contains(searchQuery, ignoreCase = true)
            }.take(5)
        }
    }

    val matchingTags = remember(searchQuery, documents) {
        if (searchQuery.isBlank()) emptyList<String>()
        else {
            documents.flatMap { doc ->
                doc.tags.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && it.contains(searchQuery, ignoreCase = true) }
            }.distinct().take(5)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        // 1. Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(placeholderText) },
            leadingIcon = { 
                Icon(
                    imageVector = Icons.Default.Search, 
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary
                ) 
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("realtime_search_input_field"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Real-time Search Scope Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Scope:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp)
            )

            SearchScope.values().forEach { scope ->
                val selected = viewModel.globalSearchScope == scope
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.globalSearchScope = scope },
                    label = {
                        Text(
                            text = when (scope) {
                                SearchScope.ALL -> "All Fields"
                                SearchScope.TITLE -> "Title Only"
                                SearchScope.TAGS -> "Tags Only"
                                SearchScope.SEMANTIC -> "Semantic (AI)"
                            },
                            fontSize = 11.sp
                        )
                    },
                    modifier = Modifier.testTag("search_scope_chip_${scope.name.lowercase()}"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        if (viewModel.globalSearchScope == SearchScope.SEMANTIC) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                if (viewModel.isSemanticSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI is analyzing meaning of documents...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (viewModel.semanticSearchError != null) viewModel.semanticSearchError!! else "AI Semantic Search Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // 3. Suggestions Dropdown Panel (Only visible when typing and suggestions exist)
        AnimatedVisibility(
            visible = searchQuery.isNotEmpty() && (matchingTitles.isNotEmpty() || matchingTags.isNotEmpty()),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    if (matchingTitles.isNotEmpty()) {
                        Text(
                            text = "SUGGESTED TITLES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                        matchingTitles.forEach { doc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        searchQuery = doc.name
                                    }
                                    .padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = doc.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = { onViewDoc(doc) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = "Quick view details",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (matchingTitles.isNotEmpty() && matchingTags.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )
                    }

                    if (matchingTags.isNotEmpty()) {
                        Text(
                            text = "SUGGESTED TAGS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            matchingTags.forEach { tag ->
                                InputChip(
                                    selected = viewModel.selectedTagsFilter.contains(tag),
                                    onClick = {
                                        if (viewModel.selectedTagsFilter.contains(tag)) {
                                            viewModel.selectedTagsFilter = viewModel.selectedTagsFilter - tag
                                        } else {
                                            viewModel.selectedTagsFilter = viewModel.selectedTagsFilter + tag
                                        }
                                        searchQuery = "" // Clear query to focus on the selected tag
                                    },
                                    label = { Text("#$tag") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Label,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("suggested_tag_chip_$tag")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}




