# スライドパズルのロジック 設計

## データ構造

```kotlin
// tiles は行優先で size * size 要素。0 が空き
data class SlideState(val size: Int, val tiles: List<Int>, val moves: Int = 0)
```

## インターフェース

```kotlin
object SlideGenerator { fun generate(size: Int, random: Random): SlideState }
fun SlideState.tap(cell: Int): SlideState  // 無視する場合は this
val SlideState.isSolved: Boolean
```

モジュールは `:slide-core`。可解性は手を積む生成で保証し、テストでは別の方法（転倒数の偶奇）で確かめる。
