package com.organize.photos.logic

import android.content.IntentSender

/**
 * Android の書き込み許可リクエストをUI層から受け取るためのブリッジ
 */
object AndroidWritePermission {
    var requestWrite: (suspend (IntentSender) -> Boolean)? = null
}
