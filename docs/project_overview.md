# プロジェクト概要

## 目的・背景

オフラインで遊べるパズル系ゲームを 1 つにまとめた Android アプリを作る。

## スコープ

### 作るもの

- パズルゲーム集の Android アプリ（オフラインで完結）
- 収録ゲーム: 9×9 の数独、スライドパズル、2048、ノノグラム、ナンバーリンク

### 作らないもの

- iOS 版

## 技術スタック概要

| 技術 | 用途 | 選定理由 |
|------|------|----------|
| Kotlin | 言語 | Android 専用なのでクロスプラットフォーム層（Flutter / KMP / React Native）の利点が無い |
| Jetpack Compose（Material 3） | UI | Android 標準の UI ツールキット。盤面のような独自描画も同じ仕組みで書ける |
| Android Gradle Plugin + Gradle Wrapper | ビルド | Android 標準。Gradle はプロジェクトごとに Wrapper で固定する |
| JDK 17 | ビルド | AGP 9.5 の最小かつ既定（下記） |
| Dev Container（Docker） | 開発環境 | 環境をリポジトリで再現する。定義: `.devcontainer/` |

### 開発環境

バージョンの正は `gradle/libs.versions.toml`、`gradle/wrapper/gradle-wrapper.properties`、
`app/build.gradle.kts` に置く。minSdk 26 は暫定値で、配布対象の端末が決まったら見直す。

**測った:** このコンテナ（`claudeuser`）で `./gradlew test assembleDebug` が成功する。
platform と build-tools は AGP が自動で取得する。
AGP 9 は Kotlin 内蔵のため `org.jetbrains.kotlin.android` は適用せず、
`org.jetbrains.kotlin.plugin.compose` のみ適用した。

## 全体アーキテクチャ

```
:app           Android アプリ。画面・ファイル入出力・ゲームの一覧
  ├─ :game-common    ゲーム共通の純粋ロジック（タイマー）
  ├─ :sudoku-core    数独の規則と状態。Android に依存しない
  ├─ :slide-core     スライドパズル
  ├─ :game2048-core  2048
  ├─ :nonogram-core  ノノグラム
  └─ :numberlink-core ナンバーリンク
```

- ゲームごとに、Android に依存しない `:{game}-core` モジュールと、
  `:app` 内の画面を持つ。純粋ロジックを JVM だけで検証できるようにするため
- ゲームの追加は、core モジュールと画面を作り `GameCatalog` に 1 項目足すことで済む
- Activity は 1 つ。画面の切り替えは Compose の状態で行う（`docs/home/design.md`）
- ネットワークは使わない。`INTERNET` 権限が無いことを CI で確認する（`docs/testing.md`）

applicationId は `dev.puzzlecollection`。**公開後は変更できない。** 公開前に見直す。

## 制約

- 実行にネットワークを要しない（オフラインで完結）
- 開発ホストは Windows。ビルドは Dev Container 内で行う
- コンテナ内でエミュレータは使わない。**測った:** コンテナに `/dev/kvm` が無い
- 実機での確認は `rules/verification.md` の手順に従う。
  **推測:** コンテナからの Wi-Fi 経由の `adb connect` は可能と見ているが未確認
  （実機をつないで試せば決着する）
- `sdkmanager` は実行時に非推奨の警告を出す（**測った:** 後継は Android CLI）。
  現時点では動作に問題が無いのでそのまま使う

## UI デザイン方針

### コンセプト

紙とインク。余白を取った静かな画面で、パズルそのものに集中させる。装飾は置かない。

### カラーパレット

端末の壁紙に左右されないよう Dynamic Color は使わない。値の定義は `Color.kt` に置く。

| 用途 | ライト | ダーク | 説明 |
|------|--------|--------|------|
| プライマリ | `#2F4B8F` | `#AFC3FF` | 藍色。記入した数字と主要なボタン |
| セカンダリ | `#566080` | `#BDC6E4` | 控えめな操作 |
| 背景 | `#F8F6F0` | `#14151A` | 生成りの紙 / 墨 |
| テキスト | `#1C1B1F` | `#E6E4E0` | 初期配置の数字にも使う |
| エラー | `#BA1A1A` | `#FFB4AB` | 衝突した数字 |

### タイポグラフィ

端末標準のフォントを使い、フォントファイルは同梱しない。サイズは Material 3 の既定に従う。

### コンポーネント方針

Material 3 の標準部品を使う。押せる部品は 48dp 四方以上を基本とする。
ただし盤面のマスは幅 360dp の画面で約 38dp になり、盤面の大きさが優先する。
