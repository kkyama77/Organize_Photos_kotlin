package com.organize.photos.logic

import java.text.Normalizer

/**
 * 日本語/英語の表記ゆれを吸収するテキスト正規化ユーティリティ
 */
object TextNormalizer {
    
    /**
     * 日英同義語辞書
     * キー: 正規化後の標準形、値: 同義語のリスト
     */
    private val SYNONYM_MAP = mapOf(
        // カメラ関連
        "camera" to listOf("カメラ", "かめら"),
        "lens" to listOf("レンズ", "れんず"),
        "make" to listOf("メーカー", "めーかー", "製造元", "せいぞうもと"),
        "model" to listOf("モデル", "もでる", "機種", "きしゅ"),
        
        // 撮影設定
        "iso" to listOf("感度", "かんど", "アイエスオー"),
        "aperture" to listOf("絞り", "しぼり", "こう", "F値", "Fち", "えふち"),
        "shutter" to listOf("シャッター", "しゃったー", "速度", "そくど"),
        "exposure" to listOf("露出", "ろしゅつ", "露光", "ろこう"),
        "focal" to listOf("焦点", "しょうてん"),
        "focus" to listOf("フォーカス", "ふぉーかす", "ピント", "ぴんと"),
        
        // GPS・位置
        "gps" to listOf("ジーピーエス", "位置", "いち", "座標", "ざひょう"),
        "latitude" to listOf("緯度", "いど"),
        "longitude" to listOf("経度", "けいど"),
        
        // ソフトウェア
        "software" to listOf("ソフトウェア", "そふとうぇあ", "ソフト", "そふと", "アプリ", "あぷり"),
        
        // 画像情報
        "width" to listOf("幅", "はば", "横", "よこ"),
        "height" to listOf("高さ", "たかさ", "縦", "たて"),
        "size" to listOf("サイズ", "さいず", "容量", "ようりょう"),
        "resolution" to listOf("解像度", "かいぞうど"),
        
        // 一般
        "file" to listOf("ファイル", "ふぁいる"),
        "name" to listOf("名前", "なまえ", "ネーム", "ねーむ"),
        "date" to listOf("日付", "ひづけ", "日時", "にちじ"),
        "time" to listOf("時刻", "じこく", "時間", "じかん"),
        "photo" to listOf("写真", "しゃしん", "画像", "がぞう"),
        "image" to listOf("画像", "がぞう", "イメージ", "いめーじ")
    )
    
    /**
     * 逆引き辞書（同義語 → 標準形）
     */
    private val REVERSE_SYNONYM_MAP: Map<String, String> by lazy {
        buildMap {
            SYNONYM_MAP.forEach { (standard, synonyms) ->
                synonyms.forEach { synonym ->
                    put(synonym.lowercase(), standard)
                }
            }
        }
    }
    
    /**
     * テキストを検索用に正規化
     * 1. Unicode正規化（NFC）
     * 2. カタカナ→ひらがな変換
     * 3. 半角変換（英数・カタカナ）
     * 4. 小文字化
     * 5. 同義語展開
     */
    fun normalize(text: String): String {
        if (text.isBlank()) return ""
        
        // 1. NFC正規化
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFC)
        
        // 2. カタカナ→ひらがな変換（検索時の表記ゆれ吸収）
        val hiragana = toHiragana(normalized)
        
        // 3. 半角変換
        val halfWidth = toHalfWidth(hiragana)
        
        // 4. 小文字化
        val lower = halfWidth.lowercase()
        
        // 5. 同義語展開（単語ごとに標準形に変換）
        val expanded = expandSynonyms(lower)
        
        return expanded
    }
    
    /**
     * カタカナをひらがなに変換（検索時の表記ゆれを吸収）
     */
    private fun toHiragana(text: String): String {
        return text.map { char ->
            when (char) {
                in 'ァ'..'ヶ' -> (char.code - 'ァ'.code + 'ぁ'.code).toChar()
                'ヵ' -> 'か'
                'ヶ' -> 'け'
                'ヮ' -> 'ゎ'
                else -> char
            }
        }.joinToString("")
    }
    
    /**
     * 全角英数・カタカナを半角に変換
     */
    private fun toHalfWidth(text: String): String {
        return text.map { char ->
            when (char) {
                // 全角英数 → 半角英数
                in 'Ａ'..'Ｚ' -> (char.code - 'Ａ'.code + 'A'.code).toChar()
                in 'ａ'..'ｚ' -> (char.code - 'ａ'.code + 'a'.code).toChar()
                in '０'..'９' -> (char.code - '０'.code + '0'.code).toChar()
                
                // 全角カタカナ → 半角カタカナ
                'ア' -> 'ｱ'
                'イ' -> 'ｲ'
                'ウ' -> 'ｳ'
                'エ' -> 'ｴ'
                'オ' -> 'ｵ'
                'カ' -> 'ｶ'
                'キ' -> 'ｷ'
                'ク' -> 'ｸ'
                'ケ' -> 'ｹ'
                'コ' -> 'ｺ'
                'サ' -> 'ｻ'
                'シ' -> 'ｼ'
                'ス' -> 'ｽ'
                'セ' -> 'ｾ'
                'ソ' -> 'ｿ'
                'タ' -> 'ﾀ'
                'チ' -> 'ﾁ'
                'ツ' -> 'ﾂ'
                'テ' -> 'ﾃ'
                'ト' -> 'ﾄ'
                'ナ' -> 'ﾅ'
                'ニ' -> 'ﾆ'
                'ヌ' -> 'ﾇ'
                'ネ' -> 'ﾈ'
                'ノ' -> 'ﾉ'
                'ハ' -> 'ﾊ'
                'ヒ' -> 'ﾋ'
                'フ' -> 'ﾌ'
                'ヘ' -> 'ﾍ'
                'ホ' -> 'ﾎ'
                'マ' -> 'ﾏ'
                'ミ' -> 'ﾐ'
                'ム' -> 'ﾑ'
                'メ' -> 'ﾒ'
                'モ' -> 'ﾓ'
                'ヤ' -> 'ﾔ'
                'ユ' -> 'ﾕ'
                'ヨ' -> 'ﾖ'
                'ラ' -> 'ﾗ'
                'リ' -> 'ﾘ'
                'ル' -> 'ﾙ'
                'レ' -> 'ﾚ'
                'ロ' -> 'ﾛ'
                'ワ' -> 'ﾜ'
                'ヲ' -> 'ｦ'
                'ン' -> 'ﾝ'
                'ー' -> 'ｰ'
                'ッ' -> 'ｯ'
                'ャ' -> 'ｬ'
                'ュ' -> 'ｭ'
                'ョ' -> 'ｮ'
                
                else -> char
            }
        }.joinToString("")
    }
    
    /**
     * 同義語を標準形に展開
     * 例: "レンズ" → "lens レンズ"（標準形 + 元の単語）
     */
    private fun expandSynonyms(text: String): String {
        val words = text.split(Regex("\\s+"))
        val expanded = mutableSetOf<String>()
        
        words.forEach { word ->
            if (word.isNotBlank()) {
                // 元の単語を追加
                expanded.add(word)
                
                // 同義語辞書で標準形を検索して追加
                REVERSE_SYNONYM_MAP[word]?.let { standard ->
                    expanded.add(standard)
                }
            }
        }
        
        return expanded.joinToString(" ")
    }
    
    /**
     * 複数キーワードをそれぞれ正規化
     */
    fun normalizeKeywords(keywords: List<String>): List<String> {
        return keywords.map { normalize(it) }.filter { it.isNotBlank() }
    }
}
