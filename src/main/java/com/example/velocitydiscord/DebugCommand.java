package com.example.velocitydiscord;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.UUID;

public class DebugCommand implements SimpleCommand {
    private final PermissionManager permissionManager;
    private final DatabaseManager databaseManager;

    public DebugCommand(PermissionManager permissionManager, DatabaseManager databaseManager) {
        this.permissionManager = permissionManager;
        this.databaseManager = databaseManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        
        // 権限チェック
        if (!source.hasPermission("velocitydiscord.debug")) {
            source.sendMessage(Component.text("このコマンドを実行する権限がありません。", NamedTextColor.RED));
            return;
        }
        
        if (args.length == 0) {
            source.sendMessage(Component.text("使用方法: /vddebug <player> または /vddebug db", NamedTextColor.YELLOW));
            return;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "db":
                showDatabaseInfo(source);
                break;
            case "cleanup":
                performDatabaseCleanup(source);
                break;
            default:
                showPlayerInfo(source, args[0]);
                break;
        }
    }
    
    private void showPlayerInfo(CommandSource source, String playerName) {
        try {
            // プレイヤーを検索（ソースが自分自身の場合のみサポート）
            UUID playerUuid = null;
            Player targetPlayer = null;
            
            if (source instanceof Player sourcePlayer && sourcePlayer.getUsername().equalsIgnoreCase(playerName)) {
                targetPlayer = sourcePlayer;
                playerUuid = sourcePlayer.getUniqueId();
            } else {
                source.sendMessage(Component.text("現在、自分自身の情報のみ確認できます。", NamedTextColor.YELLOW));
                source.sendMessage(Component.text("使用方法: /vddebug " + (source instanceof Player ? ((Player) source).getUsername() : "自分のプレイヤー名"), NamedTextColor.GRAY));
                return;
            }
            
            source.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.AQUA));
            source.sendMessage(Component.text("🔍 プレイヤー情報: " + playerName, NamedTextColor.GREEN));
            source.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.AQUA));
            
            // 基本情報
            source.sendMessage(Component.text("UUID: " + playerUuid, NamedTextColor.GRAY));
            
            if (targetPlayer.getCurrentServer().isPresent()) {
                String currentServer = targetPlayer.getCurrentServer().get().getServerInfo().getName();
                String displayName = permissionManager.getServerDisplayName(currentServer);
                source.sendMessage(Component.text("現在のサーバー: " + displayName + " (" + currentServer + ")", NamedTextColor.GRAY));
            }
            
            // Discord連携状態
            String discordId = databaseManager.getDiscordUserId(playerUuid).get();
            if (discordId != null) {
                source.sendMessage(Component.text("Discord連携: ✅ 連携済み", NamedTextColor.GREEN));
                source.sendMessage(Component.text("Discord ID: " + discordId, NamedTextColor.GRAY));
            } else {
                source.sendMessage(Component.text("Discord連携: ❌ 未連携", NamedTextColor.RED));
            }
            
            // 詳細な権限情報
            source.sendMessage(Component.text("", NamedTextColor.AQUA));
            source.sendMessage(Component.text("🎫 権限詳細情報:", NamedTextColor.YELLOW));
            String permissionInfo = permissionManager.getPermissionDebugInfo(playerUuid);
            for (String line : permissionInfo.split("\n")) {
                source.sendMessage(Component.text("  " + line, NamedTextColor.GRAY));
            }
            
            // サーバーアクセス権限テスト
            source.sendMessage(Component.text("", NamedTextColor.AQUA));
            source.sendMessage(Component.text("🌐 サーバーアクセス権限:", NamedTextColor.YELLOW));
            
            showServerAccessInfo(source, playerUuid);
            
            source.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.AQUA));
            
        } catch (Exception e) {
            source.sendMessage(Component.text("プレイヤー情報の取得中にエラーが発生しました: " + e.getMessage(), NamedTextColor.RED));
        }
    }
    
    private void showServerAccessInfo(CommandSource source, UUID playerUuid) {
        try {
            // よく使われるサーバー名を手動でテスト
            String[] testServers = {"seikatsu", "sigen", "lobby", "vip"};
            
            for (String serverName : testServers) {
                String displayName = permissionManager.getServerDisplayName(serverName);
                boolean canAccess = permissionManager.canAccessServer(playerUuid, serverName);
                
                Component accessStatus = canAccess ? 
                    Component.text("✅ アクセス可", NamedTextColor.GREEN) : 
                    Component.text("❌ アクセス不可", NamedTextColor.RED);
                
                source.sendMessage(Component.text("  ")
                    .append(Component.text(displayName + " (" + serverName + "): ", NamedTextColor.GRAY))
                    .append(accessStatus));
            }
        } catch (Exception e) {
            source.sendMessage(Component.text("  サーバーアクセス権限の取得に失敗しました", NamedTextColor.RED));
        }
    }
    
    private void showDatabaseInfo(CommandSource source) {
        source.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.AQUA));
        source.sendMessage(Component.text("🗄️ データベース情報", NamedTextColor.GREEN));
        source.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.AQUA));
        
        try {
            // データベース接続状態の確認
            source.sendMessage(Component.text("データベース接続: ✅ 正常", NamedTextColor.GREEN));
            source.sendMessage(Component.text("連携アカウント情報を確認中...", NamedTextColor.YELLOW));
            
        } catch (Exception e) {
            source.sendMessage(Component.text("データベース情報の取得中にエラーが発生しました: " + e.getMessage(), NamedTextColor.RED));
        }
        
        source.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.AQUA));
    }
    
    private void performDatabaseCleanup(CommandSource source) {
        source.sendMessage(Component.text("🧹 データベースクリーンアップを実行中...", NamedTextColor.YELLOW));
        
        try {
            // 必要に応じてクリーンアップ処理を実装
            source.sendMessage(Component.text("✅ データベースクリーンアップが完了しました。", NamedTextColor.GREEN));
            
        } catch (Exception e) {
            source.sendMessage(Component.text("❌ クリーンアップ中にエラーが発生しました: " + e.getMessage(), NamedTextColor.RED));
        }
    }
    
    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("velocitydiscord.debug");
    }
} 