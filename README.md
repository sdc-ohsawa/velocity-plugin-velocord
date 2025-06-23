# Velocord - Velocity Discord Plugin

MinecraftのVelocityプロキシサーバー用のDiscord連携プラグインです。Discord連携、サーバー統合チャット、Discordロールに基づく権限管理、アカウント連携システム、自動Discord サーバー検出機能、サーバー状態監視機能を提供します。

[![Build Status](https://github.com/sdc-ohsawa/velocity-plugin-velocord/workflows/Build%20and%20Release/badge.svg)](https://github.com/sdc-ohsawa/velocity-plugin-velocord/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Version](https://img.shields.io/github/v/release/sdc-ohsawa/velocity-plugin-velocord)](https://github.com/sdc-ohsawa/velocity-plugin-velocord/releases)

## 📥 ダウンロード

最新版は [Releases](https://github.com/sdc-ohsawa/velocity-plugin-velocord/releases) ページからダウンロードできます。

## 🚀 機能

### 🔧 新機能（v4.7.0）
- **🤖 自動Discord サーバー検出**: Botが参加している全サーバーを自動検出・設定生成
- **🎨 プレイヤースキン付きWebhook**: MinecraftプレイヤーのスキンアイコンでDiscordに送信
- **⚡ リアルタイム権限チェック**: サーバー移動時の即座な権限確認（キャッシュレス）
- **🔄 設定リロード機能**: プロキシ再起動不要での設定変更適用
- **🔍 強化されたデバッグ機能**: 詳細な権限・連携状態の確認

### 基本機能
- **Discord連携**: MinecraftサーバーとDiscordサーバー間のチャット連携
- **サーバー統合チャット**: 複数のMinecraftサーバー間でのチャット統合
- **権限管理**: Discordロールに基づくサーバーアクセス制御
- **アカウント連携**: DiscordアカウントとMinecraftアカウントの安全な連携

### アカウント連携機能
- **セキュアな連携**: 6桁の認証コードによる安全な連携システム
- **データベース対応**: MySQL/SQLiteでの連携データ管理
- **自動UUID管理**: online-modeでの正しいUUID管理
- **連携強制**: 未連携プレイヤーのアクセス制御

### 権限システム
- **シンプルな仕様**: 明確な5ステップの権限チェック
- **リアルタイム判定**: キャッシュレスでの即座な権限確認
- **サーバー別権限**: サーバーごとの細かなアクセス制御
- **複数Discord サーバー対応**: 複数のDiscordサーバーからの権限管理

### 📡 サーバー状態監視機能
- 定期的なサーバーpingによる稼働状態チェック
- サーバーの起動/停止をDiscordに自動通知
- プレイヤー接続状況を考慮した賢い監視
- 専用チャンネルでの状態通知（設定可能）

### 🎨 Embed形式メッセージ
- **色分けされたサイドバー**: 緑（参加・オンライン）、赤（退出・オフライン）、青（移動）
- **美しい表示形式**: Discord Embedによる構造化されたメッセージ
- **統一されたデザイン**: 全てのプレイヤーアクションとサーバー状態でEmbed形式

## 📋 権限システムの仕様

プラグインは以下の5ステップで権限をチェックします：

1. **サーバー接続時にDBアクセス** - 連携がされているか確認
2. **Discord UserID取得** - 連携先のDiscordユーザーIDを控える
3. **Guildロール取得** - 参加中のDiscordサーバーのロールを取得
4. **ロール確認** - DiscordユーザーIDから所持しているロールを確認
5. **権限判定** - config.ymlで設定されたロールがあったらアクセス権限を付与

## 🛠️ セットアップ

### 1. プラグインのインストール

1. [Releases](https://github.com/sdc-ohsawa/velocity-plugin-velocord/releases) から最新版（v4.7.0）をダウンロード
2. VelocityサーバーのPluginsディレクトリに配置
3. サーバーを再起動

### 2. Discord Botの設定

1. [Discord Developer Portal](https://discord.com/developers/applications)でアプリケーションを作成
2. Botを作成し、トークンを取得
3. 必要な権限を付与:
   - Send Messages
   - Use Webhooks
   - Read Message History
   - Use Slash Commands
   - Read Server Members（権限チェック用）
4. Botを対象のDiscordサーバーに招待

### 3. 設定ファイルの編集

プラグイン初回起動後、`plugins/VelocityDiscord/config.yml` を編集：

```yaml
# Discord設定
discord:
  # Discord Bot Token（必須）
  token: "YOUR_DISCORD_BOT_TOKEN"
  
  # 自動検出機能（推奨: true）
  auto_guild_detection: true
  
  # 複数のDiscordサーバー設定（自動生成される）
  guilds: {}

# チャット設定
chat:
  format: "&7[&a%server%&7] &f%player%&7: &f%message%"
  discord_format: "**[%server%]** %player%: %message%"
  discord_to_game_enabled: true
  discord_to_game_format: "&9[Discord] &b%player%&7: &f%message%"
  game_to_discord_enabled: true

# メッセージカスタマイズ設定（Embed形式）
messages:
  # Embed形式でメッセージを送信（緑・赤・青のサイドバー表示）
  use_embed_format: true
  # プレイヤー参加メッセージ（緑色のEmbed）
  player_join: "🟢 **%player%** joined the server"
  # プレイヤー退出メッセージ（赤色のEmbed）
  player_leave: "🔴 **%player%** left the server"
  # サーバー移動メッセージ（青色のEmbed）
  player_move: "🔵 **%player%**が**%from%**から**%to%**に移動しました"
  # サーバーオンラインメッセージ（緑色のEmbed）
  server_online: "🟢 **%server%** server is online"
  # サーバーオフラインメッセージ（赤色のEmbed）
  server_offline: "🔴 **%server%** server is offline"

# 権限設定（手動設定が必要）
permissions:
  discordRoleMapping:
    "YOUR_GUILD_ID":
      roles:
        "ROLE_ID_1":
          name: "管理者"
          serverAccess:
            seikatsu: true
            sigen: true
            lobby: true
            vip: true
        "ROLE_ID_2":
          name: "一般メンバー"
          serverAccess:
            seikatsu: true
            sigen: true
            lobby: false
            vip: false

# サーバー表示名設定
servers:
  seikatsu: "生存サーバー"
  sigen: "資源サーバー"
  lobby: "ロビーサーバー"
  vip: "VIPサーバー"

# データベース設定
database:
  type: "sqlite"  # または "mysql"
  mysql:
    host: "localhost"
    port: 3306
    database: "velocity_discord"
    username: "your_username"
    password: "your_password"
    connection_pool_size: 10

# アカウント連携設定
account_linking:
  enabled: true
  required: true
  verification_timeout: 10
  connection_denied_message: |
    &cこのサーバーを利用するにはDiscord連携が必要です
    &eあなたの認証コード: &b%code%
    &7Discordで /link %code% を実行してください
    &7連携完了後、再度接続してください

# サーバー状態監視設定
server-status:
  enabled: true
  scan-delay: 30
  ping-timeout: 3000
  discord-channel: ""
```

## 🎮 使用方法

### 🤖 自動Discord サーバー検出

1. **Bot Tokenを設定**してサーバー起動
2. **自動検出が実行**され、参加中のDiscordサーバーが`config.yml`に追加される
3. **手動でchannel_idを設定**（必要に応じてwebhookUrlも）
4. **ロール権限を手動設定**（permissionsセクション）

### アカウント連携

#### 初回連携
1. プレイヤーがサーバーに接続を試行
2. 未連携の場合、6桁の認証コードが表示される
3. Discordで `/link 認証コード` を実行
4. 連携完了後、サーバーに再接続

#### Discordコマンド
- `/link <認証コード>` - アカウント連携

### 管理コマンド

#### サーバー管理者用
- `/vdreload` - 設定ファイルの再読み込み（プロキシ再起動不要）
- `/vddebug <player>` - プレイヤー情報の詳細確認
- `/vddebug db` - データベース内容の確認
- `/vddebug cleanup` - 重複データのクリーンアップ

### 権限設定

- `velocitydiscord.reload` - リロードコマンドの実行権限
- `velocitydiscord.debug` - デバッグコマンドの実行権限

## 🔧 設定項目詳細

### 自動Discord サーバー検出
- **auto_guild_detection: true** - Botが参加している全サーバーを自動検出
- 初回起動時に参加中のDiscordサーバーが自動で`guilds`セクションに追加される
- `channel_id`と`webhookUrl`は手動設定が必要
- ロール権限設定は`permissions.discordRoleMapping`で手動設定

### データベース設定
- **SQLite**: プラグインディレクトリに自動作成（`plugins/VelocityDiscord/accounts.db`）
- **MySQL**: 外部MySQLサーバーとの接続、接続プール対応

### アカウント連携設定
- `enabled`: 連携機能の有効/無効
- `required`: 未連携プレイヤーの接続拒否
- `verification_timeout`: 認証コードの有効期限（分）

### 権限設定
- **複数Discord サーバー対応**: サーバーごとの個別ロール設定
- **リアルタイム権限チェック**: キャッシュレスでの即座な権限確認
- **サーバー別アクセス制御**: サーバーごとの細かな権限管理

### サーバー状態監視設定
- `enabled`: 監視機能の有効/無効
- `scan-delay`: 監視間隔（秒）
- `ping-timeout`: 接続タイムアウト（ミリ秒）
- `discord-channel`: 状態通知専用チャンネルID

### Embed形式メッセージ設定
- `use_embed_format`: Embed形式の有効/無効（true推奨）
- **色分けルール**:
  - 🟢 緑色: プレイヤー参加、サーバーオンライン
  - 🔴 赤色: プレイヤー退出、サーバーオフライン
  - 🔵 青色: サーバー間移動

## 🔄 更新履歴

### v4.7.0 (最新)
- **📡 サーバー状態監視機能**: バックエンドサーバーの稼働状態監視とDiscord通知
- **🎨 Embed形式メッセージ**: 色分けされたサイドバー付きの美しいDiscordメッセージ
- **🖥️ サーバー状態コマンド**: `/serverstatus`コマンドによる手動状態確認
- **⚙️ カスタマイズ可能通知**: 設定ファイルからメッセージテンプレートを変更可能
- **🔔 専用チャンネル対応**: サーバー状態通知専用のDiscordチャンネル設定
- **🟢🔴🔵 視覚的色分け**: 参加（緑）・退出（赤）・移動（青）の直感的な色分け

### v4.6.8
- **🤖 自動Discord サーバー検出機能**: 参加中のサーバーを自動検出・設定生成
- **👤 プレイヤースキン付きWebhook**: Minecraftスキンアイコンでメッセージ送信
- **⚡ リアルタイム権限システム**: サーバー移動時の即座な権限チェック
- **🔄 設定リロード機能**: `/vdreload`コマンドでプロキシ再起動不要の設定更新
- **🔍 デバッグ機能強化**: 詳細な権限状態・連携情報の確認機能
- **🐛 重要な不具合修正**:
  - ロール同期問題の完全解決
  - アクセス権限エラー時のスタック問題解決
  - 権限拒否時の適切な切断処理

### v4.2.0
- **権限システム簡素化**: 明確な5ステップ仕様に統一
- **Real UUID対応**: temp UUIDシステムを削除し、online-modeで正しいUUID管理
- **ReloadCommand追加**: `/vdreload`で設定ファイル再読み込み
- **MySQLドライバー修正**: 接続問題の解決
- **ログ最適化**: 不要なデバッグログを削除

### v2.4.0
- UUID移行システムの実装
- 権限キャッシュシステム
- デバッグコマンドの追加

### v1.0.0
- 初回リリース
- 基本的なDiscord連携機能
- サーバー統合チャット

## ⚠️ 注意事項

### online-mode設定
- **online-mode=true** での使用を前提としています
- offline-modeでは正しく動作しない可能性があります

### Discord Bot権限
- **Read Server Members**権限が必要です（権限チェック用）
- 権限が不足している場合、正しく動作しません

### 設定について
- **自動検出後の手動設定**: channel_idとロール権限は手動設定が必要
- **ロールID管理**: 開発者モードで正確なIDを取得してください
- **複数サーバー対応**: 各Discordサーバーごとに個別の権限設定が可能

### データベース
- 初回起動時にテーブルが自動作成されます
- MySQLを使用する場合は事前にデータベースを作成してください

## 🛠️ 開発

### 必要環境
- Java 17以上
- Gradle 7.6.3以上
- Velocity API 3.1.1

### ビルド
```bash
git clone https://github.com/sdc-ohsawa/velocity-plugin-velocord.git
cd velocity-plugin-velocord
./gradlew clean shadowJar
```

出力ファイル: `build/libs/velocord-4.7.0.jar`

### 依存関係
- **Velocity API** 3.1.1 - Velocityプロキシ連携
- **JDA** 5.6.1 - Discord API連携
- **HikariCP** 5.0.1 - 高性能データベース接続プール
- **MySQL Connector/J** 8.0.33 - MySQL接続
- **SQLite JDBC** 3.42.0.0 - SQLite接続
- **SnakeYAML** 1.33 - YAML設定ファイル処理
- **Gson** 2.10.1 - JSON処理（Webhook用）

## 📄 ライセンス

このプロジェクトはMITライセンスの下で公開されています。詳細は [LICENSE](LICENSE) ファイルを参照してください。

## 🐛 サポート

問題や質問がある場合は、[GitHubのIssues](https://github.com/sdc-ohsawa/velocity-plugin-velocord/issues)ページで報告してください。

## 🤝 貢献

プルリクエストや機能提案を歓迎します。大きな変更を行う前に、まずIssueで議論してください。

## 🏆 特徴

- **🔧 簡単セットアップ**: 自動検出機能により初期設定が簡単
- **⚡ 高性能**: HikariCP接続プールによる高速データベースアクセス
- **🛡️ セキュア**: online-mode対応の安全なUUID管理
- **🎨 美しいUI**: プレイヤースキン付きDiscordメッセージ
- **🔄 メンテナンス性**: プロキシ再起動不要の設定リロード
- **🐛 安定性**: 重要な問題がすべて修正済み 
