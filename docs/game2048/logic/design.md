# 2048 のロジック 設計

## データ構造

```kotlin
// id は画面のアニメーション用。合体の結果は進行方向の先のタイルの id を引き継ぐ
data class Tile(val id: Int, val value: Int, val row: Int, val column: Int)
data class Game2048State(val tiles: List<Tile>, val score: Int, val best: Int, val reached2048: Boolean, val nextId: Int)
enum class Direction { UP, DOWN, LEFT, RIGHT }
```

## インターフェース

```kotlin
fun newGame(best: Int, random: Random): Game2048State
fun Game2048State.move(direction: Direction, random: Random): Game2048State // 動かなければ this
val Game2048State.isOver: Boolean
```

モジュールは `:game2048-core`。
