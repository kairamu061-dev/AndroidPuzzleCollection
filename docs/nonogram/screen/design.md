# ノノグラムの画面 設計

## アーキテクチャ

`NonogramViewModel`（`TimedGameViewModel` の派生）、`NonogramRoute`、`NonogramBoard`。

`NonogramViewModel` は問題集と完成の集合を `catalog` で公開する。問題集の読み込みはファイル入出力なので IO で行う。

表示範囲は `NonogramViewport`（倍率と移動量）で持つ。拡大率は盤面の状態ではないので画面側に置き、保存しない。
ヒントの数字は事前に測って再利用する。ピンチ中は毎フレーム描き直すため。

20×20 は 400 マスあり、ヒントと合わせて部品が増えすぎるので、盤面は `Canvas` 1 枚で描き、座標からマスを求める `cellAt` を純粋関数にする。読み上げは盤面全体の説明に留める。
