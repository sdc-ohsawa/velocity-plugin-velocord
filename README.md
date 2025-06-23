# 🎮 Velocord

**VelocityプロキシサーバーとDiscordを繋ぐ統合プラグイン**

[![Version](https://img.shields.io/github/v/release/sdc-ohsawa/velocity-plugin-velocord)](https://github.com/sdc-ohsawa/velocity-plugin-velocord/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

MinecraftサーバーとDiscordサーバーを連携し、チャット統合・権限管理・アカウント連携・サーバー監視を提供します。

---

## ✨ 主な機能

🔗 **Discord連携**: MinecraftとDiscord間のリアルタイムチャット  
🛡️ **権限管理**: Discordロールに基づくサーバーアクセス制御  
👤 **アカウント連携**: セキュアな6桁コード認証システム  
📡 **サーバー監視**: 自動状態チェックとDiscord通知  
🎨 **美しい通知**: 色分けされたEmbed形式メッセージ  

---

## 📥 ダウンロード

[**最新版をダウンロード**](https://github.com/sdc-ohsawa/velocity-plugin-velocord/releases)

---

## 🚀 簡単セットアップ

### 1️⃣ プラグインインストール
```
1. jarファイルをVelocityの plugins/ フォルダに配置
2. Velocityサーバーを再起動
```

### 2️⃣ Discord Bot作成
```
1. Discord Developer Portal でBotを作成
2. Bot TokenをコピーしてWallet
3. 必要な権限を付与:
   ✅ Send Messages
   ✅ Use Webhooks  
   ✅ Read Server Members
4. BotをDiscordサーバーに招待
```

### 3️⃣ 設定ファイル編集
`plugins/VelocityDiscord/config.yml` を編集：

```yaml
discord:
  # ここにBot Tokenを入力
  token: "YOUR_DISCORD_BOT_TOKEN"
  
  # 自動検出を有効化（推奨）
  auto_guild_detection: true

# アカウント連携を有効化
account_linking:
  enabled: true
  required: true  # 連携必須にする場合
```

### 4️⃣ チャンネル設定
```
1. サーバー再起動後、config.ymlに参加中のDiscordサーバーが自動追加
2. channel_id を手動で設定（チャット連携用）
3. 権限設定を追加（後述）
```

---

## 🔧 権限設定

Discordロールに基づいてサーバーアクセスを制御できます：

```yaml
permissions:
  discordRoleMapping:
    "あなたのDiscordサーバーID":
      roles:
        "管理者ロールID":
          name: "管理者"
          serverAccess:
            lobby: true
            survival: true
            creative: true
        "メンバーロールID":
          name: "一般メンバー"
          serverAccess:
            lobby: true
            survival: true
            creative: false
```

**📋 ロールIDの取得方法:**
1. Discordで開発者モードを有効化
2. ロールを右クリック → "IDをコピー"
3. 設定ファイルに貼り付け

---

## 🎮 使い方

### プレイヤー向け
- **アカウント連携**: サーバー接続時に表示される6桁コードでDiscordの `/link コード` を実行
- **チャット**: MinecraftとDiscordで相互にチャットが可能

### 管理者向け
- `/vdreload` - 設定ファイル再読み込み（再起動不要）
- `/vddebug <プレイヤー名>` - プレイヤー情報確認
- `/serverstatus` - サーバー状態確認

---

## 📡 サーバー監視機能

バックエンドサーバーの稼働状態を自動監視し、Discordに通知します：

```yaml
server-status:
  enabled: true           # 監視機能有効
  scan-delay: 30         # 30秒間隔でチェック
  discord-channel: ""    # 専用通知チャンネルID（省略可）
```

🟢 **サーバーオンライン** / 🔴 **サーバーオフライン** で視覚的に状態を表示

---

## 💾 データベース

### SQLite（推奨・簡単）
```yaml
database:
  type: "sqlite"
```
プラグインが自動でデータベースファイルを作成します。

### MySQL（上級者向け）
```yaml
database:
  type: "mysql"
  mysql:
    host: "localhost"
    database: "velocity_discord"
    username: "user"
    password: "pass"
```

---

## ⚠️ 重要な注意事項

### 必須要件
- **Java 17以上**
- **Velocity 3.1.1以上**
- **online-mode=true** (offline-modeは非対応)

### Discord Bot権限
- `Send Messages` - メッセージ送信
- `Use Webhooks` - プレイヤースキン表示
- `Read Server Members` - 権限チェック (★重要)

### セキュリティ
- **リアルタイム権限チェック**: Discordでロールを外すと即座にアクセス不可
- **安全な連携**: 6桁コードによる確実な本人確認

---

## 🔧 トラブルシューティング

### よくある問題

**❌ 権限チェックが動かない**
→ BotにRead Server Members権限があるか確認

**❌ 接続時に止まる**  
→ v4.7.0で修正済み。最新版にアップデート

**❌ チャットが連携されない**
→ channel_idが正しく設定されているか確認

**❌ 自動検出されない**
→ Bot Tokenが正しいか、Botがサーバーに参加しているか確認

### サポート
問題が解決しない場合は [Issues](https://github.com/sdc-ohsawa/velocity-plugin-velocord/issues) で報告してください。

---

## 📈 更新履歴

### 🆕 v4.7.0 (最新)
- **📡 サーバー状態監視**: 自動監視とDiscord通知
- **🎨 Embed形式メッセージ**: 色分けされた美しい通知
- **🔧 重要な修正**: ロール同期問題・スタック問題を完全解決
- **🖥️ 管理コマンド**: `/serverstatus`コマンド追加

### v4.6.8  
- 自動Discord サーバー検出機能
- プレイヤースキン付きWebhook
- リアルタイム権限システム

### v4.2.0
- 権限システム簡素化
- Real UUID対応

---

## 📄 ライセンス

MIT License - 自由に使用・改変・配布可能

---

**🔗 リンク**  
📦 [ダウンロード](https://github.com/sdc-ohsawa/velocity-plugin-velocord/releases) | 🐛 [Issues](https://github.com/sdc-ohsawa/velocity-plugin-velocord/issues) | 📖 [Wiki](https://github.com/sdc-ohsawa/velocity-plugin-velocord/wiki)
