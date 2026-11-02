# ナンバーリンクの画面 設計

## アーキテクチャ

`NumberlinkViewModel`（`TimedGameViewModel` の派生）、`NumberlinkRoute`、`NumberlinkBoard`（`Canvas` 1 枚）。
座標からマスを求める処理は純粋関数にしてテストする。

## 色

数字ごとに色相を変えた色を使い、端点には必ず数字を書く。10 色を超えて見分けるのは難しいため。
