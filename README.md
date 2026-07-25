# YT Rank Lab

YT Rank Labは、YouTube動画を独自スコアで評価し、GitHub Pages上で表示する静的ランキングアプリです。フロントエンドはHTML、CSS、vanilla JavaScriptのみで動作し、ランキングデータはKotlin CLIが生成したJSONを読み込みます。

公開サイト: https://shirasel.github.io/yt.jp-rank-lab/home.html

## 主な機能

- 24時間、本日、7日間、急上昇、発掘、過去日のランキング表示
- ジャンル別ランキング表示
- YouTube Data APIを使った実データ収集
- ローカル開発向けのモックデータ生成
- データベースを使わないJSONベースの保存
- GitHub Actionsによるテストと定期更新
- GitHub Pages向けの`docs`ディレクトリ構成

## データベースを使わない理由

このアプリはGitHub Pagesで公開する前提です。公開サイトは事前生成されたJSONを読むだけなので、データベースや常駐Webサーバーを使わないことで、運用コスト、秘密情報管理のリスク、不要な複雑さを抑えています。

## 技術構成

- Kotlin CLI
- Java 25
- Kotlin Gradle Plugin 2.1.21
- Gradle Kotlin DSL
- kotlinx.serialization
- Ktor Client
- SLF4J
- JUnit 5
- MockK
- HTML、CSS、vanilla JavaScript
- GitHub Actions
- GitHub Pages

## ディレクトリ構成

```text
.
├─ .github/workflows/
├─ config/
├─ mock/
├─ docs/
├─ src/main/kotlin/com/ytranklab/
├─ src/test/kotlin/com/ytranklab/
├─ build.gradle.kts
└─ README.md
```

## ローカル環境

Java 25をインストールしてください。Gradle Wrapperを同梱しているため、Gradleをグローバルにインストールする必要はありません。ローカル用ヘルパーは、このプロジェクト内の`.gradle-home`にGradleキャッシュを保持します。

Kotlin 2.1.21はまだJVM 25バイトコードを出力しないため、Java 25 toolchain上でビルドしつつ、JVM 23バイトコードをターゲットにしています。

モックデータ生成:

```bash
tools/generate-mock-rankings.cmd
```

YouTube Data APIを使った実データ生成:

```bash
tools/generate-youtube-rankings.cmd
```

テスト:

```bash
tools/run-gradle-local.cmd test build
```

ローカル一括検証:

```bash
tools/verify-ranking-app.cmd
```

生成JSONの検証:

```bash
tools/validate-generated-data.cmd
```

静的プレビュー:

```bash
tools/preview-github-pages.cmd
```

## YouTube Data API

ローカルでは`.env`または環境変数に`YOUTUBE_API_KEY`を設定してください。GitHub Actionsでは、Repository Secretに同じ名前で登録します。

フロントエンドからYouTube Data APIを直接呼び出してはいけません。公開サイトは生成済みJSONだけを読み込みます。

Kotlin CLIは`config/sources.yml`から以下の収集元を読み込みます。

- 手動設定した動画ID
- 有効化されたチャンネルIDの最新投稿
- 設定された検索キーワード
- 指定地域の人気動画

収集元には優先度を設定できます。数値が小さいほど先に収集されるため、`collection.maxEstimatedQuotaUnits`が厳しい場合でも重要な収集元を優先できます。

```yaml
channels:
  - id: UCxxxxxxxxxxxxxxxxxxxxxx
    enabled: true
    priority: 50

keywords:
  - term: Minecraft
    priority: 100
  - term: 生成AI
    priority: 190

collection:
  popularPriority: 900
```

`collection.maxEstimatedQuotaUnits`は、高コストなAPI呼び出し前に推定使用量を抑制します。上限を超える収集元はスキップされ、`generation-summary.json`に記録されます。

実際のAPIキーをGit管理しないでください。`.env`と`.env.*`はGit管理外で、`.env.example`には変数名だけを記載します。

秘密情報を以下に出力しないでください。

- GitHub Actionsログ
- GitHub PagesのHTML
- JavaScriptファイル
- 公開JSON
- エラーメッセージ
- テスト結果
- ビルド成果物

## GitHub Actions

2つのworkflowがあります。

- `.github/workflows/test.yml`
  - pushとpull requestで実行
  - Node 24対応のGitHub Actionsを使用
  - ビルドとテストを実行
  - モック生成後の公開JSONを検証
  - 失敗時にGitHub Issueを作成または更新
  - `YOUTUBE_API_KEY`は不要
  - 生成差分はコミットしない
- `.github/workflows/update-rankings.yml`
  - 定期実行と手動実行に対応
  - Repository Secretの`YOUTUBE_API_KEY`で実データを更新
  - 生成JSONを検証してから変更検出
  - 失敗時にGitHub Issueを作成または更新
  - `docs/data`に変更がある場合だけコミット
  - concurrencyで更新処理の重複実行を防止

ランキング更新workflowのcronはUTCで以下です。

```text
10 3,9,15,21 * * *
```

日本時間では以下に実行されます。

- 00:10
- 06:10
- 12:10
- 18:10

手動実行時のみ、`use_mock`を`true`にするとモックデータ生成も可能です。本番更新では`use_mock=false`で実行してください。

## GitHub Pages

GitHub Pagesは、デフォルトブランチの`docs`ディレクトリから公開してください。フロントエンドは相対パスを使うため、以下のようなリポジトリ配下URLでも動作します。

```text
https://USERNAME.github.io/REPOSITORY_NAME/
```

主な静的ページ:

```text
docs/index.html
docs/home.html
docs/rankings/overall-ranking.html
docs/rankings/genre-ranking.html
docs/rankings/history-ranking.html
docs/operations/generation-log.html
docs/videos/video-detail.html?id=VIDEO_ID
```

フロントエンドは`textContent`とDOM APIで生成コンテンツを描画します。秘密情報を埋め込まず、YouTube Data APIも呼び出しません。

## ランキングアルゴリズム

スコアは短期的な伸びを重視します。`ranking.minimumViewIncrease`未満の再生増加しかない動画はランキング候補から除外されます。エンゲージメントは直近差分を使い、極端な比率は上限で抑制します。

```text
rawScore =
(
  log10(viewVelocity + 1) * velocityWeight
  + log10(subscriberRatio * 10000 + 1) * subscriberRatioWeight
  + log10(cappedDeltaLikeRate * 1000 + 1) * likeRateWeight
  + log10(cappedDeltaCommentRate * 5000 + 1) * commentRateWeight
) * ageDecay
```

重みとしきい値は`config/ranking.yml`で管理します。

## ジャンル判定

ジャンルは`config/genres.yml`で定義します。現在はタイトル、説明文、YouTubeカテゴリ、チャンネル情報を使ったルールベース分類です。

## データ保持

初期ポリシー:

- 追跡動画: 最大500本
- 総合ランキング: 上位100本
- ジャンルランキング: ジャンルごとに上位50本
- 詳細統計: 90日
- ランキング履歴: 365日

ランキングJSONの生成後、保持期間外の履歴と、最新総合ランキングに含まれない古い動画詳細JSONを削除します。

## 生成JSON

ランキング生成では以下のJSONを書き込みます。

```text
docs/data/latest/overall.json
docs/data/latest/today.json
docs/data/latest/seven-days.json
docs/data/latest/trending.json
docs/data/latest/discovery.json
docs/data/latest/generation-summary.json
docs/data/latest/validation-report.json
docs/data/latest/history-index.json
docs/data/latest/genres/{genre}.json
docs/data/videos/{videoId}.json
docs/data/statistics/latest.json
docs/data/statistics/videos/{videoId}.json
docs/data/rankings/videos/{videoId}.json
docs/data/history/YYYY/MM/DD.json
```

JSONは一時ファイルへ書き込んでから最終ファイルに置き換えます。`generation-summary.json`には収集件数、スキップされた収集元、推定YouTube API使用量、保持期間クリーンアップ件数を記録しますが、APIキーは保存しません。

---

## English

# YT Rank Lab

YT Rank Lab is a static GitHub Pages application that ranks YouTube videos with an original scoring algorithm. The frontend uses only HTML, CSS, and small vanilla JavaScript. Ranking data is generated by a Kotlin CLI and stored as JSON files.

## Features

- Daily overall ranking data
- Genre ranking data
- Mock data generation for local development
- JSON-based storage without a database
- GitHub Actions workflows for tests and scheduled updates
- GitHub Pages-ready `docs` directory

## Why No Database

The application is designed for GitHub Pages. The public site only needs to read pre-generated JSON files, so a database and permanent web server would add operational cost, secret-management risk, and unnecessary complexity.

## Technology

- Kotlin CLI
- Java 25
- Kotlin Gradle Plugin 2.1.21
- Gradle Kotlin DSL
- kotlinx.serialization
- Ktor Client
- SLF4J
- JUnit 5
- MockK
- HTML, CSS, vanilla JavaScript
- GitHub Actions
- GitHub Pages

## Directory Structure

```text
.
├─ .github/workflows/
├─ config/
├─ mock/
├─ docs/
├─ src/main/kotlin/com/ytranklab/
├─ src/test/kotlin/com/ytranklab/
├─ build.gradle.kts
└─ README.md
```

## Local Setup

Install Java 25. The repository includes Gradle Wrapper, so a global Gradle installation is not required. Local helper tools keep Gradle caches under this project in `.gradle-home`.

Kotlin 2.1.21 does not emit JVM 25 bytecode yet, so the build runs on the Java 25 toolchain while targeting JVM 23 bytecode.

Kotlin compilation runs in the Gradle process to avoid writing Kotlin daemon files outside the project-specific environment.

Mock generation:

```bash
tools/generate-mock-rankings.cmd
```

Real generation:

```bash
tools/generate-youtube-rankings.cmd
```

Tests:

```bash
tools/run-gradle-local.cmd test build
```

Full local verification:

```bash
tools/verify-ranking-app.cmd
```

Generated JSON validation:

```bash
tools/validate-generated-data.cmd
```

Validation writes `docs/data/latest/validation-report.json`, which is displayed on the operation log page. The report timestamp follows the validated ranking data timestamp to avoid update-only diffs from validation execution time.
GitHub Actions also writes a safe validation summary with status and counts only.

Static preview:

```bash
tools/preview-github-pages.cmd
```

## YouTube Data API

Set `YOUTUBE_API_KEY` in your local `.env` file or environment variables. For GitHub Actions, add the same name to repository secrets.

The frontend must never call the YouTube Data API directly. It reads generated JSON only.

The Kotlin CLI collects candidates from `config/sources.yml`:

- manually configured video IDs
- latest uploads from enabled channel IDs
- configured search keywords
- most popular videos for the configured region

The production default keeps manual videos and channels empty, uses Japanese search keywords, and includes JP popular videos. This avoids committing sample video IDs while keeping the scheduled workflow useful from the first real API run.

Collection sources can define priorities. Lower numbers are collected first, so higher-value sources remain active when `collection.maxEstimatedQuotaUnits` is tight:

```yaml
channels:
  - id: UCxxxxxxxxxxxxxxxxxxxxxx
    enabled: true
    priority: 50

keywords:
  - term: Minecraft
    priority: 100
  - term: 生成AI
    priority: 190

collection:
  popularPriority: 900
```

String-only keywords are still supported and default to priority `200`. Channels default to priority `100`, and popular videos default to priority `300` when no explicit value is set.

`collection.maxEstimatedQuotaUnits` caps source collection before expensive API calls run. When the estimated budget would be exceeded, the collector skips the source, records `quota budget limit` in `generation-summary.json`, and continues with lower-cost sources and already collected video IDs.

The API client batches `videos.list` and `channels.list` calls in groups of up to 50 IDs, retries transient network failures with exponential backoff, and stops before writing JSON when no public videos are collected.

Do not commit actual API keys. `.env` and `.env.*` are ignored by Git, while `.env.example` contains only variable names.

Secrets must not be written to:

- GitHub Actions logs
- GitHub Pages HTML
- JavaScript files
- Public JSON
- Error messages
- Test output
- Build artifacts

## GitHub Actions Schedule

Two workflows are included:

- `.github/workflows/test.yml`
  - runs on push and pull request
  - uses Node 24-compatible GitHub Actions
  - builds and tests with mock data only
  - validates generated public JSON after mock generation
  - opens or updates a GitHub Issue when the workflow fails
  - does not require `YOUTUBE_API_KEY`
  - does not commit generated changes
- `.github/workflows/update-rankings.yml`
  - runs on schedule and manual dispatch
  - uses Node 24-compatible GitHub Actions
  - uses `YOUTUBE_API_KEY` from repository secrets for real updates
  - validates generated public JSON before detecting changes
  - opens or updates a GitHub Issue when the workflow fails
  - commits only when `docs/data` changes
  - prevents overlapping update runs with workflow concurrency

The ranking update workflow uses this UTC cron:

```text
10 3,9,15,21 * * *
```

In Japan Standard Time, this runs at:

- 00:10
- 06:10
- 12:10
- 18:10

Manual dispatch can also run with mock data by setting `use_mock` to `true`.

## GitHub Pages

Enable GitHub Pages from the `docs` directory. Use relative paths in frontend code so the site works under repository subpaths such as:

```text
https://USERNAME.github.io/REPOSITORY_NAME/
```

Static pages:

```text
docs/index.html
docs/home.html
docs/rankings/overall-ranking.html
docs/rankings/genre-ranking.html
docs/rankings/history-ranking.html
docs/operations/generation-log.html
docs/videos/video-detail.html?id=VIDEO_ID
```

The frontend uses `textContent` and DOM APIs for generated content. It does not embed secrets and does not call the YouTube Data API.

`docs/index.html` is kept only as the required GitHub Pages entry point and redirects to `docs/home.html`.

The home page and operation log display `docs/data/latest/generation-summary.json` and `docs/data/latest/validation-report.json` so update freshness, validation status, health checks, source counts, estimated quota units, and retention cleanup counts can be checked from GitHub Pages.

For a public repository, set Pages to publish from the `docs` directory on the default branch.

## Ranking Algorithm

The raw score prioritizes short-term growth. Videos with fewer than `ranking.minimumViewIncrease` new views are excluded from the ranking candidates. Engagement uses recent deltas when available, and extreme rates are capped before scoring.

```text
rawScore =
(
  log10(viewVelocity + 1) * velocityWeight
  + log10(subscriberRatio * 10000 + 1) * subscriberRatioWeight
  + log10(cappedDeltaLikeRate * 1000 + 1) * likeRateWeight
  + log10(cappedDeltaCommentRate * 5000 + 1) * commentRateWeight
) * ageDecay
```

Weights and thresholds live in `config/ranking.yml`.

`ranking.minimumSubscriberCount` is the floor for known subscriber counts. When a channel hides subscriber counts, `ranking.unknownSubscriberCount` is used instead so hidden subscriber counts do not automatically receive the strongest small-channel boost.

`ranking.minimumViewIncrease` controls the minimum recent view increase required for a video to enter generated rankings.

## Genre Rules

Genres are defined in `config/genres.yml`. The first implementation uses rule-based classification over title, description, YouTube category, and channel metadata.

## Data Retention

Initial policy:

- Tracked videos: 500 maximum
- Overall ranking: top 100
- Genre ranking: top 50 per genre
- Detailed statistics: 90 days
- Ranking history: 365 days

After ranking JSON is generated successfully, `HistoryRetentionService` removes ranking history outside the configured retention window and removes stale video detail JSON files that are no longer present in the latest overall ranking.

## Current Phase

The current implementation includes scheduled GitHub Actions updates and production data-retention cleanup. The repository can build, test, generate mock rankings, update real ranking JSON from the YouTube Data API through GitHub Actions, and keep generated JSON from growing indefinitely.

## Generated JSON

Ranking generation writes:

```text
docs/data/latest/overall.json
docs/data/latest/today.json
docs/data/latest/seven-days.json
docs/data/latest/trending.json
docs/data/latest/discovery.json
docs/data/latest/generation-summary.json
docs/data/latest/validation-report.json
docs/data/latest/history-index.json
docs/data/latest/genres/{genre}.json
docs/data/videos/{videoId}.json
docs/data/statistics/latest.json
docs/data/statistics/videos/{videoId}.json
docs/data/rankings/videos/{videoId}.json
docs/data/history/YYYY/MM/DD.json
```

JSON writes use temporary files and replace the final files only after serialization succeeds.

`generation-summary.json` records collection counts, skipped sources, estimated YouTube quota units, and retention cleanup counts. It never stores API keys.

Per-video statistics history is written to `docs/data/statistics/videos/{videoId}.json`. The video detail page loads this file and displays recent view, like, comment, and subscriber-count history.

Per-video ranking history is written to `docs/data/rankings/videos/{videoId}.json`. The video detail page loads this file and displays recent overall-rank and score history.
