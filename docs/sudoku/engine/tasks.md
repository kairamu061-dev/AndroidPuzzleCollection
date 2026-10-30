# 数独エンジン タスク

## 実装タスク一覧

- [ ] `Solver`
- [ ] `Grader`
- [ ] `Generator`

## テストの線引き（★ 実装を始める前に埋める）

| 対象 | 自動で書けるか | 置き場所 |
|---|---|---|
| 解法・判定・生成 | **書ける** | `tests/sudoku-core` |
| 実機での生成の待ち時間 | 実機が要る | `docs/sudoku/screen/test-cases.md` E-01 |

**自動で書けるものは、最初のコミットから CI に載せる。**

## 依存関係

- `Solver` → `Grader` → `Generator`
