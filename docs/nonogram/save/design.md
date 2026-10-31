# ノノグラムの保存 設計

## データ構造

形式 2。1 行 1 項目。形式 1（絵 ID の行が無い）も読める。

```
nonogram-save 2
{size}
{絵の ID。絵でない問題は -}
{正解を 0/1 で size² 桁}
{マスを 0=空 1=塗り 2=× で size² 桁}
{経過ミリ秒}
```

## インターフェース

```kotlin
object NonogramSaveCodec { fun encode(state, elapsedMillis): String; fun decode(text): Pair<NonogramState, Long>? }
```
