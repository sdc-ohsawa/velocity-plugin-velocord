package com.example.velocitydiscord;

import com.google.inject.Inject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.EmbedBuilder;
import java.awt.Color;
import org.slf4j.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DiscordManager extends ListenerAdapter {
    
    private final ConfigManager configManager;
    private final ChatManager chatManager;
    private final AccountLinkingManager accountLinkingManager;
    private final Logger logger;
    private JDA jda;
    
    @Inject
    public DiscordManager(ConfigManager configManager, ChatManager chatManager, 
                          AccountLinkingManager accountLinkingManager, Logger logger) {
        this.configManager = configManager;
        this.chatManager = chatManager;
        this.accountLinkingManager = accountLinkingManager;
        this.logger = logger;
    }
    
    public void initialize() {
        try {
            String token = configManager.getBotToken();
            if (token == null || token.isEmpty() || token.equals("YOUR_DISCORD_BOT_TOKEN")) {
                logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                logger.info("⚠️ Discord Bot Token が設定されていません");
                logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                logger.info("🔧 設定手順:");
                logger.info("  1. config.ymlを開く");
                logger.info("  2. discord.token の値を有効なBot Tokenに変更");
                logger.info("  3. プロキシサーバーを再起動");
                logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                logger.info("Discord連携機能は無効化されました。");
                return;
            }
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                    .addEventListeners(this, accountLinkingManager)
                    .setActivity(Activity.playing("Minecraft"))
                    .build()
                    .awaitReady();

            // 自動検出処理
            performAutoGuildDetection();

            updateGuildCommands();
            logger.info("Discord連携機能が正常に初期化されました。");

        } catch (net.dv8tion.jda.api.exceptions.InvalidTokenException e) {
            // トークンが無効な場合は設定の問題として分かりやすく表示
            logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            logger.info("❌ Discord Bot Token が無効です");
            logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            logger.info("🔧 設定確認:");
            logger.info("  1. Discord Developer Portalで正しいTokenを確認");
            logger.info("  2. config.ymlのdiscord.tokenを正しい値に更新");
            logger.info("  3. Tokenに余分なスペースや改行がないか確認");
            logger.info("  4. プロキシサーバーを再起動");
            logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            logger.info("Discord連携機能は無効化されました。");
        } catch (Exception e) {
            logger.error("Discordボットの初期化中に予期しないエラーが発生しました", e);
        }
    }
    
    /**
     * 自動Guild検出処理
     */
    private void performAutoGuildDetection() {
        List<Guild> availableGuilds = jda.getGuilds();
        Map<String, Map<String, Object>> configuredGuilds = configManager.getDiscordGuilds();
        boolean autoDetectionEnabled = configManager.isAutoGuildDetectionEnabled();
        
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        logger.info("🔍 Discord サーバー自動検出");
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        if (availableGuilds.isEmpty()) {
            logger.warn("❌ Botがどのサーバーにも参加していません。");
            return;
        }
        
        logger.info("🌐 参加中のDiscordサーバー: {} サーバー", availableGuilds.size());
        for (Guild guild : availableGuilds) {
            logger.info("  - {} (ID: {}) [メンバー: {}]", guild.getName(), guild.getId(), guild.getMemberCount());
        }
        
        if (autoDetectionEnabled) {
            if (configuredGuilds.isEmpty()) {
                // 完全自動モード：設定がない場合は自動生成
                logger.info("⚙️ 自動設定生成を実行中...");
                configManager.autoGenerateGuildSettings(availableGuilds);
                configuredGuilds = configManager.getDiscordGuilds(); // 再取得
                logger.info("✅ {} サーバーの設定を自動生成しました", configuredGuilds.size());
            } else {
                // 新しいサーバーの追加検出のみ
                logger.info("🔄 設定更新チェック実行中...");
                configManager.autoGenerateGuildSettings(availableGuilds);
                configuredGuilds = configManager.getDiscordGuilds(); // 再取得
            }
        }
        
        // 設定済みサーバーの状態確認
        if (!configuredGuilds.isEmpty()) {
            logger.info("⚙️ 設定済みDiscordサーバー: {} サーバー", configuredGuilds.size());
            for (Map.Entry<String, Map<String, Object>> entry : configuredGuilds.entrySet()) {
                String guildId = entry.getKey();
                Map<String, Object> guildConfig = entry.getValue();
                String guildName = (String) guildConfig.get("name");
                boolean enabled = (Boolean) guildConfig.getOrDefault("enabled", true);
                boolean autoDetected = (Boolean) guildConfig.getOrDefault("auto_detected", false);
                String channelId = (String) guildConfig.get("channel_id");
                
                Guild guild = jda.getGuildById(guildId);
                if (guild != null) {
                    // ロール設定の確認
                    Map<String, Object> guildRoleMapping = configManager.getGuildRoleMapping(guildId);
                    int roleCount = guildRoleMapping.size();
                    
                    String status = enabled ? "🟢 有効" : "🔴 無効";
                    String configStatus = (channelId == null || channelId.isEmpty()) ? "⚠️ 設定不完全" : "✅ 設定完了";
                    String detectionFlag = autoDetected ? "[自動検出]" : "[手動]";
                    String roleStatus = (roleCount > 0) ? String.format("🎭 ロール:%d個", roleCount) : "📝 ロール:要手動設定";
                    
                    logger.info("  - {} ({}) {} {} {} {}", guildName, guild.getName(), status, configStatus, roleStatus, detectionFlag);
                } else {
                    logger.warn("  - {} (ID: {}) [❌ 見つかりません]", guildName, guildId);
                }
            }
            
            // 設定が不完全なサーバーの警告
            List<String> incompleteGuilds = configManager.getIncompleteAutoDetectedGuilds();
            if (!incompleteGuilds.isEmpty()) {
                logger.warn("⚠️ 以下のサーバーはchannel_idの設定が必要です：");
                for (String guildId : incompleteGuilds) {
                    Guild guild = jda.getGuildById(guildId);
                    if (guild != null) {
                        logger.warn("  - {} (ID: {}) - config.ymlでchannel_idを設定してください", guild.getName(), guildId);
                    }
                }
            }
            
            // ロール設定の手動設定案内
            boolean hasEmptyRoleSettings = false;
            for (Map.Entry<String, Map<String, Object>> entry : configuredGuilds.entrySet()) {
                String guildId = entry.getKey();
                Map<String, Object> guildRoleMapping = configManager.getGuildRoleMapping(guildId);
                if (guildRoleMapping.isEmpty()) {
                    hasEmptyRoleSettings = true;
                    break;
                }
            }
            
            if (hasEmptyRoleSettings) {
                logger.info("📝 ロール権限設定について：");
                logger.info("  - ロール設定は手動で行ってください");
                logger.info("  - config.ymlのpermissions.discordRoleMappingセクションを編集");
                logger.info("  - 設定例はconfig.ymlのコメントを参照");
            }
        }
        
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        // 自動検出対応：参加している全サーバーのメッセージをチェック
        String eventChannelId = event.getChannel().getId();
        String eventGuildId = event.getGuild().getId();
        
        Map<String, Map<String, Object>> guilds = configManager.getDiscordGuilds();
        boolean autoDetectionEnabled = configManager.isAutoGuildDetectionEnabled();
        
        boolean shouldProcess = false;
        
        if (autoDetectionEnabled && guilds.isEmpty()) {
            // 完全自動モード：設定がない場合は全サーバーのメッセージを処理
            shouldProcess = true;
        } else {
            // 設定済みサーバーのチャンネルをチェック
            for (Map.Entry<String, Map<String, Object>> entry : guilds.entrySet()) {
                String guildId = entry.getKey();
                Map<String, Object> guildConfig = entry.getValue();
                
                if (guildId.equals(eventGuildId)) {
                    boolean enabled = (Boolean) guildConfig.getOrDefault("enabled", true);
                    String channelId = (String) guildConfig.get("channel_id");
                    
                    if (enabled && (channelId == null || channelId.isEmpty() || channelId.equals(eventChannelId))) {
                        shouldProcess = true;
                        break;
                    }
                }
            }
        }
        
        if (!shouldProcess) {
            return;
        }

        // サーバー内での表示名（ニックネーム）を優先取得
        String discordDisplayName;
        if (event.getMember() != null) {
            discordDisplayName = event.getMember().getEffectiveName(); // ニックネームがあればニックネーム、なければユーザー名
        } else {
            discordDisplayName = event.getAuthor().getName(); // フォールバック
        }
        
        String content = event.getMessage().getContentDisplay();

        String formattedMessage = configManager.getDiscordToGameFormat()
                .replace("%player%", discordDisplayName)
                .replace("%message%", content);

        Component messageComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(formattedMessage);
        chatManager.broadcastMessage(messageComponent);
    }
    
    /**
     * 自動検出対応：参加している全てのDiscordサーバーにメッセージを送信
     */
    public void sendMessageToDiscord(String message) {
        if (jda == null) return;
        
        Map<String, Map<String, Object>> guilds = configManager.getDiscordGuilds();
        boolean autoDetectionEnabled = configManager.isAutoGuildDetectionEnabled();
        
        if (autoDetectionEnabled && guilds.isEmpty()) {
            // 完全自動モード：設定がない場合は参加している全サーバーに送信
            List<Guild> availableGuilds = jda.getGuilds();
            for (Guild guild : availableGuilds) {
                sendMessageToGuildDefaultChannel(guild, message);
            }
        } else {
            // 設定済みサーバーに送信
            for (Map.Entry<String, Map<String, Object>> entry : guilds.entrySet()) {
                String guildId = entry.getKey();
                Map<String, Object> guildConfig = entry.getValue();
                
                // 無効化されたサーバーはスキップ
                boolean enabled = (Boolean) guildConfig.getOrDefault("enabled", true);
                if (!enabled) {
                    continue;
                }
                
                String channelId = (String) guildConfig.get("channel_id");
                Guild guild = jda.getGuildById(guildId);
                
                if (guild != null) {
                    if (channelId != null && !channelId.isEmpty()) {
                        // 指定されたチャンネルに送信
                        try {
                            TextChannel channel = jda.getTextChannelById(channelId);
                            if (channel != null) {
                                channel.sendMessage(message).queue();
                            } else {
                                logger.warn("チャンネルが見つかりません: {} (Guild: {})", channelId, guild.getName());
                            }
                        } catch (Exception e) {
                            logger.warn("Discordへのメッセージ送信に失敗しました (Guild: {}, Channel: {}): {}", 
                                guild.getName(), channelId, e.getMessage());
                        }
                    } else {
                        // チャンネルが未設定の場合はデフォルトチャンネルに送信
                        sendMessageToGuildDefaultChannel(guild, message);
                    }
                }
            }
        }
    }
    
    /**
     * サーバーのデフォルトチャンネル（最初に書き込み可能なチャンネル）にメッセージを送信
     */
    private void sendMessageToGuildDefaultChannel(Guild guild, String message) {
        try {
            List<TextChannel> channels = guild.getTextChannels();
            for (TextChannel channel : channels) {
                if (channel.canTalk()) {
                    channel.sendMessage(message).queue();
                    logger.debug("メッセージを送信しました: {} > #{}", guild.getName(), channel.getName());
                    return;
                }
            }
            logger.warn("送信可能なチャンネルが見つかりません: {}", guild.getName());
        } catch (Exception e) {
            logger.warn("デフォルトチャンネルへの送信に失敗しました (Guild: {}): {}", guild.getName(), e.getMessage());
        }
    }
    
    /**
     * プレイヤーアクション用のEmbedメッセージを送信する
     */
    public void sendPlayerActionEmbed(String playerName, PlayerActionType actionType, String fromServer, String toServer) {
        CompletableFuture.runAsync(() -> {
            try {
                EmbedBuilder embed = new EmbedBuilder();
                
                switch (actionType) {
                    case JOIN:
                        embed.setDescription(String.format("**%s** joined the server", playerName));
                        embed.setColor(Color.GREEN);
                        break;
                    case LEAVE:
                        embed.setDescription(String.format("**%s** left the server", playerName));
                        embed.setColor(Color.RED);
                        break;
                    case MOVE:
                        embed.setDescription(String.format("**%s**が**%s**から**%s**に移動しました", playerName, fromServer, toServer));
                        embed.setColor(Color.BLUE);
                        break;
                }
                
                sendEmbedToAllChannels(embed);
                logger.debug("プレイヤーアクション Embed 送信: {} -> {}", playerName, actionType);
                
            } catch (Exception e) {
                logger.error("プレイヤーアクション Embed の送信に失敗しました: {} {}", playerName, actionType, e);
            }
        });
    }
    
    /**
     * サーバー状態変化用のEmbedメッセージを送信する
     */
    public void sendServerStatusMessage(String message, String serverName, boolean isOnline) {
        CompletableFuture.runAsync(() -> {
            try {
                EmbedBuilder embed = new EmbedBuilder();
                String displayName = configManager.getServerDisplayName(serverName);
                
                if (isOnline) {
                    embed.setDescription(String.format("**%s** server is online", displayName));
                    embed.setColor(Color.GREEN);
                } else {
                    embed.setDescription(String.format("**%s** server is offline", displayName));
                    embed.setColor(Color.RED);
                }
                
                // サーバー状態監視専用のチャンネルがあるかチェック
                String statusChannelId = configManager.getServerStatusDiscordChannel();
                
                if (statusChannelId != null && !statusChannelId.isEmpty()) {
                    TextChannel statusChannel = jda.getTextChannelById(statusChannelId);
                    if (statusChannel != null) {
                        statusChannel.sendMessageEmbeds(embed.build()).queue();
                        logger.debug("サーバー状態 Embed を専用チャンネルに送信: {} -> {}", serverName, isOnline ? "ONLINE" : "OFFLINE");
                        return;
                    } else {
                        logger.warn("サーバー状態監視用チャンネルが見つかりません: {}", statusChannelId);
                    }
                }
                
                // 専用チャンネルがない場合は通常のチャンネルに送信
                sendEmbedToAllChannels(embed);
                logger.debug("サーバー状態 Embed を通常チャンネルに送信: {} -> {}", serverName, isOnline ? "ONLINE" : "OFFLINE");
                
            } catch (Exception e) {
                logger.error("サーバー状態 Embed の送信に失敗しました {}: {}", serverName, message, e);
            }
        });
    }
    
    /**
     * 全ての設定済みチャンネルにEmbedメッセージを送信する
     */
    private void sendEmbedToAllChannels(EmbedBuilder embed) {
        if (jda == null) return;
        
        Map<String, Map<String, Object>> guilds = configManager.getDiscordGuilds();
        boolean autoDetectionEnabled = configManager.isAutoGuildDetectionEnabled();
        
        if (autoDetectionEnabled && guilds.isEmpty()) {
            // 完全自動モード：設定がない場合は参加している全サーバーに送信
            List<Guild> availableGuilds = jda.getGuilds();
            for (Guild guild : availableGuilds) {
                sendEmbedToGuildDefaultChannel(guild, embed);
            }
        } else {
            // 設定済みサーバーに送信
            for (Map.Entry<String, Map<String, Object>> entry : guilds.entrySet()) {
                String guildId = entry.getKey();
                Map<String, Object> guildConfig = entry.getValue();
                
                // 無効化されたサーバーはスキップ
                boolean enabled = (Boolean) guildConfig.getOrDefault("enabled", true);
                if (!enabled) {
                    continue;
                }
                
                String channelId = (String) guildConfig.get("channel_id");
                Guild guild = jda.getGuildById(guildId);
                
                if (guild != null) {
                    if (channelId != null && !channelId.isEmpty()) {
                        // 指定されたチャンネルに送信
                        try {
                            TextChannel channel = jda.getTextChannelById(channelId);
                            if (channel != null) {
                                channel.sendMessageEmbeds(embed.build()).queue();
                            } else {
                                logger.warn("チャンネルが見つかりません: {} (Guild: {})", channelId, guild.getName());
                            }
                        } catch (Exception e) {
                            logger.warn("Discord Embed の送信に失敗しました (Guild: {}, Channel: {}): {}", 
                                guild.getName(), channelId, e.getMessage());
                        }
                    } else {
                        // チャンネルが未設定の場合はデフォルトチャンネルに送信
                        sendEmbedToGuildDefaultChannel(guild, embed);
                    }
                }
            }
        }
    }
    
    /**
     * サーバーのデフォルトチャンネルにEmbedメッセージを送信
     */
    private void sendEmbedToGuildDefaultChannel(Guild guild, EmbedBuilder embed) {
        try {
            List<TextChannel> channels = guild.getTextChannels();
            for (TextChannel channel : channels) {
                if (channel.canTalk()) {
                    channel.sendMessageEmbeds(embed.build()).queue();
                    logger.debug("Embed を送信しました: {} > #{}", guild.getName(), channel.getName());
                    return;
                }
            }
            logger.warn("送信可能なチャンネルが見つかりません: {}", guild.getName());
        } catch (Exception e) {
            logger.warn("デフォルトチャンネルへの Embed 送信に失敗しました (Guild: {}): {}", guild.getName(), e.getMessage());
        }
    }
    
    /**
     * プレイヤーアクションの種類を定義する列挙型
     */
    public enum PlayerActionType {
        JOIN, LEAVE, MOVE
    }

    /**
     * 自動検出対応：全てのサーバーまたは設定済みサーバーにスラッシュコマンドを登録
     */
    public void updateGuildCommands() {
        if (jda == null) return;
        
        Map<String, Map<String, Object>> guilds = configManager.getDiscordGuilds();
        boolean autoDetectionEnabled = configManager.isAutoGuildDetectionEnabled();
        
        List<Guild> targetGuilds = new java.util.ArrayList<>();
        
        if (autoDetectionEnabled && guilds.isEmpty()) {
            // 完全自動モード：参加している全サーバーに登録
            targetGuilds = jda.getGuilds();
        } else {
            // 設定済みサーバーに登録
            for (String guildId : guilds.keySet()) {
                Guild guild = jda.getGuildById(guildId);
                if (guild != null) {
                    targetGuilds.add(guild);
                }
            }
        }
        
        for (Guild guild : targetGuilds) {
            try {
                guild.updateCommands().addCommands(
                    Commands.slash("link", "MinecraftアカウントとDiscordアカウントを連携します。")
                            .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "code", "ゲーム内に表示された連携コード", true)
                ).queue(
                    success -> logger.info("スラッシュコマンドを登録しました: {} ({})", guild.getName(), guild.getId()),
                    error -> logger.warn("スラッシュコマンドの登録に失敗しました: {} ({}): {}", guild.getName(), guild.getId(), error.getMessage())
                );
            } catch (Exception e) {
                logger.warn("スラッシュコマンドの登録でエラーが発生しました: {} ({}): {}", guild.getName(), guild.getId(), e.getMessage());
            }
        }
        
        if (targetGuilds.isEmpty()) {
            logger.warn("スラッシュコマンドを登録するサーバーがありません。");
        }
    }

    public JDA getJda() {
        return jda;
    }
    
    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
        }
    }
} 