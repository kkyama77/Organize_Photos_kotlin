# Kotlin Multiplatform 版 要件定義

## 対応プラットフォーム
- iOS / Android / Windows / macOS / Linux
- 優先: まずデスクトップ、後続でモバイル統合

## 機能要件
- フォルダ選択: 写真格納フォルダを指定
- スキャン: 配下の画像を再帰列挙（JPEG/PNG/HEIC/TIFF 等）
- メタデータ抽出: 撮影日時・解像度・サイズ・拡張子（EXIF優先）
- サムネ生成: 非同期でサムネイル生成＋キャッシュ（ディスク＋メモリ）
- 一覧表示: 仮想リスト（LazyColumn/LazyVerticalGrid）で高速表示
- 検索/フィルタ: テキスト検索（日本語IME対応）、日付レンジ、拡張子フィルタ
- 選択/操作: 複数選択、詳細ビュー（将来: コピー/移動/削除に拡張）
- エラー通知: 権限/パス不正/サムネ生成失敗をUI提示
- 設定: サムネキャッシュ上限、対象拡張子、既定フォルダ記憶

## 非機能要件
- パフォーマンス: 数千枚でも快適スクロール/検索
- メモリ: サムネは上限付きキャッシュ（LRU）
- 応答性: バックグラウンド生成→逐次反映
- 国際化: 日本語UI・IME前提（英語追加は将来）
- 配布: 各OSネイティブパッケージ、モバイルはストア配布前提

## 技術スタック
- UI: Compose Multiplatform（Desktop + Android + iOS）
- ロジック: Kotlin Multiplatform 共有コード（スキャン/メタデータ/検索）
- 画像/EXIF: KMP対応ライブラリ（例: metadata-extractor JVM、iOS/Androidは別実装）
- キャッシュ: 共通ロジック＋プラットフォーム適合ストレージ（デスクトップ=ファイル、モバイル=ファイル+DB）
- ビルド: Gradle (KMP), JDK 17, Android Studio or IntelliJ, iOSはXcode必須
- テスト: 共有ロジックはKMPテスト、固有部分は別途UI/インストルメンテーション

## アーキテクチャ方針
- 共有層: スキャン/メタデータ/検索/キャッシュ制御をKMP共通に集約
- UI層: Composeコンポーネントを共通化し、エントリだけプラットフォーム別
- 非同期: コルーチンでバックグラウンド処理、メインへスナップショット反映
- 拡張性: 将来の重複検出・AIタグ付け等をモジュール追加で対応

## 初期マイルストーン
1. KMP + Compose Desktop テンプレ生成、デスクトップでダミー画像リスト表示
2. 実フォルダスキャンとメタデータ抽出を共通ロジックに実装（JPEG/PNG/HEIC/TIFF対応）
3. サムネ生成＋キャッシュ＋仮想リストで数千枚性能確認
4. 検索/フィルタ（テキスト・日付・拡張子）を追加
5. モバイル（Android→iOS）ビルドを有効化し、同UIを流用

## 現在の実装スケルトン
- KMP + Compose Multiplatform (Desktop/Android/iOS) プロジェクトを生成済み
- デスクトップ: フォルダ選択 → 再帰スキャン（拡張子フィルタ対応、metadata-extractor でEXIF読み取り）
- 共有UI: フォルダ選択＋再スキャンボタン、検索テキスト＋拡張子フィルタ、空表示/エラーメッセージ
- 共有ロジック: スキャン/検索/サムネキャッシュのインターフェース（Desktop版は実装済み）
- プラットフォーム別エントリ: Desktop `desktopRun`, Android `:composeApp:assembleDebug`, iOS `MainViewController()`

## 使い方（ローカルビルド）
前提: JDK 17, Android SDK (モバイル向け), Xcode (iOS 向け)。

1. Gradle Wrapper 取得（初回のみ）
	- Windows: `pwsh -Command "Invoke-WebRequest https://services.gradle.org/distributions/gradle-8.7-bin.zip -OutFile gradle-8.7-bin.zip; Expand-Archive gradle-8.7-bin.zip -DestinationPath .; Copy-Item gradle-8.7\lib\gradle-wrapper.jar gradle\wrapper\"`
	- その後 `./gradlew --version` で確認
2. デスクトップ実行: `./gradlew :composeApp:desktopRun`
3. 共通テスト: `./gradlew :composeApp:allTests`
4. Android ビルド（エミュレータ/端末に転送）: `./gradlew :composeApp:assembleDebug`
5. iOS: Xcode で `iosApp` ターゲットを設定し、`MainViewController()` をエントリとして利用（UIKit/Compose Multiplatform）

## 次にやること
- 実フォルダスキャン（デスクトップ: java.nio + metadata-extractor を呼び出すロジック実装）
- サムネイル生成（ImageIO/Skia 経由）と LRU キャッシュ統合
- Android/iOS のストレージ権限処理とフォルダピッカー導入
- 検索UIを日付レンジ/拡張子複合フィルタに拡張
