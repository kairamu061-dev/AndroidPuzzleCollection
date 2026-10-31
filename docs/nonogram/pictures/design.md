# ノノグラムの絵の問題集 設計

## データ構造

`nonogram-core/src/main/resources/dev/puzzlecollection/nonogram/pictures.tsv`。1 行 1 問、タブ区切り、並びが出題順。

```
{id}\t{名前}\t{400 桁の 0/1}
```

**id は進捗と中断データに保存される。変更すると完成の記録が失われる。**
名前は画面に出す文言だが、絵と 1 対 1 なので `strings.xml` ではなくここに置く。

進捗は `filesDir/nonogram/progress.txt`。1 行 1 ID。

## インターフェース

```kotlin
data class NonogramPicture(val id: String, val name: String, val puzzle: NonogramPuzzle)
object NonogramPictures { val all: List<NonogramPicture>; fun parse(text: String): List<NonogramPicture> }
fun nextPicture(pictures: List<NonogramPicture>, completed: Set<String>): NonogramPicture?
object NonogramProgress { fun encode(completed: Set<String>): String; fun decode(text: String): Set<String> }
```

## 追加の手順

1. 画像を `{category}/{NN}_{id}.png` で置く
2. スクリプトの `PICKS` に追加して実行する
3. `./gradlew test` で解ける保証を確かめる
