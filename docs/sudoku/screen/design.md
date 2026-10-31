# 数独の画面 設計

## 技術選定

| 技術 | 用途 | 選定理由 |
|------|------|----------|
| 81 個の Composable によるマス | 盤面 | `Canvas` 1 枚より、マスごとの押下・読み上げ・テストの指定が素直に書ける。太線だけ重ねて描く |
| `ViewModel` + `StateFlow` | 状態 | 回転を越えて状態を保つ。生成と保存のコルーチンの置き場所になる |
| `Dispatchers.Default` / `IO` | 生成 / 保存 | UI スレッドを止めない |

## アーキテクチャ

- `SudokuViewModel`: `TimedGameViewModel` の派生。操作を `GameState` へ渡す。
  Activity スコープで持ち、ホームへ戻ってもメモリ上のゲームを保つ
- `SudokuRoute`: `TimedGameRoute` へ数独の選択肢と画面を渡す
- `SudokuGameScreen`: 描画のみ
- `BoardView` / `NumberPad` / `ActionBar`: 部品

保存は 1 本のコルーチンに直列化する。順序が入れ替わると古い状態が最後に書かれるため。

## データ構造

状態は `TimedUiState`（`docs/game-shell/design.md`）。

## インターフェース

```kotlin
@Composable fun SudokuRoute(onExit: () -> Unit, viewModel: SudokuViewModel = ...)

class SudokuViewModel(
    saveFile: SaveFile,
    clock: () -> Long = ...,                           // 単調増加の時計
    generate: (Difficulty, () -> Unit) -> Puzzle = ...,// テストで既知の問題に差し替える
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
)
```

## 依存関係

| ライブラリ / サービス | 用途 |
|-----------------------|------|
| androidx.lifecycle:lifecycle-viewmodel-compose | `ViewModel` |
| androidx.lifecycle:lifecycle-runtime-compose | 画面の表示状態に合わせたタイマーの制御 |
