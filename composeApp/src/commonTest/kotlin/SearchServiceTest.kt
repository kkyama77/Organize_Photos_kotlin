package com.organize.photos

import com.organize.photos.logic.SearchService
import com.organize.photos.model.PhotoItem
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchServiceTest {
    @Test
    fun filtersByExtension() {
        val photos = listOf(
            PhotoItem("1", "a.jpg", "/a.jpg", Clock.System.now(), 100, 100, 1000, "jpg"),
            PhotoItem("2", "b.png", "/b.png", Clock.System.now(), 100, 100, 1000, "png"),
        )
        val sut = SearchService()

        val result = sut.filter(photos, query = "", extensions = setOf("jpg"))

        assertEquals(1, result.size)
        assertEquals("jpg", result.first().extension)
    }
}
