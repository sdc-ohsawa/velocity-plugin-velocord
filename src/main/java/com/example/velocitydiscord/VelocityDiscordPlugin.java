package com.example.velocitydiscord;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

@Plugin(
    id = "velocord",
    name = "Velocord",
    version = "4.7.0",
    description = "MinecraftとDiscordを繋ぐ統合チャット・連携プラグイン",
    authors = {"comugi"}
)
public class VelocityDiscordPlugin {
    
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    
    private DiscordManager discordManager;
    private ChatManager chatManager;
    private PermissionManager permissionManager;
    private DatabaseManager databaseManager;
    private AccountLinkingManager accountLinkingManager;
    private ScheduledExecutorService scheduler;
    private ConfigManager configManager;
    private ServerStatusMonitor serverStatusMonitor;
    
    @Inject
    public VelocityDiscordPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }
    
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Velocity Discord Plugin を初期化中...");
        
        try {
            // スケジューラーの初期化
            scheduler = Executors.newScheduledThreadPool(2);
            
            // 設定ファイルの初期化
            configManager = new ConfigManager(dataDirectory);
            configManager.loadConfig();
            
            // データベースの初期化
            databaseManager = new DatabaseManager(configManager, logger);
            databaseManager.initialize();
            
            // 各マネージャーの初期化（依存関係を考慮した順序）
            permissionManager = new PermissionManager(configManager, null, databaseManager, logger);
            
            // WebhookManagerの初期化
            WebhookManager webhookManager = new WebhookManager(configManager.getWebhookUrl(), logger);
            
            chatManager = new ChatManager(server, logger, configManager, permissionManager, webhookManager);
            accountLinkingManager = new AccountLinkingManager(configManager, databaseManager, permissionManager, logger, scheduler);
            
            // DiscordManagerの初期化（accountLinkingManagerが作成された後）
            discordManager = new DiscordManager(configManager, chatManager, accountLinkingManager, logger);
            discordManager.initialize();
            
            // PermissionManagerにDiscordManagerを設定
            permissionManager.setDiscordManager(discordManager);
            
            // コマンドの登録
            CommandManager commandManager = server.getCommandManager();
            commandManager.register(commandManager.metaBuilder("link").build(), accountLinkingManager);
            commandManager.register(commandManager.metaBuilder("vddebug").build(), new DebugCommand(permissionManager, databaseManager));
            commandManager.register(commandManager.metaBuilder("vdreload").build(), new ReloadCommand(configManager, logger));
            commandManager.register(commandManager.metaBuilder("serverstatus").build(), new ServerStatusCommand(this, logger));
            
            // イベントリスナーの登録
            server.getEventManager().register(this, chatManager);
            server.getEventManager().register(this, accountLinkingManager);
            server.getEventManager().register(this, discordManager);
            
            // サーバー状態監視機能の初期化
            if (configManager.isServerStatusEnabled()) {
                serverStatusMonitor = new ServerStatusMonitor(server, logger, configManager, discordManager, scheduler);
                logger.info("サーバー状態監視機能を有効化しました");
            } else {
                logger.info("サーバー状態監視機能は無効化されています");
            }
            
            logger.info("Velocity Discord Plugin の初期化が完了しました！");
            
        } catch (Exception e) {
            logger.error("プラグインの初期化中にエラーが発生しました", e);
        }
    }
    
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("Velocity Discord Plugin をシャットダウン中...");
        
        if (serverStatusMonitor != null) {
            serverStatusMonitor.stopMonitoring();
        }
        
        if (discordManager != null) {
            discordManager.shutdown();
        }
        
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        
        if (scheduler != null) {
            scheduler.shutdown();
        }
        
        logger.info("Velocity Discord Plugin のシャットダウンが完了しました。");
    }
    
    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        // PostLoginEventでは何もしない（権限チェックはServerPreConnectEventで実行）
    }
    
    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        if (discordManager != null && event.getPlayer().getCurrentServer().isPresent()) {
            String playerName = event.getPlayer().getUsername();
            String serverName = event.getPlayer().getCurrentServer().get().getServerInfo().getName();
            String displayName = permissionManager.getServerDisplayName(serverName);
            
            // 初回接続か移動かを判定
            if (event.getPreviousServer() != null) {
                // サーバー間移動
                String previousServerName = event.getPreviousServer().getServerInfo().getName();
                String previousDisplayName = permissionManager.getServerDisplayName(previousServerName);
                discordManager.sendPlayerActionEmbed(playerName, DiscordManager.PlayerActionType.MOVE, 
                    previousDisplayName, displayName);
            } else {
                // 初回接続
                discordManager.sendPlayerActionEmbed(playerName, DiscordManager.PlayerActionType.JOIN, 
                    null, null);
            }
        }
    }
    
    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        String playerName = event.getPlayer().getUsername();
        
        // 接続先サーバー情報を取得
        event.getResult().getServer().ifPresent(targetServer -> {
            String targetServerName = targetServer.getServerInfo().getName();
            
            logger.info("Permission check: Player {} (UUID: {}) attempting to connect to server {}", 
                playerName, playerUuid, targetServerName);
            
            try {
                // アカウント連携チェック
                if (configManager.isAccountLinkingEnabled() && configManager.isForceLinkEnabled()) {
                    boolean isLinked = databaseManager.isAccountLinked(playerUuid).get();
                    if (!isLinked) {
                        // 未連携の場合は接続を拒否し、連携コードを生成
                        String code = accountLinkingManager.generateVerificationCode(playerUuid);
                        String message = configManager.getAccountLinkingConnectionDeniedMessage().replace("%code%", code);
                        logger.info("Player {} is not linked. Generated verification code: {}", playerName, code);
                        
                        // 接続を拒否し、プレイヤーを切断
                        event.setResult(ServerPreConnectEvent.ServerResult.denied());
                        event.getPlayer().disconnect(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
                        return;
                    }
                }
                
                // リアルタイム権限チェック（常に実行）
                if (!permissionManager.canAccessServer(playerUuid, targetServerName)) {
                    // アクセス拒否
                    event.setResult(ServerPreConnectEvent.ServerResult.denied());
                    logger.info("Player {} denied access to server {} - disconnecting", playerName, targetServerName);
                    
                    // 詳細なメッセージを作成
                    String discordId = databaseManager.getDiscordUserId(playerUuid).get();
                    Component disconnectMessage;
                    
                    if (discordId == null) {
                        // Discord未連携の場合
                        disconnectMessage = Component.text()
                            .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED))
                            .append(Component.text("\n"))
                            .append(Component.text("⚠ Discord連携が必要です ⚠", NamedTextColor.RED))
                            .append(Component.text("\n\n"))
                            .append(Component.text("このサーバーにアクセスするには", NamedTextColor.YELLOW))
                            .append(Component.text("\n"))
                            .append(Component.text("Discordアカウントとの連携が必要です。", NamedTextColor.YELLOW))
                            .append(Component.text("\n\n"))
                            .append(Component.text("連携方法: ", NamedTextColor.GRAY))
                            .append(Component.text("/link", NamedTextColor.AQUA))
                            .append(Component.text(" コマンドを実行してください。", NamedTextColor.GRAY))
                            .append(Component.text("\n"))
                            .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED))
                            .build();
                    } else {
                        // Discord連携済みだが権限不足の場合
                        String displayName = permissionManager.getServerDisplayName(targetServerName);
                        disconnectMessage = Component.text()
                            .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED))
                            .append(Component.text("\n"))
                            .append(Component.text("⚠ アクセス権限不足 ⚠", NamedTextColor.RED))
                            .append(Component.text("\n\n"))
                            .append(Component.text(displayName + " サーバーへの", NamedTextColor.YELLOW))
                            .append(Component.text("\n"))
                            .append(Component.text("アクセス権限がありません。", NamedTextColor.YELLOW))
                            .append(Component.text("\n\n"))
                            .append(Component.text("必要な権限について管理者に", NamedTextColor.GRAY))
                            .append(Component.text("\n"))
                            .append(Component.text("お問い合わせください。", NamedTextColor.GRAY))
                            .append(Component.text("\n"))
                            .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED))
                            .build();
                    }
                    
                    // プレイヤーを明示的に切断
                    event.getPlayer().disconnect(disconnectMessage);
                    return;
                }
                
                // アクセス許可
                logger.info("Player {} granted access to server {}", playerName, targetServerName);
                
            } catch (Exception e) {
                logger.error("Error during permission check for player {} to server {}", playerName, targetServerName, e);
                
                // エラー時は安全のため接続を拒否
                event.setResult(ServerPreConnectEvent.ServerResult.denied());
                Component errorMessage = Component.text()
                    .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED))
                    .append(Component.text("\n"))
                    .append(Component.text("⚠ サーバーエラー ⚠", NamedTextColor.RED))
                    .append(Component.text("\n\n"))
                    .append(Component.text("権限確認中にサーバーエラーが", NamedTextColor.YELLOW))
                    .append(Component.text("\n"))
                    .append(Component.text("発生しました。", NamedTextColor.YELLOW))
                    .append(Component.text("\n\n"))
                    .append(Component.text("しばらく時間をおいて", NamedTextColor.GRAY))
                    .append(Component.text("\n"))
                    .append(Component.text("再度お試しください。", NamedTextColor.GRAY))
                    .append(Component.text("\n"))
                    .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED))
                    .build();
                event.getPlayer().disconnect(errorMessage);
            }
        });
    }
    
    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (discordManager != null) {
            String playerName = event.getPlayer().getUsername();
            discordManager.sendPlayerActionEmbed(playerName, DiscordManager.PlayerActionType.LEAVE, 
                null, null);
        }
    }
    
    public ProxyServer getServer() {
        return server;
    }
    
    public Logger getLogger() {
        return logger;
    }
    
    public Path getDataDirectory() {
        return dataDirectory;
    }
    
    public DiscordManager getDiscordManager() {
        return discordManager;
    }
    
    public ChatManager getChatManager() {
        return chatManager;
    }
    
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public AccountLinkingManager getAccountLinkingManager() {
        return accountLinkingManager;
    }
    
    public ServerStatusMonitor getServerStatusMonitor() {
        return serverStatusMonitor;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
} 