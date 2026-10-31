# 数独の保存と再開 設計

## 技術選定

| 技術 | 用途 | 選定理由 |
|------|------|----------|
| 行ごとの文字列形式 | 直列化 | 項目が少なく、JSON ライブラリの依存に見合わない。`:sudoku-core` を標準ライブラリのみに保てる |
| `android.util.AtomicFile` | ファイル | 書き込み途中の異常終了で前回分が残る |

## アーキテクチャ

- `SaveCodec`（`:sudoku-core`）: `SavedGame` と文字列の相互変換。読めなければ `null`
- `SaveFile`（`:app`、`docs/game-shell/design.md`）: `filesDir/sudoku/save.txt` の読み書きと削除

## データ構造

形式 1。1 行 1 項目、この順序。

```
sudoku-save 1          … 見出しと形式バージョン
EASY                   … 難易度（Difficulty の名前）
{81 桁}                … 初期配置（0 は空き）
{81 桁}                … 解
{81 桁}                … 記入を含む現在の値
{81 個の整数をカンマ区切り} … メモのビット集合
{整数}                 … 経過ミリ秒
```

```kotlin
data class SavedGame(val state: GameState, val elapsedMillis: Long)
```

読み込み時の検証: 行数、各桁の範囲、解の完全性、初期マスの値が現在の値と一致すること、経過時間が 0 以上。

## インターフェース

```kotlin
object SaveCodec { fun encode(game: SavedGame): String; fun decode(text: String): SavedGame? }
fun shouldSave(before: GameState, after: GameState): Boolean  // 盤面が変わったときだけ真
```

## 依存関係

| ライブラリ / サービス | 用途 |
|-----------------------|------|
| play | `GameState` |
