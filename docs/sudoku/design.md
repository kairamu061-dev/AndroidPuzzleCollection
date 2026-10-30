# 数独 設計

実装はサブ項目ごとに書く。ここには横断事項のみ置く。

- [engine](./engine/design.md) — 問題の生成・解法・難易度の判定
- [play](./play/design.md) — 盤面の状態と操作、タイマー
- [screen](./screen/design.md) — 開始・プレイの画面
- [save](./save/design.md) — 中断データの保存と再開

## モジュールの境界

| モジュール | 持つもの | 依存 |
|---|---|---|
| `:sudoku-core` | engine・play と、save のうち直列化 | Kotlin 標準ライブラリのみ。Android に依存しない |
| `:app` | screen と、save のうちファイル入出力 | Android, Compose, `:sudoku-core` |

`:sudoku-core` に Android の型を入れると、JVM だけでのテストが成り立たなくなる。
