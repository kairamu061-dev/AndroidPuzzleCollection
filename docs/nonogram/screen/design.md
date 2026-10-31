# ノノグラムの画面 設計

## アーキテクチャ

`NonogramViewModel`（`TimedGameViewModel` の派生）、`NonogramRoute`、`NonogramBoard`。

20×20 は 400 マスあり、ヒントと合わせて部品が増えすぎるので、盤面は `Canvas` 1 枚で描き、座標からマスを求める `cellAt` を純粋関数にする。読み上げは盤面全体の説明に留める。
