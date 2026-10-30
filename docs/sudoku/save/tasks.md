# 数独の保存と再開 タスク

## 実装タスク一覧

- [x] `SaveCodec`
- [x] `SaveRepository`

## テストの線引き（★ 実装を始める前に埋める）

| 対象 | 自動で書けるか | 置き場所 |
|---|---|---|
| 直列化の往復・形式・拒否 | **書ける** | `tests/sudoku-core` |
| ファイルの読み書き | **書ける**（Robolectric） | `tests/app` |
| OS にプロセスを落とされた後の再開 | 実機が要る | `docs/sudoku/screen/test-cases.md` E-06 |

**自動で書けるものは、最初のコミットから CI に載せる。**

## 依存関係

- `GameState` → `SaveCodec` → `SaveRepository`
