package com.organize.photos.logic

import com.organize.photos.model.PhotoItem

interface ThumbnailGenerator {
    suspend fun generate(item: PhotoItem, maxSize: Int = 256): ByteArray?
}
