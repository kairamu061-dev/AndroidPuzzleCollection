# 数独エンジン 設計

## 技術選定

| 技術 | 用途 | 選定理由 |
|------|------|----------|
| ビットマスク + 最小候補優先のバックトラック | 解法 | 生成で何度も呼ぶので速さが要る |
| 180° 点対称の穴あけ | 生成 | 数独の問題らしい見た目になる |
| `kotlin.random.Random` の注入 | 生成 | シードで再現でき、テストが決定的になる |

## アーキテクチャ

- `Solver`: 解法・解の個数
- `Grader`: 手筋を順に適用して難易度を決める
- `Generator`: 完成盤を作り、唯一解と難易度の上限を保ったまま穴をあける。
  完成後の難易度が要求と違えば最初からやり直す

穴あけの条件:

| 要求 | 穴をあけてよい条件 |
|---|---|
| やさしい | 唯一解、判定がやさしい、初期配置が 36 個以上残る |
| ふつう | 唯一解、判定がふつう以下 |
| むずかしい | 唯一解 |

## データ構造

```kotlin
enum class Difficulty { EASY, MEDIUM, HARD }

// 値は 81 要素、0 は空き。インスタンスは作成後に変更しない
class Puzzle(val givens: List<Int>, val solution: List<Int>, val difficulty: Difficulty)
```

## インターフェース

```kotlin
object Solver {
    fun solve(cells: List<Int>): List<Int>?
    fun countSolutions(cells: List<Int>, limit: Int = 2): Int
}
object Grader { fun grade(givens: List<Int>): Difficulty }
object Generator {
    fun generate(difficulty: Difficulty, random: Random, onAttempt: () -> Unit = {}): GeneratedPuzzle
}
class GeneratedPuzzle(val puzzle: Puzzle, val attempts: Int)
```

## 依存関係

| ライブラリ / サービス | 用途 |
|-----------------------|------|
| Kotlin 標準ライブラリ | のみ |
