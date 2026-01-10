@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
package com.organize.photos.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.organize.photos.logic.AdvancedSearchConfig
import com.organize.photos.logic.AdvancedSearchEngine
import com.organize.photos.logic.FieldAnalyzer
import com.organize.photos.logic.FieldFilter
import com.organize.photos.logic.SearchFieldCategory
import com.organize.photos.logic.SearchService
import com.organize.photos.model.PhotoItem

/**
 * 詳細検索パネル
 */
@Composable
fun AdvancedSearchPanel(
    photos: List<PhotoItem>,
    onConfigChange: (AdvancedSearchConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var config by remember { mutableStateOf(AdvancedSearchConfig()) }
    var expandedCategories by remember { mutableStateOf(setOf<SearchFieldCategory>()) }
    
    val availableValues = remember(photos) { FieldAnalyzer.extractAvailableValues(photos) }
    val definedFields = remember { FieldAnalyzer.getDefinedFields() }
    val fieldsByCategory = remember(definedFields) {
        definedFields.groupBy { it.category }
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // ヘッダー
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "詳細検索",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            
            // 検索モード切り替え（フィールド間）
            Text("フィールド間:", style = MaterialTheme.typography.labelSmall)
            FilterChip(
                selected = config.matchMode == SearchService.SearchMode.AND,
                onClick = {
                    config = config.copy(
                        matchMode = if (config.matchMode == SearchService.SearchMode.AND) {
                            SearchService.SearchMode.OR
                        } else {
                            SearchService.SearchMode.AND
                        }
                    )
                    onConfigChange(config)
                },
                label = {
                    Text(
                        if (config.matchMode == SearchService.SearchMode.AND) "AND" else "OR",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                modifier = Modifier.padding(start = 8.dp)
            )
            
            if (config.fieldFilters.isNotEmpty()) {
                IconButton(
                    onClick = {
                        config = AdvancedSearchConfig()
                        onConfigChange(config)
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "クリア")
                }
            }
        }
        
        Divider(modifier = Modifier.padding(vertical = 12.dp))
        
        // フィールド一覧
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(SearchFieldCategory.values()) { category ->
                val fieldsInCategory = fieldsByCategory[category] ?: emptyList()
                if (fieldsInCategory.isNotEmpty()) {
                    SearchFieldCategoryPanel(
                        category = category,
                        fields = fieldsInCategory,
                        availableValues = availableValues,
                        selectedFilters = config.fieldFilters,
                        isExpanded = category in expandedCategories,
                        onExpandChange = { expanded ->
                            expandedCategories = if (expanded) {
                                expandedCategories + category
                            } else {
                                expandedCategories - category
                            }
                        },
                        onFilterChange = { fieldKey, filter ->
                            val newFilters = config.fieldFilters.toMutableMap()
                            if (filter.selectedValues.isEmpty()) {
                                newFilters.remove(fieldKey)
                            } else {
                                newFilters[fieldKey] = filter
                            }
                            config = config.copy(fieldFilters = newFilters)
                            onConfigChange(config)
                        }
                    )
                }
            }
        }
    }
}

/**
 * カテゴリ別フィールドパネル
 */
@Composable
private fun SearchFieldCategoryPanel(
    category: SearchFieldCategory,
    fields: List<com.organize.photos.logic.SearchField>,
    availableValues: Map<String, Set<String>>,
    selectedFilters: Map<String, FieldFilter>,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onFilterChange: (String, FieldFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // カテゴリヘッダー
        TextButton(
            onClick = { onExpandChange(!isExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            Text(
                categoryLabel(category),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Start
            )
            Text(
                if (isExpanded) "▼" else "▶",
                style = MaterialTheme.typography.labelSmall
            )
        }
        
        // フィールド一覧（展開時）
        if (isExpanded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    fields.forEach { field ->
                        SearchFieldRow(
                            field = field,
                            availableValues = availableValues[field.key] ?: emptySet(),
                            selectedFilter = selectedFilters[field.key],
                            onFilterChange = { filter ->
                                onFilterChange(field.key, filter)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * フィールド選択行
 */
@Composable
private fun SearchFieldRow(
    field: com.organize.photos.logic.SearchField,
    availableValues: Set<String>,
    selectedFilter: FieldFilter?,
    onFilterChange: (FieldFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var showValueSelector by remember { mutableStateOf(false) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        // フィールド名と選択ボタン
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    field.displayName,
                    style = MaterialTheme.typography.labelMedium
                )
                if (selectedFilter?.selectedValues?.isNotEmpty() == true) {
                    Text(
                        "${selectedFilter.selectedValues.size}個選択",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            TextButton(
                onClick = { showValueSelector = !showValueSelector },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("選択", style = MaterialTheme.typography.labelSmall)
            }
        }
        
        // 値セレクタ（展開時）
        if (showValueSelector && availableValues.isNotEmpty()) {
            ValueSelectorPanel(
                fieldKey = field.key,
                availableValues = availableValues.sorted(),
                selectedValues = selectedFilter?.selectedValues ?: emptySet(),
                searchMode = selectedFilter?.searchMode ?: SearchService.SearchMode.OR,
                onSelectionChange = { values, mode ->
                    onFilterChange(
                        FieldFilter(
                            field = field,
                            selectedValues = values,
                            searchMode = mode
                        )
                    )
                },
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )
        }
    }
}

/**
 * 値セレクタパネル
 */
@Composable
private fun ValueSelectorPanel(
    fieldKey: String,
    availableValues: List<String>,
    selectedValues: Set<String>,
    searchMode: SearchService.SearchMode,
    onSelectionChange: (Set<String>, SearchService.SearchMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // AND/OR トグル
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("マッチ:", style = MaterialTheme.typography.labelSmall)
                FilterChip(
                    selected = searchMode == SearchService.SearchMode.AND,
                    onClick = {
                        onSelectionChange(
                            selectedValues,
                            if (searchMode == SearchService.SearchMode.AND) {
                                SearchService.SearchMode.OR
                            } else {
                                SearchService.SearchMode.AND
                            }
                        )
                    },
                    label = {
                        Text(
                            if (searchMode == SearchService.SearchMode.AND) "AND" else "OR",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            Divider(modifier = Modifier.padding(bottom = 8.dp))
            
            // 値チェックボックス
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(availableValues) { value ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = value in selectedValues,
                            onCheckedChange = { checked ->
                                val newValues = if (checked) {
                                    selectedValues + value
                                } else {
                                    selectedValues - value
                                }
                                onSelectionChange(newValues, searchMode)
                            }
                        )
                        Text(
                            value,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun categoryLabel(category: SearchFieldCategory): String = when (category) {
    SearchFieldCategory.CAMERA -> "📷 カメラ情報"
    SearchFieldCategory.LENS -> "🔍 レンズ"
    SearchFieldCategory.EXPOSURE -> "⚙️ 撮影設定"
    SearchFieldCategory.FOCUS -> "👁️ フォーカス"
    SearchFieldCategory.GPS -> "📍 GPS"
    SearchFieldCategory.DATE -> "📅 撮影日時"
    SearchFieldCategory.IMAGE_INFO -> "🖼️ 画像情報"
    SearchFieldCategory.SOFTWARE -> "💾 ソフトウェア"
}
