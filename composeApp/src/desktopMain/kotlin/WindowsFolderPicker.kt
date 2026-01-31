package com.organize.photos.desktop

import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.platform.win32.Shell32
import com.sun.jna.platform.win32.ShellAPI
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinNT

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
     * @return 選択されたパス、またはnull
     */
    fun selectFolder(title: String, initialPath: String?, ownerHwnd: WinDef.HWND?): String? {
        return try {
            val browseInfo = ShellAPI.BROWSEINFO().apply {
                hwndOwner = ownerHwnd
                pidlRoot = null
                pszDisplayName = CharArray(260)
                lpszTitle = WString(title)
                ulFlags = BIF_RETURNONLYFSDIRS or BIF_NEWDIALOGSTYLE
                lpfn = null
                lParam = null
            }

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
            // フォールバック: File APIで実装（ユーザーが不要な場合は削除）
            null
        }
    }
}
