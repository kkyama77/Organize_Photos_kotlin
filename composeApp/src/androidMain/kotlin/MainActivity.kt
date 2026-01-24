package com.organize.photos.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.runtime.remember
import com.organize.photos.logic.PhotoScanner
import com.organize.photos.logic.ThumbnailGenerator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val photoScanner = remember { PhotoScanner(this@MainActivity) }
            val thumbnailGenerator = remember { ThumbnailGenerator(this@MainActivity) }
            AndroidAppFrame(
                context = this@MainActivity,
                photoScanner = photoScanner,
                thumbnailGenerator = thumbnailGenerator
            )
        }
    }
}

