package com.organize.photos.desktop

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.win32.StdCallLibrary
import java.io.File

/**
 * Windows 標準のフォルダ選択ダイアログを使用するクラス
 */
object WindowsFolderPicker {
    
    interface Shell32 : StdCallLibrary {
        fun SHBrowseForFolder(browseInfo: BROWSEINFO): Pointer
        fun SHGetPathFromIDList(pidl: Pointer, pszPath: CharArray): Boolean
    }
    
    @Structure.FieldOrder("hwndOwner", "pidlRoot", "pszDisplayName", "lpszTitle", "ulFlags", "lpfn", "lParam", "iImage")
    class BROWSEINFO : Structure() {
        var hwndOwner: WinDef.HWND? = null
        var pidlRoot: Pointer? = null
        var pszDisplayName: CharArray = CharArray(260)
        var lpszTitle: String? = null
        var ulFlags: Int = BIF_RETURNONLYFSDIRS or BIF_NEWDIALOGSTYLE
        var lpfn: Pointer? = null
        var lParam: WinDef.LPARAM? = null
        var iImage: Int = 0
        
        companion object {
            private const val BIF_RETURNONLYFSDIRS = 0x0001  // ファイルシステムディレクトリのみ
            private const val BIF_NEWDIALOGSTYLE = 0x0040      // 新しいダイアログスタイル
        }
    }
    
    /**
     * Windows 標準フォルダ選択ダイアログを表示
     * @param title ダイアログのタイトル
     * @param initialPath 初期パス
     * @return 選択されたパス、またはnull
     */
    fun selectFolder(title: String, initialPath: String?): String? {
        return try {
            val shell32 = Native.load("shell32", Shell32::class.java)
            val browseInfo = BROWSEINFO().apply {
                lpszTitle = title
                ulFlags = 0x0001 or 0x0040  // BIF_RETURNONLYFSDIRS | BIF_NEWDIALOGSTYLE
            }
            
            val pidl = shell32.SHBrowseForFolder(browseInfo)
            
            if (pidl == null || pidl == Pointer.NULL) {
                null  // ユーザーがキャンセルした
            } else {
                val pszPath = CharArray(260)
                if (shell32.SHGetPathFromIDList(pidl, pszPath)) {
                    pszPath.takeWhile { it != '\u0000' }.joinToString("")
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            // フォールバック: File APIで実装（ユーザーが不要な場合は削除）
            null
        }
    }
}
