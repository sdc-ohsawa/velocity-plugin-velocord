# 🎨 Velocord Embed機能改善提案

## 📋 現在の実装状況

### ✅ 実装済み機能
- 基本的なEmbed送信（色付きサイドバー）
- プレイヤーアクション通知（参加・退出・移動）
- サーバー状態監視通知
- 設定可能なメッセージテンプレート

### 🚧 改善可能な領域
- より詳細な情報表示
- インタラクティブな要素
- ビジュアル要素の強化
- パフォーマンス情報の表示

---

## 🆕 提案する改善機能

### 1. **プレイヤー情報Embed強化**

```java
public class EnhancedPlayerEmbeds {
    
    public MessageEmbed createPlayerJoinEmbed(String playerName, String serverName, int onlineCount) {
        EmbedBuilder embed = new EmbedBuilder();
        
        embed.setColor(Color.GREEN);
        embed.setThumbnail("https://mc-heads.net/avatar/" + playerName);
        
        embed.setAuthor(playerName + " が参加しました", null, 
                       "https://mc-heads.net/head/" + playerName);
        
        embed.addField("🌐 サーバー", serverName, true);
        embed.addField("👥 オンライン", onlineCount + " 人", true);
        embed.addField("⏰ 時刻", getCurrentTime(), true);
        
        embed.setTimestamp(Instant.now());
        embed.setFooter("Velocord", "https://example.com/icon.png");
        
        return embed.build();
    }
    
    public MessageEmbed createServerMoveEmbed(String playerName, String fromServer, String toServer) {
        EmbedBuilder embed = new EmbedBuilder();
        
        embed.setColor(Color.BLUE);
        embed.setThumbnail("https://mc-heads.net/avatar/" + playerName);
        
        embed.setDescription(String.format("🔄 **%s** がサーバーを移動しました", playerName));
        
        embed.addField("📤 移動元", fromServer, true);
        embed.addField("📥 移動先", toServer, true);
        embed.addField("⏱️ 時刻", getCurrentTime(), true);
        
        embed.setTimestamp(Instant.now());
        
        return embed.build();
    }
}
```

### 2. **サーバー状態の詳細表示**

```java
public class DetailedServerStatusEmbed {
    
    public MessageEmbed createServerStatusEmbed(String serverName, boolean isOnline, 
                                               int playerCount, long pingMs) {
        EmbedBuilder embed = new EmbedBuilder();
        
        if (isOnline) {
            embed.setColor(Color.GREEN);
            embed.setTitle("🟢 " + serverName + " - オンライン");
            
            embed.addField("👥 プレイヤー数", playerCount + " 人", true);
            embed.addField("📡 応答時間", pingMs + " ms", true);
            embed.addField("📊 状態", "正常", true);
            
        } else {
            embed.setColor(Color.RED);
            embed.setTitle("🔴 " + serverName + " - オフライン");
            
            embed.addField("❌ 状態", "接続不可", true);
            embed.addField("⏰ 検出時刻", getCurrentTime(), true);
            embed.addField("🔄 次回チェック", "30秒後", true);
        }
        
        embed.setTimestamp(Instant.now());
        embed.setFooter("サーバー監視システム");
        
        return embed.build();
    }
}
```

### 3. **統計情報Embed**

```java
public class StatisticsEmbed {
    
    public MessageEmbed createDailyStatsEmbed(Map<String, Integer> serverStats, 
                                             int totalPlayers, int peakPlayers) {
        EmbedBuilder embed = new EmbedBuilder();
        
        embed.setTitle("📊 今日の統計情報");
        embed.setColor(new Color(0x7289DA));
        embed.setDescription("過去24時間のサーバー統計データです");
        
        // サーバー別統計
        StringBuilder serverInfo = new StringBuilder();
        for (Map.Entry<String, Integer> entry : serverStats.entrySet()) {
            serverInfo.append("**").append(entry.getKey()).append("**: ")
                     .append(entry.getValue()).append(" 人\n");
        }
        embed.addField("🌐 サーバー別参加者", serverInfo.toString(), false);
        
        embed.addField("👥 総参加者", totalPlayers + " 人", true);
        embed.addField("📈 ピーク時", peakPlayers + " 人", true);
        embed.addField("📅 期間", "過去24時間", true);
        
        embed.setTimestamp(Instant.now());
        embed.setFooter("データは自動更新されます");
        
        return embed.build();
    }
}
```

### 4. **権限エラー用の詳細Embed**

```java
public class PermissionErrorEmbed {
    
    public MessageEmbed createAccessDeniedEmbed(String playerName, String serverName, 
                                               String requiredRole) {
        EmbedBuilder embed = new EmbedBuilder();
        
        embed.setColor(Color.RED);
        embed.setTitle("🚫 アクセス拒否");
        embed.setThumbnail("https://mc-heads.net/avatar/" + playerName);
        
        embed.setDescription(String.format("**%s** さんは **%s** サーバーへの" +
                           "アクセス権限がありません。", playerName, serverName));
        
        embed.addField("👤 プレイヤー", playerName, true);
        embed.addField("🌐 サーバー", serverName, true);
        embed.addField("🎭 必要な権限", requiredRole, true);
        
        embed.addField("💡 解決方法", 
                      "• Discord管理者に権限を依頼\n" +
                      "• 適切なロールの付与を待つ\n" +
                      "• アクセス可能なサーバーを利用", false);
        
        embed.setTimestamp(Instant.now());
        embed.setFooter("権限システム | 詳細はサーバー管理者まで");
        
        return embed.build();
    }
}
```

---

## 🛠️ 実装ロードマップ

### Phase 1: 基本強化（1週間）
- [x] カラー統一システム
- [x] テンプレートシステム
- [ ] プレイヤーアバター表示
- [ ] タイムスタンプの追加

### Phase 2: 情報拡張（2週間）
- [ ] サーバー統計の詳細表示
- [ ] プレイヤー数とping情報
- [ ] 権限エラーの詳細化
- [ ] 設定可能なフッター

### Phase 3: インタラクティブ機能（3週間）
- [ ] ボタン付きEmbed
- [ ] ページネーション
- [ ] リアクション投票機能
- [ ] 動的更新システム

---

## 📈 期待される効果

### ユーザー体験の向上
- **視覚的な情報整理**: より見やすく理解しやすい通知
- **詳細な状況把握**: サーバー状態やエラーの詳細表示
- **プロフェッショナルな外観**: 統一されたデザインシステム

### 管理の効率化
- **問題の早期発見**: 詳細なサーバー監視情報
- **ユーザーサポート**: 明確なエラーメッセージと解決策
- **統計データ**: 使用状況の可視化

### 技術的メリット
- **保守性**: テンプレートシステムによる一元管理
- **拡張性**: 新機能追加の容易さ
- **パフォーマンス**: 効率的な通知システム

---

## 🔧 設定例

### config.yml拡張案

```yaml
embed:
  # Embed機能の全体設定
  enabled: true
  
  # カラーテーマ
  colors:
    success: 0x00FF00
    error: 0xFF0000
    warning: 0xFFAA00
    info: 0x0099FF
    
  # フッター設定
  footer:
    text: "Velocord v4.7.0"
    icon_url: "https://example.com/icon.png"
    
  # プレイヤーアバター表示
  player_avatars:
    enabled: true
    service: "mc-heads.net"  # crafthead.net, mc-heads.net
    
  # 統計情報
  statistics:
    enabled: true
    update_interval: 3600  # 1時間ごと
    
  # 詳細表示設定
  details:
    show_ping: true
    show_player_count: true
    show_timestamp: true
```

---

この改善により、Velocordのユーザー体験が大幅に向上し、より情報豊富で視覚的に魅力的な通知システムが実現できます。 