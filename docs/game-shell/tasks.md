# ゲーム共通部品 タスク

## 実装タスク一覧

- [ ] `:game-common` へ `PlayTimer` を移す
- [ ] 画面部品・`SaveFile`・`TimedGameViewModel` を切り出し、数独を差し替える

## テストの線引き（★ 実装を始める前に埋める）

| 対象 | 自動で書けるか | 置き場所 |
|---|---|---|
| タイマー・書式 | **書ける** | `tests/game-common`, `tests/app` |
| 保存ファイル | **書ける**（Robolectric） | `tests/app` |
