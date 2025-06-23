package com.example.velocitydiscord;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatManager {
    
    private final ProxyServer server;
    private final ConfigManager configManager;
    private final PermissionManager permissionManager;
    private final WebhookManager webhookManager;
    private final Map<UUID, String> playerServers;
    private final Logger logger;
    
    public ChatManager(ProxyServer server, Logger logger, ConfigManager configManager, 
                      PermissionManager permissionManager, WebhookManager webhookManager) {
        this.server = server;
        this.logger = logger;
        this.configManager = configManager;
        this.permissionManager = permissionManager;
        this.webhookManager = webhookManager;
        this.playerServers = new HashMap<>();
    }
    
    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        Player sourcePlayer = event.getPlayer();
        String originalMessage = event.getMessage();

        // イベントはキャンセルしない（キック回避のため）

        // 送信元プレイヤーがどのサーバーにも接続していない場合は、何もしない
        if (sourcePlayer.getCurrentServer().isEmpty()) {
            return;
        }

        ServerInfo sourceServerInfo = sourcePlayer.getCurrentServer().get().getServerInfo();
        String sourceServerName = sourceServerInfo.getName();
        String sourceServerDisplayName = permissionManager.getServerDisplayName(sourceServerName);

        // 整形済みメッセージを作成
        String formattedMessageText = configManager.getChatFormat()
                .replace("%player%", sourcePlayer.getUsername())
                .replace("%server%", sourceServerDisplayName)
                .replace("%message%", originalMessage);
        Component formattedComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(formattedMessageText);

        // 他のサーバーにいるプレイヤーにのみ、整形済みメッセージを送信
        for (Player targetPlayer : server.getAllPlayers()) {
            // 自分自身には送信しない（バニラチャットが既にあるため）
            if (targetPlayer.equals(sourcePlayer)) {
                continue;
            }

            // ターゲットプレイヤーのサーバー情報を取得し、送信元と同じサーバーにいる場合はスキップ
            if (targetPlayer.getCurrentServer().isPresent()) {
                if (targetPlayer.getCurrentServer().get().getServerInfo().equals(sourceServerInfo)) {
                    continue; // 同じサーバーなのでスキップ
                }
            }
            
            // 異なるサーバーにいるか、プロキシにいるプレイヤーにメッセージを送信
            targetPlayer.sendMessage(formattedComponent);
        }

        // DiscordへのWebhook送信は常に行う
        if (configManager.isGameToDiscordEnabled()) {
            String discordMessage = String.format("[%s] %s: %s", sourceServerDisplayName, sourcePlayer.getUsername(), originalMessage);
            
            webhookManager.sendMessageWithSkin(sourcePlayer.getUsername(), discordMessage, sourceServerDisplayName)
                    .thenAccept(success -> {
                        if (!success) {
                            logger.warn("Discord Webhookへのメッセージ送信に失敗しました: {}", sourcePlayer.getUsername());
                        }
                    });
        }
    }
    
    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        RegisteredServer server = event.getServer();
        
        // プレイヤーが接続したサーバーを記録
        playerServers.put(player.getUniqueId(), server.getServerInfo().getName());
        
        // サーバー接続メッセージを送信
        String serverName = permissionManager.getServerDisplayName(server.getServerInfo().getName());
        Component message = Component.text(
                player.getUsername() + " が " + serverName + " に接続しました",
                NamedTextColor.GREEN
        );
        
        for (Player onlinePlayer : this.server.getAllPlayers()) {
            onlinePlayer.sendMessage(message);
        }
    }
    
    /**
     * 全プレイヤーにメッセージを送信（Discordからのメッセージ用）
     */
    public void broadcastMessage(String message) {
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        
        for (Player onlinePlayer : server.getAllPlayers()) {
            onlinePlayer.sendMessage(component);
        }
    }
    
    /**
     * プレイヤーが現在接続しているサーバーを取得
     */
    public String getPlayerCurrentServer(UUID playerId) {
        return playerServers.getOrDefault(playerId, "unknown");
    }
    
    /**
     * 全プレイヤーのサーバー情報を取得
     */
    public Map<UUID, String> getAllPlayerServers() {
        return new HashMap<>(playerServers);
    }

    /**
     * Discordなど、サーバー外部から受信したメッセージを全プレイヤーにブロードキャストします。
     * @param messageComponent 表示するメッセージコンポーネント
     */
    public void broadcastMessage(Component messageComponent) {
        for (Player player : server.getAllPlayers()) {
            player.sendMessage(messageComponent);
        }
    }
} 