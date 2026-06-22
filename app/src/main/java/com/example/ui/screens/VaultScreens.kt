package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.example.data.CategoryEntity
import com.example.data.DocumentEntity
import com.example.ui.theme.VerifiedGreen
import com.example.ui.theme.SyncingBlue
import com.example.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Top Navigation state
enum class VaultTab {
    HOME, DOCUMENTS, FOLDERS, SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigator(viewModel: VaultViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(VaultTab.HOME) }
    var selectedPreviewDoc by remember { mutableStateOf<DocumentEntity?>(null) }
    var showUploadDialog by remember { mutableStateOf(false) }

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
            // Screen switching
            when (currentTab) {
                VaultTab.HOME -> DashboardScreen(
                    viewModel = viewModel,
                    onViewDoc = { selectedPreviewDoc = it },
                    onNavigateToDocs = { currentTab = VaultTab.DOCUMENTS }
                )
                VaultTab.DOCUMENTS -> DocumentsScreen(
                    viewModel = viewModel,
                    onViewDoc = { selectedPreviewDoc = it }
                )
                VaultTab.FOLDERS -> FoldersScreen(
                    viewModel = viewModel,
                    onViewDoc = { selectedPreviewDoc = it }
                )
                VaultTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }

            // Upload prompt Dialog Overlay
            if (showUploadDialog) {
                UploadDocumentDialog(
                    viewModel = viewModel,
                    onDismiss = { showUploadDialog = false },
                    onComplete = {
                        showUploadDialog = false
                    }
                )
            }

            // Preview Sheet overlay
            selectedPreviewDoc?.let { doc ->
                DocumentDetailDialog(
                    doc = doc,
                    viewModel = viewModel,
                    onOpenInAppViewer = { docToView ->
                        selectedPreviewDoc = null
                        viewModel.activeInAppViewerDoc = docToView
                    },
                    onDismiss = { selectedPreviewDoc = null }
                )
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
    onNavigateToDocs: () -> Unit
) {
    val documents by viewModel.allActiveDocuments.collectAsState()
    val scrollState = rememberScrollState()
    var searchQuery by remember { mutableStateOf("") }

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
        }

        // Quick Search Bar (Case-insensitive matching across name, fileName, tags, notes, type)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Quick search documents...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .testTag("dashboard_search_bar"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )

        if (searchQuery.isNotEmpty()) {
            Text(
                text = "Search Results",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            val filteredSearchDocs = documents.filter { doc ->
                doc.name.contains(searchQuery, ignoreCase = true) ||
                doc.fileName.contains(searchQuery, ignoreCase = true) ||
                doc.ocrText.contains(searchQuery, ignoreCase = true) ||
                doc.tags.contains(searchQuery, ignoreCase = true) ||
                doc.notes.contains(searchQuery, ignoreCase = true) ||
                getFileTypeDescription(doc.mimeType).contains(searchQuery, ignoreCase = true)
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
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getIconForMime(doc.mimeType),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
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
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            MaterialTheme.colorScheme.secondaryContainer,
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getIconForMime(doc.mimeType),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
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
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getIconForMime(doc.mimeType),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DocumentsScreen(
    viewModel: VaultViewModel,
    onViewDoc: (DocumentEntity) -> Unit
) {
    val documents by viewModel.allActiveDocuments.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var currentSortBy by remember { mutableStateOf("Date Added") }
    var selectedDocIds by remember { mutableStateOf(setOf<Long>()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
            doc.name.contains(searchQuery, ignoreCase = true) ||
                    doc.fileName.contains(searchQuery, ignoreCase = true) ||
                    doc.ocrText.contains(searchQuery, ignoreCase = true) ||
                    doc.tags.contains(searchQuery, ignoreCase = true) ||
                    doc.notes.contains(searchQuery, ignoreCase = true) ||
                    getFileTypeDescription(doc.mimeType).contains(searchQuery, ignoreCase = true)
        }

        val matchesTag = if (viewModel.selectedTagFilter == null) {
            true
        } else {
            doc.tags.contains(viewModel.selectedTagFilter!!, ignoreCase = true)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // 1. Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search all documents by name, ocr, tag...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("document_search_bar"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )

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
                                val docsToExport = sortedDocs.filter { selectedDocIds.contains(it.id) }
                                scope.launch {
                                    var count = 0
                                    docsToExport.forEach { doc ->
                                        val res = viewModel.repository.downloadDocumentToPublicDownloads(doc)
                                        if (res.isSuccess) count++
                                    }
                                    Toast.makeText(context, "Exported $count of ${docsToExport.size} files to device Downloads folder!", Toast.LENGTH_LONG).show()
                                    selectedDocIds = emptySet()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", fontSize = 11.sp)
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No documents found fitting category criteria.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getIconForMime(doc.mimeType),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
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

    var searchQuery by remember { mutableStateOf("") }
    var showCustomCategoryDialog by remember { mutableStateOf(false) }
    var showManageCategoryDialog by remember { mutableStateOf(false) }
    var selectedDocIds by remember { mutableStateOf(setOf<Long>()) }

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
    val filteredFolderDocs = remember(folderDocs, searchQuery) {
        if (searchQuery.isEmpty()) {
            folderDocs
        } else {
            folderDocs.filter { doc ->
                doc.name.contains(searchQuery, ignoreCase = true) ||
                        doc.fileName.contains(searchQuery, ignoreCase = true) ||
                        doc.ocrText.contains(searchQuery, ignoreCase = true) ||
                        doc.tags.contains(searchQuery, ignoreCase = true) ||
                        getFileTypeDescription(doc.mimeType).contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 1. Directory Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(if (currentSubfolderId == null) "Search folders..." else "Search files in this folder...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("folder_search_bar"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )

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
                                    val docsToExport = filteredFolderDocs.filter { selectedDocIds.contains(it.id) }
                                    scope.launch {
                                        var count = 0
                                        docsToExport.forEach { doc ->
                                            val res = viewModel.repository.downloadDocumentToPublicDownloads(doc)
                                            if (res.isSuccess) count++
                                        }
                                        Toast.makeText(context, "Exported $count of ${docsToExport.size} files to device Downloads folder!", Toast.LENGTH_LONG).show()
                                        selectedDocIds = emptySet()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export", fontSize = 11.sp)
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
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
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
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getIconForMime(doc.mimeType),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
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
    var searchTextQuery by remember { mutableStateOf("") }
    var currentPageIndex by remember { mutableStateOf(0) }

    val file = remember(doc) { File(doc.localUri) }

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
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getIconForMime(doc.mimeType),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }

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

                if (selectedTab == 0) {
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
                                    scope.launch {
                                        viewModel.repository.insertCategory(
                                            CategoryEntity(
                                                name = newCatName.trim(),
                                                parentId = selectedParentId,
                                                isDefault = false
                                            )
                                        )
                                        newCatName = ""
                                        Toast.makeText(context, "Folder created!", Toast.LENGTH_SHORT).show()
                                    }
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
                                                    scope.launch {
                                                        viewModel.repository.insertCategory(
                                                            folder.copy(name = editingFolderName.trim())
                                                        )
                                                        editingFolderId = null
                                                        Toast.makeText(context, "Renamed successfully!", Toast.LENGTH_SHORT).show()
                                                    }
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
                                                    scope.launch {
                                                        viewModel.repository.deleteCategoryById(folder.id)
                                                        Toast.makeText(context, "Folder deleted.", Toast.LENGTH_SHORT).show()
                                                    }
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
    onComplete: () -> Unit
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

                Spacer(modifier = Modifier.height(16.dp))

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
fun DocumentDetailDialog(
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
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
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
                            modifier = Modifier.fillMaxWidth(),
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
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = editNotes,
                            onValueChange = { editNotes = it },
                            label = { Text("Additional Notes (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isEditing = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
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
                                Text("Save Settings")
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
                            AsyncImage(
                                model = File(doc.localUri),
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
                                    Text(
                                        text = doc.ocrText.ifEmpty { "No index contents saved in database. Click here or use View button to open." },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Detail Metrics (Showing simplified File Type instead of MIME type, security pin row completely removed)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DetailRow(label = "Category", value = doc.category)
                        DetailRow(label = "File Type", value = getFileTypeDescription(doc.mimeType))
                        DetailRow(label = "File Size", value = viewModel.formatFileSize(doc.fileSize))
                        if (doc.tags.isNotEmpty()) {
                            DetailRow(label = "Tags", value = doc.tags)
                        }
                        if (doc.notes.isNotEmpty()) {
                            DetailRow(label = "Personal Notes", value = doc.notes)
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
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("In-App Viewer", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { shareDocument(context, doc) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", fontSize = 12.sp)
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
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), modifier = Modifier.padding(top = 4.dp))
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
    val file = File(doc.localUri)
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
    val file = File(doc.localUri)
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
