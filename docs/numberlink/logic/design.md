# ナンバーリンクのロジック 設計

## データ構造

```kotlin
// lines は生成時の解（保存とテスト用。クリア判定には使わない）。endpoints は各線の両端
data class NumberlinkPuzzle(val size: Int, val lines: List<List<Int>>) { val endpoints: List<Pair<Int, Int>> }

// paths[k] は数字 k+1 の線。どちらかの端点から始まるマス番号の列。空なら未着手
data class NumberlinkState(val puzzle, val paths: List<List<Int>>, undo, redo, stroke...)
```

## インターフェース

```kotlin
object NumberlinkSolver {
    /** 規則の下での解の個数（limit で打ち切り）。ノード上限に達したら null */
    fun countSolutions(size: Int, endpoints: List<Pair<Int, Int>>, limit: Int = 2, nodeBudget: Int, onCheck: () -> Unit): Int?
}
object NumberlinkGenerator { val SIZES = listOf(6, 7, 8); fun generate(size: Int, random: Random, onAttempt: () -> Unit = {}): GeneratedNumberlink }
fun NumberlinkState.strokeStart(cell: Int): NumberlinkState
fun NumberlinkState.strokeTo(cell: Int): NumberlinkState
fun NumberlinkState.strokeEnd(): NumberlinkState
val NumberlinkState.isSolved: Boolean   // 規則で判定
```

## 解法

端点から線を 1 マスずつ延ばす探索。毎回、延ばせる先が最も少ない線を選ぶ。刈り込み:

- 空きマスのうち、空きか線の先頭に隣接する数が 1 以下のもの（行き止まり）があれば打ち切る
- 未完成の線の先頭から相手の端点へ、空きマスを通って行けなければ打ち切る
- 空きマスの連結成分が、どの未完成の線の先頭と相手の両方にも接していなければ打ち切る

**刈り込みの誤りは黙って唯一性を壊す。** 刈り込みの無い総当たりと、小さな盤面で個数を照合する（U-03）。

## 生成

1. 盤面を線で覆い尽くす: 1 マスずつの線から始め、隣り合う線の端どうしを無作為につなぐ（線の長さは盤面の辺の 2 倍まで）
2. 長さ 3 未満の線を含む盤面は捨てる
3. 解を 2 つまで探す。別の解が見つかったら、その解が元の線から外れるマスで元の線を 2 本に分け（どちらも長さ 3 以上）、もう一度探す
4. 解の個数が 1 と証明できたら返す。分けられない・ノード上限に達した・分割の回数が上限を超えたら最初からやり直す

モジュールは `:numberlink-core`。
