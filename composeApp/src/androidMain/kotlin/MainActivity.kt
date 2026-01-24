package com.organize.photos.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.organize.photos.logic.PhotoScanner
import com.organize.photos.logic.ThumbnailGenerator

class MainActivity : ComponentActivity() {
    private var permissionGranted by mutableStateOf(false)
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // パーミッション確認
        checkAndRequestPermission()
        
        setContent {
            if (permissionGranted) {
                val photoScanner = remember { PhotoScanner(this@MainActivity) }
                val thumbnailGenerator = remember { ThumbnailGenerator(this@MainActivity) }
                AndroidAppFrame(
                    context = this@MainActivity,
                    photoScanner = photoScanner,
                    thumbnailGenerator = thumbnailGenerator
                )
            } else {
                PermissionScreen(
                    onRequestPermission = { checkAndRequestPermission() }
                )
            }
        }
    }
    
    private fun checkAndRequestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        when {
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                permissionGranted = true
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }
}

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text("写真へのアクセスが必要です")
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("許可する")
            }
        }
    }
}

