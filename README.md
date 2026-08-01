# HeadHunt

[![CI](https://github.com/QuQuLa89/Head-Hunt-Plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/QuQuLa89/Head-Hunt-Plugin/actions/workflows/ci.yml)

Kotlin製の[PaperMC](https://papermc.io/)サーバー向け、頭ブロックを使った宝探しミニゲームプラグインです。

頭（スカル）ブロックをマップ内に「宝」として隠し、ソロ戦またはチーム戦で、すべての宝を探し出す速さを競います。

## 機能

- **ソロ戦・チーム戦** — 個人戦またはチーム戦を選択できます。
- **チームモード** — `shared`ではチーム全体で発見状況を共有し、`individual`では開始時点のチームメンバー全員が各自ですべての宝を発見すると勝利します。
- **ゲーム内での宝登録** — 管理者ごとにsetmodeを切り替え、頭ブロックの設置または右クリックで登録・解除できます。
- **宝データの永続化** — 宝の座標は`treasures.yml`に保存され、サーバー再起動後も引き継がれます。
- **安全な保存** — 一時ファイルを使って原子的に保存し、直前の内容を`treasures.yml.bak`へバックアップします。
- **整合性検証・修復** — YAMLとブロック側の宝IDを検証し、修復できます。
- **安全な削除機能** — `deleteall`は30秒以内の確認操作を必要とし、ブロック側の宝IDも削除します。
- **権限対応のタブ補完** — 実行者が使用できるサブコマンドと引数だけを候補に表示します。

## 動作環境

- Paper（またはPaperベースのフォーク） **1.21.4**
- Java 21

## インストール方法

1. ビルド済みの`HeadHunt-<version>.jar`を用意します。[ビルド方法](#ビルド方法)を参照するか、Releasesページから取得してください。
2. JARファイルをサーバーの`plugins/`フォルダーに配置します。
3. サーバーを再起動してください。

## コマンド

すべてのコマンドは`/headhunt`（エイリアス: `/hhunt`）配下にあります。

| コマンド | 説明 | 権限 |
|---|---|---|
| `/headhunt setmode <on\|off>` | 実行した管理者の宝登録モードを切り替えます。 | `headhunt.admin` |
| `/headhunt start <solo\|team> [shared\|individual]` | 宝登録の整合性を確認してゲームを開始します。 | `headhunt.admin` |
| `/headhunt reset` | 発見状況をリセットします。宝の座標定義は保持します。 | `headhunt.admin` |
| `/headhunt stop` | 進行中のゲームを終了します。 | `headhunt.admin` |
| `/headhunt list` | 登録済み宝の一覧を表示します。 | `headhunt.admin` |
| `/headhunt validate` | YAML、座標、頭ブロック、宝IDの整合性を検証します。 | `headhunt.admin` |
| `/headhunt repair` | 不足している宝IDを修復し、無効な座標定義を除去します。 | `headhunt.admin` |
| `/headhunt deleteall [confirm]` | すべての宝情報を削除します。30秒以内の確認が必要です。 | `headhunt.admin` |
| `/headhunt team create <name>` | チームを作成します。 | `headhunt.admin` |
| `/headhunt team join <name> [player]` | プレイヤーをチームへ追加します。 | `headhunt.admin` |
| `/headhunt team leave [player]` | プレイヤーをチームから離脱させます。 | `headhunt.admin` |
| `/headhunt team delete <name>` | チームを削除します。 | `headhunt.admin` |
| `/headhunt team list` | チーム一覧を表示します。 | `headhunt.use` |
| `/headhunt team info [name]` | チームメンバーを表示します。 | `headhunt.use` |

### 宝の登録方法

1. `headhunt.admin`権限を持つプレイヤーが`/headhunt setmode on`を実行します。
2. 次のいずれかを行います。
   - 設置済みの頭ブロックを、メインハンドが空の状態で右クリックします。
   - 新しい頭ブロックを設置します。
3. 登録済みの頭ブロックをスニークしながら右クリックすると、登録を解除できます。
4. 作業後に`/headhunt setmode off`を実行します。

setmodeは実行した管理者だけに適用されます。ゲーム開始時には、すべての管理者のsetmodeが自動的に無効になります。

### 整合性の検証と修復

ゲーム開始時には、次の条件を自動的に検証します。

- 登録ワールドが読み込まれています。
- Y座標がワールドの範囲内です。
- 登録座標に頭ブロックがあります。
- ブロック側の宝IDがYAMLの登録情報と一致しています。
- 複数の宝が同じ座標へ登録されていません。

問題がある場合はゲームを開始しません。`/headhunt validate`で内容を確認し、`/headhunt repair`で修復してください。読み込まれていないワールドの情報は自動削除せず、問題として保持します。

## 権限

| 権限 | デフォルト | 説明 |
|---|---|---|
| `headhunt.admin` | op | ゲーム、宝、チームを管理できます。 |
| `headhunt.use` | true | チーム一覧とチーム情報を表示できます。 |

`headhunt.use`を明示的に拒否された利用者には、基本コマンドとそのタブ補完を表示しません。`headhunt.admin`を持つ管理者はすべてのコマンドを使用できます。

## データと再起動時の動作

- 宝の座標とIDは`plugins/HeadHunt/treasures.yml`へ保存します。
- 保存前のファイルは`plugins/HeadHunt/treasures.yml.bak`へ保持します。
- チーム、メンバー、進行中のゲーム、発見状況、setmodeはメモリ上だけで管理します。
- サーバー再起動またはプラグイン再読み込み後は、チームを再作成してゲームを開始してください。

`deleteall`は読み込み済みワールドの頭ブロックから宝IDを削除します。ワールドが読み込まれていないなどの理由で更新できない宝は削除せずに保持し、管理者へ件数と理由を表示します。

## ビルド方法

Windowsでは次を実行します。

```powershell
.\gradlew.bat clean build
```

LinuxまたはmacOSでは次を実行します。

```bash
./gradlew clean build
```

テストとビルドが成功すると、シェード済みJARを`build/libs/HeadHunt-<version>.jar`へ出力します。

ローカルのPaper 1.21.4テストサーバーを起動する場合は、次を実行します。

```bash
./gradlew runServer
```

## ライセンス

[MIT License](LICENSE)のもとで公開しています。
