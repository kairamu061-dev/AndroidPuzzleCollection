# ノノグラムのロジック 設計

## データ構造

```kotlin
enum class Mark { EMPTY, FILLED, CROSS }
data class NonogramPuzzle(val size: Int, val solution: List<Boolean>)  // rowClues, columnClues を算出
data class NonogramState(val puzzle, val cells: List<Mark>, val tool: Mark, undo, redo, ...)
```

## インターフェース

```kotlin
object LineSolver { fun solve(size, rowClues, columnClues): List<Boolean>? } // 確定し切れなければ null
object NonogramGenerator { fun generate(size: Int, random: Random, onAttempt: () -> Unit = {}): NonogramPuzzle }
fun NonogramState.strokeStart(cell): NonogramState
fun NonogramState.strokeTo(cell): NonogramState
fun NonogramState.strokeEnd(): NonogramState
```

解法は 1 行ごとに「置ける全パターンで共通するマス」を動的計画法で求め、変化がなくなるまで行と列を繰り返す。
行と列の確定だけで全マスが決まれば、解は唯一。

模様は乱数のマスを近傍で均して作る。塗りの偏りが無いと解けない問題ばかりになるため。

モジュールは `:nonogram-core`。
