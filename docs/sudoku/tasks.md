# 数独 タスク

## 実装タスク一覧

- [x] [engine](./engine/tasks.md)
- [x] [play](./play/tasks.md)
- [x] [save](./save/tasks.md)
- [x] [screen](./screen/tasks.md)

## テストの線引き（★ 実装を始める前に埋める）

サブ項目の tasks.md に書く。全体の線は `docs/testing.md`。

## 依存関係

- engine → play（問題の型を使う）
- play → save（状態を直列化する）
- play, save → screen
