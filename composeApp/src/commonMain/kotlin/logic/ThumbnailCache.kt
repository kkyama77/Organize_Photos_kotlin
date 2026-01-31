package com.organize.photos.logic

import com.organize.photos.model.PhotoItem
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ThumbnailCache(private val maxItems: Int = 256) {
    private val lock = Mutex()
    private val cache = object : LinkedHashMap<String, ByteArray>(maxItems, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>?): Boolean {
            return size > maxItems
        }
    }

    suspend fun get(id: String): ByteArray? = lock.withLock { cache[id] }

    suspend fun put(item: PhotoItem, bytes: ByteArray) = lock.withLock {
        cache[item.id] = bytes
    }

    suspend fun remove(id: String) = lock.withLock { cache.remove(id) }

    suspend fun clear() = lock.withLock { cache.clear() }
}
