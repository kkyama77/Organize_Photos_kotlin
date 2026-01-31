package com.organize.photos.desktop

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.platform.win32.Shell32
import com.sun.jna.platform.win32.ShellAPI
import com.sun.jna.platform.win32.WinDef

/**
 * Windows 標準のフォルダ選択ダイアログを使用するクラス
 */
object WindowsFolderPicker {
    private const val BIF_RETURNONLYFSDIRS = 0x0001  // ファイルシステムディレクトリのみ
    private const val BIF_NEWDIALOGSTYLE = 0x0040      // 新しいダイアログスタイル
    
    /**
     * Windows 標準フォルダ選択ダイアログを表示
     * @param title ダイアログのタイトル
     * @param initialPath 初期パス
     * @param ownerWindow 親ウィンドウ
     * @return 選択されたパス、またはnull
     */
    fun selectFolder(title: String, initialPath: String?, ownerWindow: java.awt.Window?): String? {
        // Windows以外のプラットフォームでは何もしない
        if (!System.getProperty("os.name").lowercase().contains("win")) {
            return null
        }
        
        return try {
            // HWNDの取得方法を修正
            val ownerHwnd = ownerWindow?.let { window ->
                WinDef.HWND(Pointer.createConstant(Native.getComponentID(window)))
            }
            
            // BROWSEINFOの作成を修正（applyではなく通常の代入）
            val browseInfo = ShellAPI.BROWSEINFO()
            browseInfo.hwndOwner = ownerHwnd
            browseInfo.pidlRoot = null
            browseInfo.pszDisplayName = CharArray(260)
            browseInfo.lpszTitle = WString(title)
            browseInfo.ulFlags = BIF_RETURNONLYFSDIRS or BIF_NEWDIALOGSTYLE
            browseInfo.lpfn = null
            // lParam は削除（存在しないフィールド）

            val pidl = Shell32.INSTANCE.SHBrowseForFolder(browseInfo)

            if (pidl == null || pidl == Pointer.NULL) {
                null  // ユーザーがキャンセルした
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
