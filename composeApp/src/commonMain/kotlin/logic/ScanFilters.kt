package com.organize.photos.logic

import kotlinx.datetime.Instant

data class ScanFilters(
    val extensions: Set<String> = setOf("jpg", "jpeg", "png", "heic", "tif", "tiff"),
    val dateRange: ClosedRange<Instant>? = null,
)
