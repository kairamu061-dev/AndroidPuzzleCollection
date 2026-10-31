# スライドパズルの保存 設計

## データ構造

形式 1。1 行 1 項目。

```
slide-save 1
{size}
{tiles をカンマ区切り}
{手数}
{経過ミリ秒}
```

検証: サイズが 3〜5、tiles が `0..size²-1` の順列、手数と時間が 0 以上。

## インターフェース

```kotlin
object SlideSaveCodec { fun encode(state, elapsedMillis): String; fun decode(text): Pair<SlideState, Long>? }
```
