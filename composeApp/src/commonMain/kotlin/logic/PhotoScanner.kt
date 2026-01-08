package com.organize.photos.logic

import com.organize.photos.model.PhotoItem

interface PhotoScanner {
    suspend fun scan(root: String, filters: ScanFilters = ScanFilters()): List<PhotoItem>
}

class FakePhotoScanner : PhotoScanner {
    override suspend fun scan(root: String, filters: ScanFilters): List<PhotoItem> {
        // Placeholder: replace with real recursive scan + metadata extraction per platform
        return emptyList()
    }
}
