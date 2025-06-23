package com.example.velocitydiscord;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * サーバー状態確認コマンド
 * /serverstatus でサーバーの現在の状態を表示
 */
public class ServerStatusCommand implements SimpleCommand {
    
    private final VelocityDiscordPlugin plugin;
    private final Logger logger;
    
    public ServerStatusCommand(VelocityDiscordPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }
    
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        
        ServerStatusMonitor monitor = plugin.getServerStatusMonitor();
        if (monitor == null) {
            source.sendMessage(Component.text("サーバー状態監視機能が無効化されています。", NamedTextColor.RED));
            return;
        }
        
        Map<String, Boolean> serverStatus = monitor.getCurrentServerStatus();
        
        if (serverStatus.isEmpty()) {
            source.sendMessage(Component.text("監視対象のサーバーがありません。", NamedTextColor.YELLOW));
            return;
        }
        
        source.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GRAY));
        source.sendMessage(Component.text("🖥️ サーバー状態一覧", NamedTextColor.AQUA));
        source.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GRAY));
        
        for (Map.Entry<String, Boolean> entry : serverStatus.entrySet()) {
            String serverName = entry.getKey();
            Boolean isOnline = entry.getValue();
            
            String displayName = plugin.getConfigManager().getServerDisplayName(serverName);
            
            Component statusComponent;
            if (isOnline) {
                statusComponent = Component.text("🟢 オンライン", NamedTextColor.GREEN);
            } else {
                statusComponent = Component.text("🔴 オフライン", NamedTextColor.RED);
            }
            
            source.sendMessage(
                Component.text("  ")
                    .append(Component.text(displayName, NamedTextColor.WHITE))
                    .append(Component.text(" (", NamedTextColor.GRAY))
                    .append(Component.text(serverName, NamedTextColor.GRAY))
                    .append(Component.text("): ", NamedTextColor.GRAY))
                    .append(statusComponent)
            );
        }
        
        source.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GRAY));
    }
    
    @Override
    public boolean hasPermission(Invocation invocation) {
        // 管理者権限を持つプレイヤーのみ実行可能
        return invocation.source().hasPermission("velocord.serverstatus");
    }
    
    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of(); // サブコマンドなし
    }
} 