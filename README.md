# HeadHunt

[![CI](https://github.com/QuQuLa89/Head-Hunt-Plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/QuQuLa89/Head-Hunt-Plugin/actions/workflows/ci.yml)

Kotlin製の[PaperMC](https://papermc.io/)サーバー向け、頭ブロックを使った宝探しミニゲームプラグインです。

頭（スカル）ブロックをマップ内に「宝」として隠しておき、ソロ戦またはチーム戦でプレイヤーが右クリックしてすべての宝を探し出す速さを競います。

## 機能

- **ソロ戦・チーム戦** — 個人戦にするか、チームに分かれて競うか選べる。
- **チームモード** — `shared`（チームメイトの誰かが発見すればチーム全体としてカウント）と`individual`（メンバー各自が発見する必要あり）の2種類。
- **ゲーム内での宝登録** — setmodeを切り替えて頭ブロックを設置・右クリックするだけで登録／解除でき、コンフィグ編集は不要。
- **宝データの永続化** — 宝の座標は`treasures.yml`に保存され、サーバー再起動後も引き継がれる。
- **安全な削除機能** — `deleteall`は30秒以内の確認操作が必要で、誤削除を防止する。
- **タブ補完** — すべてのサブコマンド・引数に対応。

## 動作環境

- Paper（またはPaperベースのフォーク） **1.21.x**
- Java 21

## インストール方法

1. ビルド済みの`HeadHunt-<version>.jar`を用意する（[ビルド方法](#ビルド方法)を参照、またはReleasesページから取得）。
2. jarファイルをサーバーの`plugins/`フォルダに入れる。
3. サーバーを再起動、またはリロードする。

## コマンド

すべてのコマンドは`/headhunt`（エイリアス: `/hhunt`）配下にあります。

| コマンド | 説明 | 権限 |
| --- | --- | --- |
| `/headhunt setmode <on\|off>` | 宝登録モードの切り替え | `headhunt.admin` |
| `/headhunt start <solo\|team> [shared\|individual]` | 宝探しを開始 | `headhunt.admin` |
| `/headhunt reset` | 発見状況をリセット（宝の座標定義は保持） | `headhunt.admin` |
| `/headhunt stop` | 進行中の宝探しを終了 | `headhunt.admin` |
| `/headhunt list` | 登録済み宝の一覧表示 | `headhunt.admin` |
| `/headhunt deleteall [confirm]` | 全ての宝を削除（要確認） | `headhunt.admin` |
| `/headhunt team create <name>` | チームを作成 | `headhunt.admin` |
| `/headhunt team join <name> [player]` | プレイヤーをチームに追加 | `headhunt.admin` |
| `/headhunt team leave [player]` | プレイヤーをチームから離脱させる | `headhunt.admin` |
| `/headhunt team delete <name>` | チームを削除 | `headhunt.admin` |
| `/headhunt team list` | チーム一覧を表示 | `headhunt.use` |
| `/headhunt team info [name]` | チームメンバーを表示 | `headhunt.use` |

### 宝の登録方法

1. `/headhunt setmode on`
2. op権限を持った状態で、以下のいずれかを行う。
   - 既に設置済みの頭ブロックを、手ぶらの状態で右クリックする
   - 新しく頭ブロックを設置する
3. スニーク＋右クリックで、登録済みの頭ブロックを登録解除できます。
4. 終わったら`/headhunt setmode off`を実行します。

## 権限

| 権限 | デフォルト | 説明 |
| --- | --- | --- |
| `headhunt.admin` | op | 管理系コマンド（setmode, start, reset, stop, deleteall, team管理, list） |
| `headhunt.use` | true | 基本コマンド（team list, team info） |

## ビルド方法

```bash
./gradlew shadowJar
```

シェード済みjarは`build/libs/`に出力されます。

プラグインを読み込んだ状態でローカルのテストサーバーを起動する場合:

```bash
./gradlew runServer
```

## ライセンス

[MIT License](LICENSE)のもとで公開しています。
