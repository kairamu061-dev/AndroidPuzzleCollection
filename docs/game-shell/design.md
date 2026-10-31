# ゲーム共通部品 設計

## アーキテクチャ

| 置き場所 | 部品 | 役割 |
|---|---|---|
| `:game-common` | `PlayTimer` | Android に依存しない計測 |
| `:app` `ui/GameChrome.kt` | `BackIconButton`, `LoadingScreen`, `ConfirmNewGameDialog` | 画面部品 |
| `:app` `ui/GameStartScreen.kt` | `GameStartScreen` | 開始画面 |
| `:app` `ui/TimeFormat.kt` | `formatElapsed` | 書式 |
| `:app` `data/SaveFile.kt` | `SaveFile` | `AtomicFile` による読み書き |
| `:app` `session/TimedGameViewModel.kt` | `TimedGameViewModel<G, O>` | 進行の基底。`G` は盤面の状態、`O` は開始時の選択肢 |

## インターフェース

```kotlin
sealed interface TimedUiState<out G> {
    data object Loading; data class Start(val saved: G?, val elapsedMillis: Long)
    data object Generating; data class Playing<G>(val game: G, val elapsedMillis: Long)
}

abstract class TimedGameViewModel<G : Any, O>(saveFile, clock, ioDispatcher, computeDispatcher) {
    protected abstract fun create(option: O, onAttempt: () -> Unit): G
    protected abstract fun encode(game: G, elapsedMillis: Long): String
    protected abstract fun decode(text: String): Pair<G, Long>?
    protected abstract fun isFinished(game: G): Boolean
    protected abstract fun needsSave(before: G, after: G): Boolean
    protected open fun loadExtra()            // IO で読むゲーム固有のデータ
    protected open fun onFinished(game: G)    // クリアの瞬間に 1 回
    protected fun update(operation: (G) -> G)
}
```

2048 は時間を持たず開始画面も無いので基底を使わない。`SaveFile` だけ共有する。
