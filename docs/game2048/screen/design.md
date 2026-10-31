# 2048 の画面 設計

## アーキテクチャ

`Game2048ViewModel`、`Game2048Route`、`TileBoard`。スワイプの判定は `dragDirection(dx, dy)` の純粋関数にする。
