# ナンバーリンクの保存 設計

## データ構造

形式 1。1 行 1 項目。

```
numberlink-save 1
{size}
{端点: 数字ごとに "a-b" をカンマ区切り}
{解: 数字ごとのマス列を "." 区切り、数字の間は "/"}
{線: 解と同じ書き方。空の線は空文字}
{経過ミリ秒}
```

検証: マス番号が盤面内、端点が重複しない、線が端点から始まり縦横に連続する。

## インターフェース

```kotlin
object NumberlinkSaveCodec { fun encode(state, elapsedMillis): String; fun decode(text): Pair<NumberlinkState, Long>? }
```
