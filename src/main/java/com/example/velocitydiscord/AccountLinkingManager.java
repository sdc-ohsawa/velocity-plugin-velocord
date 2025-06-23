package com.example.velocitydiscord;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.Map;

public class AccountLinkingManager extends ListenerAdapter implements SimpleCommand {

    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private final PermissionManager permissionManager;
    private final Logger logger;
    private final ScheduledExecutorService scheduler;
    
    private final ConcurrentHashMap<String, UUID> verificationCodes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> codeExpiry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UUID> playerUuidCache = new ConcurrentHashMap<>();
    
    public AccountLinkingManager(ConfigManager configManager, DatabaseManager databaseManager, 
                                 PermissionManager permissionManager, Logger logger, ScheduledExecutorService scheduler) {
        this.configManager = configManager;
        this.databaseManager = databaseManager;
        this.permissionManager = permissionManager;
        this.logger = logger;
        this.scheduler = scheduler;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("link")) return;

        String code = event.getOption("code").getAsString();
        String discordUserId = event.getUser().getId();

        if (verificationCodes.containsKey(code)) {
            UUID playerUuid = verificationCodes.get(code);
            
            databaseManager.linkAccount(playerUuid, discordUserId).thenAccept(success -> {
                if (success) {
                    event.reply("✅ アカウント連携が完了しました！ Minecraftに再接続してください。").setEphemeral(true).queue();
                    logger.info("Successfully linked Discord user {} to Minecraft UUID: {}", discordUserId, playerUuid);
                    verificationCodes.remove(code);
                    codeExpiry.remove(code);
                } else {
                    event.reply("❌ データベースエラーにより、アカウント連携に失敗しました。").setEphemeral(true).queue();
                }
            });
        } else {
            event.reply("❌ 無効な、または期限切れの連携コードです。").setEphemeral(true).queue();
        }
    }

    @Override
    public void execute(Invocation invocation) {
        invocation.source().sendMessage(Component.text("アカウント連携はDiscordのスラッシュコマンド `/link` から行ってください。", NamedTextColor.RED));
    }
    
    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        // PreLoginEventでは連携チェックを行わない（Real UUIDが取得できないため）
        // 連携チェックはServerPreConnectEventで実行
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        String username = event.getPlayer().getUsername();
        UUID realUuid = event.getPlayer().getUniqueId();
        
        logger.info("PostLogin: Player {} - UUID: {}", username, realUuid);
        
        // UUIDをキャッシュ
        playerUuidCache.put(username, realUuid);
    }

    public String generateVerificationCode(UUID playerUuid) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        verificationCodes.put(code, playerUuid);
        
        long expiryTime = System.currentTimeMillis() + (long) configManager.getAccountLinkingVerificationTimeout() * 60 * 1000;
        codeExpiry.put(code, expiryTime);

        scheduler.schedule(() -> {
            if (Long.valueOf(expiryTime).equals(codeExpiry.get(code))) {
                verificationCodes.remove(code);
                codeExpiry.remove(code);
            }
        }, configManager.getAccountLinkingVerificationTimeout(), TimeUnit.MINUTES);

        return code;
    }
    
    public CompletableFuture<String> getPlayerDiscordId(UUID minecraftUuid) {
        return databaseManager.getDiscordUserId(minecraftUuid);
    }
    
    public CompletableFuture<UUID> getMinecraftUuid(String discordUserId) {
        return databaseManager.getMinecraftUuid(discordUserId);
    }
    
    public CompletableFuture<Boolean> unlinkAccount(UUID minecraftUuid) {
        return databaseManager.unlinkAccount(minecraftUuid);
    }
} 