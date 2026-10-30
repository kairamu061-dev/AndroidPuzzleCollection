# 数独のプレイ状態 設計

## 技術選定

| 技術 | 用途 | 選定理由 |
|------|------|----------|
| 不変のデータクラス | 状態 | 操作は「古い状態 → 新しい状態」の関数になり、Undo は過去の状態を持つだけで済む。Compose の再描画とも相性がよい |
| 時刻の注入 | タイマー | 実時間を待たずにテストできる |

## アーキテクチャ

- `GameState`: 状態と操作。操作はすべて新しい `GameState` を返し、無視する場合は `this` を返す
- `PlayTimer`: 経過時間。時刻は引数で受け取る

## データ構造

```kotlin
data class BoardSnapshot(val values: List<Int>, val memos: List<Int>) // memos はビット集合（bit n = 数字 n）

data class GameState(
    val puzzle: Puzzle,
    val board: BoardSnapshot,
    val selected: Int? = null,
    val memoMode: Boolean = false,
    val undoStack: List<BoardSnapshot> = emptyList(),
    val redoStack: List<BoardSnapshot> = emptyList(),
)
```

選択とメモ切り替えは盤面の変更ではないので履歴に含めない。

## インターフェース

```kotlin
fun GameState.select(cell: Int): GameState
fun GameState.input(digit: Int): GameState  // memoMode で記入とメモを分ける
fun GameState.erase(): GameState
fun GameState.undo(): GameState
fun GameState.redo(): GameState
fun GameState.toggleMemoMode(): GameState
val GameState.conflicts: Set<Int>
val GameState.remaining: List<Int>          // 添字 0 が数字 1
val GameState.isSolved: Boolean

class PlayTimer(initialElapsedMillis: Long = 0) {
    fun start(now: Long); fun pause(now: Long); fun elapsed(now: Long): Long
}
```

## 依存関係

| ライブラリ / サービス | 用途 |
|-----------------------|------|
| engine | `Puzzle` |
