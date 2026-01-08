package com.organize.photos.preview

import com.organize.photos.model.PhotoItem
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.random.Random

object PreviewData {
    fun fakePhotos(count: Int = 30): List<PhotoItem> {
        val now = Clock.System.now()
        return List(count) { index ->
            val w = listOf(800, 1024, 1920, 2560).random()
            val h = listOf(600, 768, 1080, 1440).random()
            val ext = listOf("jpg", "jpeg", "png").random()
            PhotoItem(
                id = "fake-$index",
                displayName = "Sample ${index + 1}.$ext",
                absolutePath = "/sample/path/${index + 1}.$ext",
                capturedAt = now.minus(randomOffset()),
                width = w,
                height = h,
                sizeBytes = (w * h * 3L),
                extension = ext,
                thumbnail = null,
            )
        }
    }

    private fun randomOffset(): kotlin.time.Duration {
        val days = Random.nextInt(0, 365)
        return kotlin.time.Duration.parse("${days}d")
    }
}
