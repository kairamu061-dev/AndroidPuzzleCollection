# 2048 の保存 設計

## データ構造

形式 1。1 行 1 項目。

```
2048-save 1
{16 個の値をカンマ区切り}
{スコア}
{ベスト}
{0 or 1}
```

検証: 値は 0 か 2 以上の 2 の冪、スコアとベストは 0 以上、スコア ≤ ベスト。

## インターフェース

```kotlin
object Game2048SaveCodec { fun encode(state): String; fun decode(text): Game2048State? }
```

タイルの id は保存せず、読み込み時に振り直す。
