@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
package com.organize.photos.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.organize.photos.logic.AdvancedSearchConfig
import com.organize.photos.logic.AdvancedSearchEngine
import com.organize.photos.logic.PhotoScanner
import com.organize.photos.logic.ScanFilters
import com.organize.photos.logic.SearchService
import com.organize.photos.logic.SortOrder
import com.organize.photos.logic.SortService
import com.organize.photos.logic.ThumbnailCache
import com.organize.photos.logic.ThumbnailGenerator
import com.organize.photos.logic.UserMetadata
import com.organize.photos.logic.UserMetadataManager
import com.organize.photos.model.PhotoItem
import com.organize.photos.preview.PreviewData
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import com.organize.photos.image.decodeImageBitmap

@Composable
fun App(
    photoScanner: PhotoScanner,
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
    var sortOrder by rememberSaveable { mutableStateOf(SortOrder.FILE_NAME_ASC) }
    var photos by remember { mutableStateOf(initialItems) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedPhotoForView by remember { mutableStateOf<PhotoItem?>(null) }
    var isAdvancedPanelExpanded by rememberSaveable { mutableStateOf(false) }
    var advancedSearchConfig by remember { mutableStateOf(AdvancedSearchConfig()) }
    val searchService = remember { SearchService() }
    val advancedSearchEngine = remember { AdvancedSearchEngine() }
    val thumbnailCache = remember { ThumbnailCache(maxItems = 256) }
    val gridState = rememberLazyGridState()
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

    val hasActiveAdvancedFilter = remember(advancedSearchConfig) {
        advancedSearchConfig.fieldFilters.values.any { it.selectedValues.isNotEmpty() }
    }

    val filtered = remember(query, selectedExtensions, photos, searchMode, hasActiveAdvancedFilter, advancedSearchConfig, sortOrder) {
        var result = photos
        
        // 簡易検索を適用（入力がある場合）
        if (query.isNotBlank()) {
            result = searchService.filter(result, query, selectedExtensions, dateRange = null, searchMode = searchMode)
        }
        
        // 撮影情報フィルターを適用（展開されている場合）
        if (hasActiveAdvancedFilter) {
            result = advancedSearchEngine.filter(result, advancedSearchConfig)
        }
        
        // 拡張子フィルタは常に適用
        result = result.filter { selectedExtensions.isEmpty() || selectedExtensions.contains(it.extension.lowercase()) }
        
        // 並べ替えを適用
        SortService.sort(result, sortOrder)
    }
    
    // 並べ替え時にスクロール位置をトップに戻す
    LaunchedEffect(sortOrder) {
        gridState.scrollToItem(0)
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Organize",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (openFolderPicker != null) {
                            Button(
                                onClick = {
                                    val picked = openFolderPicker.invoke()
                                    if (!picked.isNullOrBlank()) {
                                        selectedFolder = picked
                                        triggerScan(picked)
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier.height(36.dp)
                            ) { Text("選択", style = MaterialTheme.typography.labelSmall) }
                        }
                        TextButton(
                            onClick = { triggerScan(selectedFolder) },
                            enabled = selectedFolder.isNotBlank() && !isLoading,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("再スキャン", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
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
            
            // 並べ替えドロップダウン
            SortRow(
                sortOrder = sortOrder,
                onSortOrderChange = { sortOrder = it }
            )
            
            // 撮影情報フィルタートグル
            TextButton(
                onClick = { isAdvancedPanelExpanded = !isAdvancedPanelExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    if (isAdvancedPanelExpanded) "撮影情報フィルターをたたむ ▲" else "撮影情報フィルターを開く ▼",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            
            // 撮影情報フィルターパネル（展開時）
            if (isAdvancedPanelExpanded) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    AdvancedSearchPanel(
                        photos = photos,
                        config = advancedSearchConfig,
                        onConfigChange = { advancedSearchConfig = it }
                    )
                }
            }


            if (filtered.isEmpty()) {
                EmptyState()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 180.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier.fillMaxSize(),
                    state = gridState
                ) {
                    items(filtered, key = { it.id }) { photo ->
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
    
    // Image viewer dialog
    selectedPhotoForView?.let { photo ->
        ImageViewerDialog(
            photo = photo,
            onDismiss = { selectedPhotoForView = null },
            onSave = { updated ->
                photos = photos.map { if (it.id == updated.id) updated else it }
                selectedPhotoForView = null
            }
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
            Text("検索モード:", style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = { onSearchModeChange(SearchService.SearchMode.OR) },
                modifier = Modifier.height(40.dp),
                colors = if (searchMode == SearchService.SearchMode.OR) {
                    androidx.compose.material3.ButtonDefaults.buttonColors()
                } else {
                    androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                }
            ) { Text("OR", style = MaterialTheme.typography.labelLarge) }
            Button(
                onClick = { onSearchModeChange(SearchService.SearchMode.AND) },
                modifier = Modifier.height(40.dp),
                colors = if (searchMode == SearchService.SearchMode.AND) {
                    androidx.compose.material3.ButtonDefaults.buttonColors()
                } else {
                    androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                }
            ) { Text("AND", style = MaterialTheme.typography.labelLarge) }
        }

        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("jpg", "jpeg", "png", "heic", "tif", "tiff").forEach { ext ->
                val selected = selectedExtensions.contains(ext)
                AssistChip(
                    onClick = { onToggleExtension(ext) },
                    label = { Text(ext.uppercase(), style = MaterialTheme.typography.labelMedium) },
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
private fun SortRow(
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("並べ替え:", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(80.dp))
        
        Box(modifier = Modifier.weight(1f)) {
            Button(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(sortOrder.displayName, style = MaterialTheme.typography.bodyMedium)
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                SortOrder.values().forEach { order ->
                    DropdownMenuItem(
                        text = { Text(order.displayName) },
                        onClick = {
                            onSortOrderChange(order)
                            expanded = false
                        }
                    )
                }
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
    val density = LocalDensity.current

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium)
                .padding(12.dp)
                .combinedClickable(
                    onClick = {},
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
            onDismissRequest = { showContextMenu = false },
            offset = with(density) { DpOffset(menuOffset.x.toDp(), menuOffset.y.toDp()) },
            properties = PopupProperties(focusable = true)
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
}

@Composable
private fun ImageViewerDialog(
    photo: PhotoItem,
    onDismiss: () -> Unit,
    onSave: (PhotoItem) -> Unit,
) {
    var title by remember { mutableStateOf(photo.title) }
    var tagsInput by remember { mutableStateOf(photo.tags.joinToString(", ")) }
    var comment by remember { mutableStateOf(photo.comment) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxSize(0.9f),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
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
                
                // ✨ ユーザーメタデータ編集セクション
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text("📝 編集可能情報", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("タイトル") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = tagsInput,
                        onValueChange = { tagsInput = it },
                        label = { Text("タグ（カンマ区切り）") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("コメント") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(vertical = 8.dp),
                        maxLines = 4
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val userMetadata = UserMetadata(
                                    title = title,
                                    tags = tagsInput
                                        .split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() },
                                    comment = comment
                                )
                                UserMetadataManager.setUserMetadata(
                                    photo.absolutePath,
                                    userMetadata
                                )
                                // UI上のPhotoItemも即更新
                                val updatedPhoto = photo.copy(
                                    title = userMetadata.title,
                                    tags = userMetadata.tags,
                                    comment = userMetadata.comment
                                )
                                onSave(updatedPhoto)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💾 保存")
                        }
                        
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("キャンセル")
                        }
                    }
                }
                
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text("ファイル情報", style = MaterialTheme.typography.titleMedium)
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

