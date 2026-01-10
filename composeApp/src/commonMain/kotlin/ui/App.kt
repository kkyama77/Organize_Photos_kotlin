@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
package com.organize.photos.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import com.organize.photos.logic.FakePhotoScanner
import com.organize.photos.logic.AdvancedSearchConfig
import com.organize.photos.logic.AdvancedSearchEngine
import com.organize.photos.logic.PhotoScanner
import com.organize.photos.logic.ScanFilters
import com.organize.photos.logic.SearchService
import com.organize.photos.logic.ThumbnailCache
import com.organize.photos.logic.ThumbnailGenerator
import com.organize.photos.model.PhotoItem
import com.organize.photos.preview.PreviewData
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.layout.ContentScale
import com.organize.photos.image.decodeImageBitmap

@Composable
fun App(
    photoScanner: PhotoScanner = FakePhotoScanner(),
    openFolderPicker: (() -> String?)? = null,
    initialItems: List<PhotoItem> = emptyList(),
    thumbnailGenerator: ThumbnailGenerator? = null,
    openWithDefaultApp: ((String) -> Unit)? = null,
) {
    MaterialTheme {
        PhotoGridScreen(photoScanner, openFolderPicker, initialItems, thumbnailGenerator, openWithDefaultApp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoGridScreen(
    photoScanner: PhotoScanner,
    openFolderPicker: (() -> String?)?,
    initialItems: List<PhotoItem>,
    thumbnailGenerator: ThumbnailGenerator?,
    openWithDefaultApp: ((String) -> Unit)?,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var searchMode by rememberSaveable { mutableStateOf(SearchService.SearchMode.OR) }
    var selectedExtensions by rememberSaveable { mutableStateOf(setOf("jpg", "jpeg", "png", "heic", "tif", "tiff")) }
    var selectedFolder by rememberSaveable { mutableStateOf("") }
    var photos by remember { mutableStateOf(initialItems) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedPhotoForView by remember { mutableStateOf<PhotoItem?>(null) }
    var useAdvancedSearch by rememberSaveable { mutableStateOf(false) }
    var advancedSearchConfig by remember { mutableStateOf(AdvancedSearchConfig()) }
    val searchService = remember { SearchService() }
    val advancedSearchEngine = remember { AdvancedSearchEngine() }
    val thumbnailCache = remember { ThumbnailCache(maxItems = 256) }
    val scope = rememberCoroutineScope()

    fun triggerScan(target: String) {
        if (target.isBlank()) return
        isLoading = true
        error = null
        scope.launch {
            val filters = ScanFilters(extensions = selectedExtensions)
            val result = runCatching { photoScanner.scan(target, filters) }
            result.onSuccess { photos = it }
                .onFailure { error = it.message ?: "Scan failed" }
            isLoading = false
        }
    }

    val filtered = remember(query, selectedExtensions, photos, searchMode, useAdvancedSearch, advancedSearchConfig) {
        var result = if (useAdvancedSearch) {
            advancedSearchEngine.filter(photos, advancedSearchConfig)
        } else {
            searchService.filter(photos, query, selectedExtensions, dateRange = null, searchMode = searchMode)
        }
        // 拡張子フィルタは常に適用
        result.filter { selectedExtensions.isEmpty() || selectedExtensions.contains(it.extension.lowercase()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Organize Photos") },
                actions = {
                    if (openFolderPicker != null) {
                        Button(
                            onClick = {
                                val picked = openFolderPicker.invoke()
                                if (!picked.isNullOrBlank()) {
                                    selectedFolder = picked
                                    triggerScan(picked)
                                }
                            },
                            enabled = !isLoading
                        ) { Text("フォルダ選択") }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TextButton(
                        onClick = { triggerScan(selectedFolder) },
                        enabled = selectedFolder.isNotBlank() && !isLoading
                    ) {
                        Text("再スキャン")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            StatusRow(selectedFolder = selectedFolder, error = error)

            FilterRow(
                query = query,
                onQueryChange = { query = it },
                searchMode = searchMode,
                onSearchModeChange = { searchMode = it },
                selectedExtensions = selectedExtensions,
                onToggleExtension = { ext ->
                    selectedExtensions = if (selectedExtensions.contains(ext)) {
                        selectedExtensions - ext
                    } else {
                        selectedExtensions + ext
                    }
                }
            )
            
            // 詳細検索トグル
            TextButton(
                onClick = { useAdvancedSearch = !useAdvancedSearch },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    if (useAdvancedSearch) "簡易検索に戻す ▲" else "詳細検索 ▼",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            
            // 詳細検索パネル（展開時）
            if (useAdvancedSearch) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    AdvancedSearchPanel(
                        photos = photos,
                        onConfigChange = { advancedSearchConfig = it }
                    )
                }
            }

            Crossfade(targetState = filtered) { items ->
                if (items.isEmpty()) {
                    EmptyState()
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items, key = { it.id }) { photo ->
                            PhotoCard(
                                item = photo,
                                thumbnailGenerator = thumbnailGenerator,
                                cache = thumbnailCache,
                                onDoubleClick = { 
                                    openWithDefaultApp?.invoke(photo.absolutePath)
                                },
                                onShowProperties = { selectedPhotoForView = photo }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Image viewer dialog
    selectedPhotoForView?.let { photo ->
        ImageViewerDialog(
            photo = photo,
            onDismiss = { selectedPhotoForView = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(
    query: String,
    onQueryChange: (String) -> Unit,
    searchMode: SearchService.SearchMode,
    onSearchModeChange: (SearchService.SearchMode) -> Unit,
    selectedExtensions: Set<String>,
    onToggleExtension: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("検索 (名前・メタデータ) ※複数キーワードは , で区切る") }
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("検索モード:", style = MaterialTheme.typography.labelSmall)
            Button(
                onClick = { onSearchModeChange(SearchService.SearchMode.OR) },
                modifier = Modifier.height(36.dp),
                colors = if (searchMode == SearchService.SearchMode.OR) {
                    androidx.compose.material3.ButtonDefaults.buttonColors()
                } else {
                    androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                }
            ) { Text("OR", style = MaterialTheme.typography.labelSmall) }
            Button(
                onClick = { onSearchModeChange(SearchService.SearchMode.AND) },
                modifier = Modifier.height(36.dp),
                colors = if (searchMode == SearchService.SearchMode.AND) {
                    androidx.compose.material3.ButtonDefaults.buttonColors()
                } else {
                    androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                }
            ) { Text("AND", style = MaterialTheme.typography.labelSmall) }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("jpg", "jpeg", "png", "heic", "tif", "tiff").forEach { ext ->
                val selected = selectedExtensions.contains(ext)
                AssistChip(
                    onClick = { onToggleExtension(ext) },
                    label = { Text(ext.uppercase()) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        labelColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

@Composable
private fun PhotoCard(
    item: PhotoItem,
    thumbnailGenerator: ThumbnailGenerator?,
    cache: ThumbnailCache,
    onDoubleClick: () -> Unit = {},
    onShowProperties: () -> Unit = {},
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E88E5), Color(0xFF42A5F5))
    )
    var thumbBytes by remember { mutableStateOf(item.thumbnail) }
    var thumbBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    LaunchedEffect(item.id) {
        if (thumbBytes == null && thumbnailGenerator != null) {
            val cached = cache.get(item.id)
            val bytes = cached ?: thumbnailGenerator.generate(item, maxSize = 256)?.also {
                cache.put(item, it)
            }
            thumbBytes = bytes
        }
        thumbBitmap = thumbBytes?.let { decodeImageBitmap(it) }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium)
            .padding(12.dp)
            .combinedClickable(
                onClick = { showContextMenu = true },
                onDoubleClick = onDoubleClick,
                onLongClick = { showContextMenu = true }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(gradient, shape = MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center
        ) {
            val bmp = thumbBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = item.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(text = item.extension.uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Text(text = item.displayName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Text(text = item.absolutePath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val resolution = listOfNotNull(item.width, item.height).takeIf { it.size == 2 }?.joinToString(" x ")
        resolution?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
        item.sizeBytes?.let { size ->
            Text(text = formatSize(size), style = MaterialTheme.typography.bodySmall)
        }
        item.capturedAt?.let { captured ->
            Text(text = "Captured: $captured", style = MaterialTheme.typography.bodySmall)
        }
    }
    
    // Context menu
    DropdownMenu(
        expanded = showContextMenu,
        onDismissRequest = { showContextMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("デフォルトアプリで開く") },
            onClick = {
                showContextMenu = false
                onDoubleClick()
            }
        )
        DropdownMenuItem(
            text = { Text("プロパティ") },
            onClick = {
                showContextMenu = false
                onShowProperties()
            }
        )
        DropdownMenuItem(
            text = { Text("パスをコピー") },
            onClick = {
                showContextMenu = false
                // TODO: Copy to clipboard
            }
        )
        DropdownMenuItem(
            text = { Text("削除") },
            onClick = {
                showContextMenu = false
                // TODO: Delete file
            }
        )
    }
}

@Composable
private fun ImageViewerDialog(
    photo: PhotoItem,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxSize(0.9f),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = photo.displayName,
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(onClick = onDismiss) {
                        Text("閉じる")
                    }
                }
                
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // TODO: Load full-size image
                    Text(
                        text = "画像: ${photo.absolutePath}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text("解像度: ${photo.width} x ${photo.height}", style = MaterialTheme.typography.bodySmall)
                    Text("サイズ: ${photo.sizeBytes?.let { formatSize(it) } ?: "不明"}", style = MaterialTheme.typography.bodySmall)
                    Text("パス: ${photo.absolutePath}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("メタデータ", style = MaterialTheme.typography.titleMedium)
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(200.dp).padding(top = 8.dp)
                    ) {
                        val entries = photo.metadata.entries.sortedBy { it.key }.take(50)
                        items(entries.size) { idx ->
                            val (key, value) = entries[idx]
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text(text = key, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = value, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusRow(selectedFolder: String, error: String?) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        if (selectedFolder.isNotBlank()) {
            Text(text = "選択フォルダ: $selectedFolder", style = MaterialTheme.typography.bodySmall)
        } else {
            Text(text = "フォルダを選択してスキャン", style = MaterialTheme.typography.bodySmall)
        }
        error?.let {
            Text(text = "エラー: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("画像がありません")
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value > 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    return String.format("%.1f %s", value, units[unitIndex])
}

