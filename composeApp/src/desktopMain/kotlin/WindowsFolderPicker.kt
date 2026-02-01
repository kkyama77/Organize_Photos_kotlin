package com.organize.photos.desktop

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.platform.win32.Shell32
import com.sun.jna.platform.win32.ShellAPI.BROWSEINFO
import com.sun.jna.platform.win32.WinDef.HWND

/**
 * Windows 標準のフォルダ選択ダイアログを使用するクラス
 * Windows でのみ利用可能
 */
object WindowsFolderPicker {
    private const val BIF_RETURNONLYFSDIRS = 0x0001
    private const val BIF_NEWDIALOGSTYLE = 0x0040
    
    fun selectFolder(title: String, initialPath: String?, ownerWindow: java.awt.Window?): String? {
        // Windows以外のプラットフォームでは何もしない
        if (!System.getProperty("os.name").lowercase().contains("win")) {
            return null
        }
        
        return try {
            // HWNDの取得
            val ownerHwnd = ownerWindow?.let { window ->
                HWND(Pointer.createConstant(Native.getComponentID(window)))
            }
            
            // BROWSEINFOの作成
            val browseInfo = BROWSEINFO().apply {
                hwndOwner = ownerHwnd
                pidlRoot = null
                pszDisplayName = CharArray(260)
                lpszTitle = WString(title)
                ulFlags = BIF_RETURNONLYFSDIRS or BIF_NEWDIALOGSTYLE
                lpfn = null
                lParam = Pointer.NULL
                iImage = 0
            }

            val pidl = Shell32.INSTANCE.SHBrowseForFolder(browseInfo)

            if (pidl == null || pidl == Pointer.NULL) {
                null
            } else {
                val pszPath = CharArray(260)
                if (Shell32.INSTANCE.SHGetPathFromIDList(pidl, pszPath)) {
                    pszPath.takeWhile { it != '\u0000' }.joinToString("")
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
