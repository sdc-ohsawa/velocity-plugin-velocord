package com.example.velocitydiscord;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private final Path dataDirectory;
    private Map<String, Object> config;

    public ConfigManager(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public void loadConfig() {
        File configFile = new File(dataDirectory.toFile(), "config.yml");
        System.out.println("[ConfigManager] 📂 設定ファイルパス: " + configFile.getAbsolutePath());
        
        if (!configFile.exists()) {
            System.out.println("[ConfigManager] ⚠️ config.ymlが存在しません。デフォルト設定を作成します。");
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                    System.out.println("[ConfigManager] ✅ リソースからconfig.ymlをコピーしました。");
                } else {
                    createDefaultConfig(configFile);
                    System.out.println("[ConfigManager] ✅ デフォルトconfig.ymlを作成しました。");
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to copy default config.yml", e);
            }
        } else {
            System.out.println("[ConfigManager] ✅ config.ymlが存在します。");
        }

        try (FileInputStream fis = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            config = yaml.load(fis);
            if (config == null) {
                config = new HashMap<>();
                System.out.println("[ConfigManager] ⚠️ config.ymlが空でした。空のマップを作成します。");
            } else {
                System.out.println("[ConfigManager] ✅ config.ymlを正常に読み込みました。");
                System.out.println("[ConfigManager] 📋 設定項目数: " + config.size());
                // 主要セクションの存在確認
                if (config.containsKey("discord")) {
                    System.out.println("[ConfigManager] ✅ discordセクション: 存在");
                } else {
                    System.out.println("[ConfigManager] ❌ discordセクション: 存在しません");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.yml", e);
        }
    }

    private void createDefaultConfig(File configFile) throws IOException {
        configFile.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("# Velocity Discord Plugin Configuration\n\n");
            
            // Discord設定（新形式）
            writer.write("discord:\n");
            writer.write("  # Discord Bot Token\n");
            writer.write("  token: \"YOUR_DISCORD_BOT_TOKEN\"\n");
            writer.write("  \n");
            writer.write("  # Guild ID自動検出機能（trueの場合、参加しているサーバーを自動検出）\n");
            writer.write("  auto_guild_detection: true\n");
            writer.write("  \n");
            writer.write("  # 複数のDiscordサーバー設定\n");
            writer.write("  # auto_guild_detectionがtrueの場合、参加しているサーバーが自動追加されます\n");
            writer.write("  guilds: {}\n");
            writer.write("  # 手動設定例:\n");
            writer.write("  # \"1234567890123456789\":\n");
            writer.write("  #   name: \"メインサーバー\"\n");
            writer.write("  #   channel_id: \"YOUR_CHANNEL_ID\"\n");
            writer.write("  #   webhookUrl: \"YOUR_WEBHOOK_URL\"\n");
            writer.write("  #   enabled: true\n\n");
            
            // チャット設定
            writer.write("chat:\n");
            writer.write("  format: \"&7[&a%server%&7] &f%player%&7: &f%message%\"\n");
            writer.write("  discord_format: \"**[%server%]** %player%: %message%\"\n");
            writer.write("  discord_to_game_enabled: true\n");
            writer.write("  discord_to_game_format: \"&9[Discord] &b%player%&7: &f%message%\"\n");
            writer.write("  game_to_discord_enabled: true\n\n");
            
            // プレイヤーアクションメッセージ設定（Embed形式で送信）
            writer.write("messages:\n");
            writer.write("  # Embed形式でメッセージを送信（緑・赤・青のサイドバー表示）\n");
            writer.write("  use_embed_format: true\n");
            writer.write("  # プレイヤー参加メッセージ（緑色のEmbed）\n");
            writer.write("  player_join: \"🟢 **%player%** joined the server\"\n");
            writer.write("  # プレイヤー退出メッセージ（赤色のEmbed）\n");
            writer.write("  player_leave: \"🔴 **%player%** left the server\"\n");
            writer.write("  # サーバー移動メッセージ（青色のEmbed）\n");
            writer.write("  player_move: \"🔵 **%player%**が**%from%**から**%to%**に移動しました\"\n");
            writer.write("  # サーバーオンラインメッセージ（緑色のEmbed）\n");
            writer.write("  server_online: \"🟢 **%server%** server is online\"\n");
            writer.write("  # サーバーオフラインメッセージ（赤色のEmbed）\n");
            writer.write("  server_offline: \"🔴 **%server%** server is offline\"\n\n");
            
            // 権限設定（手動設定用テンプレート）
            writer.write("permissions:\n");
            writer.write("  # ロール権限設定（手動設定が必要）\n");
            writer.write("  # auto_guild_detectionによりサーバーが自動検出されますが、\n");
            writer.write("  # ロール設定は手動で行ってください\n");
            writer.write("  discordRoleMapping: {}\n");
            writer.write("  # 手動設定例:\n");
            writer.write("  # \"1234567890123456789\": # Guild ID\n");
            writer.write("  #   roles:\n");
            writer.write("  #     \"1111111111111111111\": # Role ID\n");
            writer.write("  #       name: \"管理者\"\n");
            writer.write("  #       serverAccess:\n");
            writer.write("  #         seikatsu: true\n");
            writer.write("  #         sigen: true\n");
            writer.write("  #         lobby: true\n");
            writer.write("  #         vip: true\n");
            writer.write("  #     \"2222222222222222222\": # Role ID\n");
            writer.write("  #       name: \"一般メンバー\"\n");
            writer.write("  #       serverAccess:\n");
            writer.write("  #         seikatsu: true\n");
            writer.write("  #         sigen: true\n");
            writer.write("  #         lobby: false\n");
            writer.write("  #         vip: false\n\n");
            
            // サーバー表示名設定
            writer.write("servers:\n");
            writer.write("  seikatsu: \"生存サーバー\"\n");
            writer.write("  sigen: \"資源サーバー\"\n");
            writer.write("  lobby: \"ロビーサーバー\"\n");
            writer.write("  vip: \"VIPサーバー\"\n\n");
            
            // データベース設定
            writer.write("database:\n");
            writer.write("  type: \"sqlite\"\n");
            writer.write("  mysql:\n");
            writer.write("    host: \"localhost\"\n");
            writer.write("    port: 3306\n");
            writer.write("    database: \"velocity_discord\"\n");
            writer.write("    username: \"root\"\n");
            writer.write("    password: \"password\"\n");
            writer.write("    connection_pool_size: 10\n");
            writer.write("  sqlite:\n");
            writer.write("    file: \"plugins/velocord/accounts.db\"\n\n");
            
            // アカウント連携設定
            writer.write("account_linking:\n");
            writer.write("  enabled: true\n");
            writer.write("  required: true\n");
            writer.write("  command_prefix: \"!link\"\n");
            writer.write("  verification_timeout: 10\n");
            writer.write("  success_message: \"&aアカウントの連携が完了しました！\"\n");
            writer.write("  failure_message: \"&cアカウントの連携に失敗しました。\"\n");
            writer.write("  already_linked_message: \"&eこのアカウントは既に連携されています。\"\n");
            writer.write("  unlink_message: \"&aアカウントの連携を解除しました。\"\n");
            writer.write("  connection_denied_message: |\n");
            writer.write("    &cこのサーバーを利用するにはDiscord連携が必要です\n");
            writer.write("    &eあなたの認証コード: &b%code%\n");
            writer.write("    &7Discordで /link %code% を実行してください\n");
            writer.write("    &7連携完了後、再度接続してください\n\n");
            
            // サーバー状態監視設定
            writer.write("server-status:\n");
            writer.write("  # サーバー状態監視機能の有効/無効\n");
            writer.write("  enabled: true\n");
            writer.write("  # サーバー状態をチェックする間隔（秒）\n");
            writer.write("  scan-delay: 30\n");
            writer.write("  # サーバーへの接続タイムアウト時間（ミリ秒）\n");
            writer.write("  ping-timeout: 3000\n");
            writer.write("  # サーバー状態通知専用のDiscordチャンネルID（空の場合は通常のチャンネルを使用）\n");
            writer.write("  discord-channel: \"\"\n\n");
            
            // Embed設定
            writer.write("embed:\n");
            writer.write("  # Embedのフッター設定\n");
            writer.write("  footer:\n");
            writer.write("    text: \"Velocord v4.7.1\"\n");
            writer.write("    icon_url: \"\"\n");
        }
    }

    public void saveConfig() {
        File configFile = new File(dataDirectory.toFile(), "config.yml");
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);
        try (FileWriter writer = new FileWriter(configFile)) {
            yaml.dump(config, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config.yml", e);
        }
    }

    /**
     * コメント保持機能付きでguilds設定のみを更新する
     */
    private void saveConfigWithComments() {
        File configFile = new File(dataDirectory.toFile(), "config.yml");
        try {
            // 現在のファイル内容を読み取り
            String currentContent = Files.readString(configFile.toPath());
            
            // guildsセクションの開始位置を見つける
            int guildsStart = currentContent.indexOf("  guilds:");
            if (guildsStart == -1) {
                // guildsセクションが見つからない場合は通常の保存を実行
                saveConfig();
                return;
            }
            
            // guilds行の終わり（改行）を見つける
            int guildsLineEnd = currentContent.indexOf("\n", guildsStart);
            if (guildsLineEnd == -1) guildsLineEnd = currentContent.length();
            
            // 次のセクション（permissions:）の開始位置を見つける
            int nextSectionStart = currentContent.indexOf("\npermissions:", guildsLineEnd);
            if (nextSectionStart == -1) {
                nextSectionStart = currentContent.length();
            }
            
            // guilds設定の新しい内容を生成
            StringBuilder guildsContent = new StringBuilder();
            guildsContent.append("  guilds:");
            
            Map<String, Map<String, Object>> guilds = getDiscordGuilds();
            if (guilds.isEmpty()) {
                guildsContent.append(" {}\n");
                guildsContent.append("  # 手動設定例:\n");
                guildsContent.append("  # \"1234567890123456789\":\n");
                guildsContent.append("  #   name: \"メインサーバー\"\n");
                guildsContent.append("  #   channel_id: \"YOUR_CHANNEL_ID\"\n");
                guildsContent.append("  #   webhookUrl: \"YOUR_WEBHOOK_URL\"\n");
                guildsContent.append("  #   enabled: true\n");
            } else {
                guildsContent.append("\n");
                for (Map.Entry<String, Map<String, Object>> entry : guilds.entrySet()) {
                    String guildId = entry.getKey();
                    Map<String, Object> guildConfig = entry.getValue();
                    
                    guildsContent.append("    \"").append(guildId).append("\":\n");
                    guildsContent.append("      name: \"").append(guildConfig.get("name")).append("\"\n");
                    guildsContent.append("      channel_id: \"").append(guildConfig.getOrDefault("channel_id", "")).append("\"\n");
                    guildsContent.append("      webhookUrl: \"").append(guildConfig.getOrDefault("webhookUrl", "")).append("\"\n");
                    guildsContent.append("      enabled: ").append(guildConfig.getOrDefault("enabled", true)).append("\n");
                    if ((boolean) guildConfig.getOrDefault("auto_detected", false)) {
                        guildsContent.append("      auto_detected: true\n");
                    }
                }
                
                // コメント例を最後に追加
                guildsContent.append("  # 手動設定例:\n");
                guildsContent.append("  # \"1234567890123456789\":\n");
                guildsContent.append("  #   name: \"メインサーバー\"\n");
                guildsContent.append("  #   channel_id: \"YOUR_CHANNEL_ID\"\n");
                guildsContent.append("  #   webhookUrl: \"YOUR_WEBHOOK_URL\"\n");
                guildsContent.append("  #   enabled: true\n");
            }
            
            // ファイル内容を再構築
            String beforeGuilds = currentContent.substring(0, guildsStart);
            String afterGuilds = currentContent.substring(nextSectionStart);
            String newContent = beforeGuilds + guildsContent.toString() + afterGuilds;
            
            // ファイルに書き込み
            Files.writeString(configFile.toPath(), newContent);
            
        } catch (IOException e) {
            System.err.println("コメント保持機能付き保存に失敗しました。通常の保存を実行します: " + e.getMessage());
            saveConfig(); // フォールバック
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T get(String path, T def) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = config;
        for (int i = 0; i < parts.length - 1; i++) {
            Object node = current.get(parts[i]);
            if (node instanceof Map) {
                current = (Map<String, Object>) node;
            } else {
                return def;
            }
        }
        return (T) current.getOrDefault(parts[parts.length - 1], def);
    }
    
    @SuppressWarnings("unchecked")
    private void set(String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = config;
        for (int i = 0; i < parts.length - 1; i++) {
            current = (Map<String, Object>) current.computeIfAbsent(parts[i], k -> new HashMap<>());
        }
        current.put(parts[parts.length - 1], value);
    }

    // Discord
    public String getBotToken() { 
        String token = get("discord.token", "YOUR_DISCORD_BOT_TOKEN");
        // デバッグ用：token設定状況をログ出力（セキュリティのため一部をマスク）
        if ("YOUR_DISCORD_BOT_TOKEN".equals(token)) {
            System.out.println("[ConfigManager] ⚠️ Discord Token: デフォルト値（未設定）");
        } else if (token.isEmpty()) {
            System.out.println("[ConfigManager] ⚠️ Discord Token: 空文字");
        } else {
            String maskedToken = token.substring(0, Math.min(10, token.length())) + "..." + 
                               token.substring(Math.max(10, token.length() - 5));
            System.out.println("[ConfigManager] ✅ Discord Token: " + maskedToken + " (設定済み)");
        }
        return token;
    }
    
    // 自動取得設定
    public boolean isAutoGuildDetectionEnabled() { 
        return get("discord.auto_guild_detection", true); 
    }
    
    public void setAutoGuildDetection(boolean enabled) {
        set("discord.auto_guild_detection", enabled);
        saveConfig();
    }
    
    // 複数Discordサーバー対応メソッド
    @SuppressWarnings("unchecked")
    public Map<String, Map<String, Object>> getDiscordGuilds() { 
        return get("discord.guilds", Collections.emptyMap()); 
    }
    
    /**
     * 参加しているDiscordサーバーを自動検出してconfig.ymlに設定を追加
     * Guild設定のみ自動生成し、ロール設定は手動設定が必要
     */
    @SuppressWarnings("unchecked")
    public void autoGenerateGuildSettings(List<net.dv8tion.jda.api.entities.Guild> availableGuilds) {
        if (availableGuilds.isEmpty()) {
            return;
        }
        
        Map<String, Map<String, Object>> existingGuilds = getDiscordGuilds();
        Map<String, Object> guildsConfig = new HashMap<>();
        
        // 既存の設定を保持
        for (Map.Entry<String, Map<String, Object>> entry : existingGuilds.entrySet()) {
            guildsConfig.put(entry.getKey(), entry.getValue());
        }
        
        // 新しいサーバーを自動追加（Guild設定のみ）
        boolean hasNewGuilds = false;
        for (net.dv8tion.jda.api.entities.Guild guild : availableGuilds) {
            String guildId = guild.getId();
            
            if (!guildsConfig.containsKey(guildId)) {
                // Guild設定のみを追加
                Map<String, Object> guildConfig = new HashMap<>();
                guildConfig.put("name", guild.getName());
                guildConfig.put("channel_id", ""); // 手動設定が必要
                guildConfig.put("webhookUrl", ""); // 手動設定が必要
                guildConfig.put("enabled", true);
                guildConfig.put("auto_detected", true); // 自動検出フラグ
                
                guildsConfig.put(guildId, guildConfig);
                hasNewGuilds = true;
            }
        }
        
        if (hasNewGuilds) {
            set("discord.guilds", guildsConfig);
            saveConfigWithComments(); // コメント保持機能付きで保存
        }
    }
    
    /**
     * 自動検出されたサーバーのうち、設定が不完全なものを取得
     */
    @SuppressWarnings("unchecked")
    public List<String> getIncompleteAutoDetectedGuilds() {
        Map<String, Map<String, Object>> guilds = getDiscordGuilds();
        List<String> incompleteGuilds = new java.util.ArrayList<>();
        
        for (Map.Entry<String, Map<String, Object>> entry : guilds.entrySet()) {
            Map<String, Object> guildConfig = entry.getValue();
            boolean autoDetected = (Boolean) guildConfig.getOrDefault("auto_detected", false);
            
            if (autoDetected) {
                String channelId = (String) guildConfig.get("channel_id");
                if (channelId == null || channelId.isEmpty()) {
                    incompleteGuilds.add(entry.getKey());
                }
            }
        }
        
        return incompleteGuilds;
    }
    
    public boolean isGuildEnabled(String guildId) {
        Map<String, Map<String, Object>> guilds = getDiscordGuilds();
        Map<String, Object> guildConfig = guilds.get(guildId);
        if (guildConfig == null) return false;
        return (Boolean) guildConfig.getOrDefault("enabled", true);
    }
    
    public String getGuildChannelId(String guildId) {
        Map<String, Map<String, Object>> guilds = getDiscordGuilds();
        Map<String, Object> guildConfig = guilds.get(guildId);
        if (guildConfig == null) return "";
        return (String) guildConfig.getOrDefault("channel_id", "");
    }
    
    public String getGuildWebhookUrl(String guildId) {
        Map<String, Map<String, Object>> guilds = getDiscordGuilds();
        Map<String, Object> guildConfig = guilds.get(guildId);
        if (guildConfig == null) return "";
        return (String) guildConfig.getOrDefault("webhookUrl", "");
    }
    
    // 後方互換性のため、最初に見つかったサーバーのチャンネルIDを返す
    public String getDiscordChannelId() { 
        Map<String, Map<String, Object>> guilds = getDiscordGuilds();
        for (Map<String, Object> guildConfig : guilds.values()) {
            String channelId = (String) guildConfig.get("channel_id");
            if (channelId != null && !channelId.isEmpty()) {
                return channelId;
            }
        }
        return get("discord.channel_id", "YOUR_CHANNEL_ID"); 
    }
    
    // 後方互換性のため、最初に見つかったサーバーのWebhook URLを返す
    public String getWebhookUrl() { 
        Map<String, Map<String, Object>> guilds = getDiscordGuilds();
        for (Map<String, Object> guildConfig : guilds.values()) {
            String webhookUrl = (String) guildConfig.get("webhookUrl");
            if (webhookUrl != null && !webhookUrl.isEmpty()) {
                return webhookUrl;
            }
        }
        return get("discord.webhook_url", ""); 
    }

    // Chat
    public boolean isGameToDiscordEnabled() { return get("chat.game_to_discord_enabled", true); }
    public boolean isDiscordToGameEnabled() { return get("chat.discord_to_game_enabled", true); }
    public String getChatFormat() { return get("chat.format", "&7[&a%server%&7] &f%player%&7: &f%message%"); }
    public String getDiscordFormat() { return get("chat.discord_format", "**[%server%]** %player%: %message%"); }
    public String getDiscordToGameFormat() { return get("chat.discord_to_game_format", "&9[Discord] &b%player%&7: &f%message%"); }

    // Linking - account_linkingセクションに対応
    public boolean isAccountLinkingEnabled() { return get("account_linking.enabled", true); }
    public boolean isForceLinkEnabled() { return get("account_linking.required", true); }
    public int getAccountLinkingVerificationTimeout() { return get("account_linking.verification_timeout", 10); }
    public String getAccountLinkingConnectionDeniedMessage() { return get("account_linking.connection_denied_message", "&cDiscord連携が必要です。あなたのコード: %code%"); }
    public String getAccountLinkingAlreadyLinkedMessage() { return get("account_linking.already_linked_message", "&eこのアカウントは既に連携済みです。"); }
    public String getAccountLinkingUnlinkMessage() { return get("account_linking.unlink_message", "&aアカウントの連携を解除しました。"); }
    
    // Database
    public String getDatabaseType() { return get("database.type", "sqlite"); }
    public Map<String, Object> getMysqlConfig() { return get("database.mysql", Collections.emptyMap()); }

    // Permissions - 複数サーバー対応の新しいロールマッピング構造
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDiscordRoleMapping() { 
        Map<String, Object> mapping = get("permissions.discordRoleMapping", Collections.emptyMap());
        System.out.println("[ConfigManager] 🔍 ロールマッピング取得:");
        System.out.println("[ConfigManager]   📊 マッピング数: " + mapping.size());
        for (Map.Entry<String, Object> entry : mapping.entrySet()) {
            System.out.println("[ConfigManager]   📂 Guild: " + entry.getKey());
        }
        return mapping;
    }
    
    // 特定のサーバーのロール設定を取得
    @SuppressWarnings("unchecked")
    public Map<String, Object> getGuildRoleMapping(String guildId) {
        Map<String, Object> roleMapping = getDiscordRoleMapping();
        Object guildData = roleMapping.get(guildId);
        if (guildData instanceof Map<?, ?> guildMap) {
            Object rolesData = guildMap.get("roles");
            if (rolesData instanceof Map<?, ?>) {
                return (Map<String, Object>) rolesData;
            }
        }
        return Collections.emptyMap();
    }
    
    // Servers
    public Map<String, String> getServers() { return get("servers", Collections.emptyMap()); }
    
    // Server Status Monitoring - サーバー状態監視設定
    public boolean isServerStatusEnabled() {
        return get("server-status.enabled", true);
    }

    public int getServerStatusScanDelay() {
        return get("server-status.scan-delay", 30);
    }

    public int getServerStatusPingTimeout() {
        return get("server-status.ping-timeout", 3000);
    }

    public String getServerStatusDiscordChannel() {
        return get("server-status.discord-channel", "");
    }
    
    /**
     * サーバーの表示名を取得する
     * servers設定から取得し、設定がない場合はサーバー名をそのまま返す
     */
    public String getServerDisplayName(String serverName) {
        Map<String, String> servers = getServers();
        return servers.getOrDefault(serverName, serverName);
    }
    
    // Message Templates - メッセージテンプレート
    public boolean useEmbedFormat() {
        return get("messages.use_embed_format", true);
    }
    
    public String getPlayerJoinMessage() {
        return get("messages.player_join", "🟢 **%player%** joined the server");
    }
    
    public String getPlayerLeaveMessage() {
        return get("messages.player_leave", "🔴 **%player%** left the server");
    }
    
    public String getPlayerMoveMessage() {
        return get("messages.player_move", "🔵 **%player%**が**%from%**から**%to%**に移動しました");
    }
    
    public String getServerOnlineMessage() {
        return get("messages.server_online", "🟢 **%server%** server is online");
    }
    
    public String getServerOfflineMessage() {
        return get("messages.server_offline", "🔴 **%server%** server is offline");
    }
    
    // Embed Footer - Embedフッター設定
    public String getEmbedFooterText() {
        return get("embed.footer.text", "Velocord v4.7.1");
    }
    
    public String getEmbedFooterIconUrl() {
        return get("embed.footer.icon_url", "");
    }
} 