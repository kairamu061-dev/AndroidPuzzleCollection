# ノノグラムの保存 設計

## データ構造

形式 1。1 行 1 項目。

```
nonogram-save 1
{size}
{正解を 0/1 で size² 桁}
{マスを 0=空 1=塗り 2=× で size² 桁}
{経過ミリ秒}
```

## インターフェース

```kotlin
object NonogramSaveCodec { fun encode(state, elapsedMillis): String; fun decode(text): Pair<NonogramState, Long>? }
```
