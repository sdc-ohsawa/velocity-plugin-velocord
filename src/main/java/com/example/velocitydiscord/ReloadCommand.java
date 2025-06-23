package com.example.velocitydiscord;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

public class ReloadCommand implements SimpleCommand {
    
    private final ConfigManager configManager;
    private final Logger logger;
    
    public ReloadCommand(ConfigManager configManager, Logger logger) {
        this.configManager = configManager;
        this.logger = logger;
    }
    
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        
        // 権限チェック
        if (!source.hasPermission("velocitydiscord.reload")) {
            source.sendMessage(Component.text("このコマンドを実行する権限がありません。", NamedTextColor.RED));
            return;
        }
        
        try {
            source.sendMessage(Component.text("設定ファイルを再読み込み中...", NamedTextColor.YELLOW));
            
            // config.ymlを再読み込み
            configManager.loadConfig();
            
            source.sendMessage(Component.text("✅ 設定ファイルの再読み込みが完了しました！", NamedTextColor.GREEN));
            logger.info("Configuration reloaded by {}", source.toString());
            
        } catch (Exception e) {
            source.sendMessage(Component.text("❌ 設定ファイルの再読み込み中にエラーが発生しました。", NamedTextColor.RED));
            logger.error("Error reloading configuration", e);
        }
    }
    
    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("velocitydiscord.reload");
    }
} 