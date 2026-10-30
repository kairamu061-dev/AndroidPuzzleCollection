# ホーム 設計

## 技術選定

| 技術 | 用途 | 選定理由 |
|------|------|----------|
| Compose の状態による画面切り替え | 画面遷移 | 画面数が少なく、Navigation ライブラリの依存と型定義の準備に見合わない |
| `BackHandler` | 戻る操作 | Activity 1 つで完結する |

## アーキテクチャ

- `PuzzleApp`: 現在の行き先（ホーム or ゲーム ID）を `rememberSaveable` で持ち、切り替える
- `GameCatalog`: 収録ゲームの一覧。ゲーム追加時に触るのはここだけ
- `HomeScreen`: 一覧を描画する

## データ構造

```kotlin
data class GameEntry(
    val id: String,          // 画面の復元に使う。変更しない
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val content: @Composable (onExit: () -> Unit) -> Unit,
)
```

## インターフェース

```kotlin
@Composable fun PuzzleApp()
@Composable fun HomeScreen(games: List<GameEntry>, onOpen: (GameEntry) -> Unit)
```

## 依存関係

| ライブラリ / サービス | 用途 |
|-----------------------|------|
| androidx.activity:activity-compose | Activity と `BackHandler` |
