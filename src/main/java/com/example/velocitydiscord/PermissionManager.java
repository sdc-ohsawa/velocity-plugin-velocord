package com.example.velocitydiscord;

import com.velocitypowered.api.proxy.Player;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionManager {
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private final Logger logger;
    private DiscordManager discordManager;

    public PermissionManager(ConfigManager configManager, DiscordManager discordManager, 
                             DatabaseManager databaseManager, Logger logger) {
        this.configManager = configManager;
        this.discordManager = discordManager;
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    public void setDiscordManager(DiscordManager discordManager) {
        this.discordManager = discordManager;
    }

    public String getServerDisplayName(String serverName) {
        Map<String, String> servers = configManager.getServers();
        return servers.getOrDefault(serverName, serverName);
    }

    /**
     * 複数サーバー対応のリアルタイム権限チェック（自動検出対応）
     * 設定済みサーバーまたは参加している全サーバーからロール情報を取得して権限を確認します
     */
    public boolean canAccessServer(UUID playerUuid, String serverName) {
        try {
            // ① DB でリンク確認
            logger.info("=== 権限チェック開始: Player {} to server {} ===", playerUuid, serverName);
            
            String discordUserId = databaseManager.getDiscordUserId(playerUuid).get();
            if (discordUserId == null) {
                logger.info("Player {} is not linked to Discord – deny access to {}", playerUuid, serverName);
                return false;
            }
            
            logger.info("✅ Discord ID取得成功: Player {} -> Discord ID {}", playerUuid, discordUserId);
            
            // ② Discord ユーザーID確保済み
            var jda = discordManager.getJda();
            if (jda == null) {
                logger.warn("JDA not ready – deny access to {}", serverName);
                return false;
            }
            
            // ③ 自動検出対応：設定済みサーバーまたは参加している全サーバーから権限チェック
            Map<String, Map<String, Object>> configuredGuilds = configManager.getDiscordGuilds();
            boolean autoDetectionEnabled = configManager.isAutoGuildDetectionEnabled();
            
            java.util.List<net.dv8tion.jda.api.entities.Guild> targetGuilds = new java.util.ArrayList<>();
            
            if (autoDetectionEnabled && configuredGuilds.isEmpty()) {
                // 完全自動モード：参加している全サーバーをチェック
                targetGuilds = jda.getGuilds();
                logger.info("🤖 自動検出モード: {} サーバーをチェック", targetGuilds.size());
            } else {
                // 設定済みサーバーをチェック
                for (String guildId : configuredGuilds.keySet()) {
                    if (!configManager.isGuildEnabled(guildId)) {
                        continue;
                    }
                    
                    var guild = jda.getGuildById(guildId);
                    if (guild != null) {
                        targetGuilds.add(guild);
                    }
                }
                logger.info("⚙️ 設定モード: {} サーバーをチェック", targetGuilds.size());
            }
            
            // ④ 各サーバーでMember取得と権限チェック
            for (net.dv8tion.jda.api.entities.Guild guild : targetGuilds) {
                String guildId = guild.getId();
                logger.info("🔍 サーバーチェック中: {} ({})", guild.getName(), guildId);
                
                Member member;
                try {
                    member = guild.retrieveMemberById(discordUserId).complete();
                } catch (Exception e) {
                    logger.warn("❌ メンバー取得失敗: Discord ID {} from guild {} – エラー: {}", 
                        discordUserId, guild.getName(), e.getMessage());
                    continue;
                }
                
                if (member == null) {
                    logger.info("ℹ️ メンバー未発見: Discord ID {} not found in guild {}", discordUserId, guild.getName());
                    continue;
                }
                
                logger.info("✅ メンバー取得成功: {} found in guild {}", member.getUser().getName(), guild.getName());
                
                // メンバーのロール情報をログ出力
                List<Role> memberRoles = member.getRoles();
                logger.info("🎭 メンバーロール数: {} in guild {}", memberRoles.size(), guild.getName());
                
                if (memberRoles.isEmpty()) {
                    logger.info("ℹ️ ロールなし: No roles for member {} in guild {}", member.getUser().getName(), guild.getName());
                } else {
                    logger.info("📋 メンバーロール一覧:");
                    for (Role role : memberRoles) {
                        logger.info("  - {} (ID: {})", role.getName(), role.getId());
                    }
                }
                
                // ⑤ ロール権限チェック
                boolean hasAccess = false;
                
                if (autoDetectionEnabled && configuredGuilds.isEmpty()) {
                    // 完全自動モード：全ロールで基本的なアクセス権限をチェック
                    logger.info("🤖 自動権限チェック実行中...");
                    hasAccess = checkAutoDetectedRolePermissions(member, serverName, guild);
                } else {
                    // 設定モード：設定されたロールマッピングをチェック
                    logger.info("⚙️ 設定ベース権限チェック実行中...");
                    Map<String, Object> guildRoleMapping = configManager.getGuildRoleMapping(guildId);
                    logger.info("📖 設定済みロール数: {} for guild {}", guildRoleMapping.size(), guild.getName());
                    
                    if (guildRoleMapping.isEmpty()) {
                        logger.warn("⚠️ ロール設定なし: No role mapping configured for guild {}", guild.getName());
                    } else {
                        logger.info("📋 設定済みロール一覧:");
                        for (String roleId : guildRoleMapping.keySet()) {
                            Object roleConfig = guildRoleMapping.get(roleId);
                            if (roleConfig instanceof Map<?,?> roleConfigMap) {
                                String roleName = (String) roleConfigMap.get("name");
                                logger.info("  - {} (ID: {})", roleName != null ? roleName : "Unknown", roleId);
                            }
                        }
                    }
                    
                    hasAccess = checkConfiguredRolePermissions(member, serverName, guildId);
                }
                
                if (hasAccess) {
                    logger.info("✅ アクセス許可: Player {} granted access to server {} by guild: {}", 
                        playerUuid, serverName, guild.getName());
                    return true;
                } else {
                    logger.info("❌ アクセス拒否: No matching role for server {} in guild {}", serverName, guild.getName());
                }
            }
            
            logger.info("❌ 最終判定: No matching roles found for player {} across all guilds – deny access to server {}", 
                playerUuid, serverName);
            return false;
            
        } catch (Exception e) {
            logger.error("💥 権限チェックエラー: Error during permission check for player {} to server {}", 
                playerUuid, serverName, e);
            // エラー時は安全のため拒否
            return false;
        }
    }
    
    /**
     * 設定されたロールマッピングに基づく権限チェック
     */
    private boolean checkConfiguredRolePermissions(Member member, String serverName, String guildId) {
        logger.info("  📝 設定ベース権限チェック開始: server={}, guild={}", serverName, guildId);
        
        Map<String, Object> guildRoleMapping = configManager.getGuildRoleMapping(guildId);
        List<Role> memberRoles = member.getRoles();
        
        logger.info("  👤 メンバー: {} ({})", member.getUser().getName(), member.getUser().getId());
        logger.info("  🎭 所持ロール数: {}", memberRoles.size());
        logger.info("  ⚙️ 設定済みロール数: {}", guildRoleMapping.size());
        
        for (Role role : memberRoles) {
            String roleId = role.getId();
            String roleName = role.getName();
            
            logger.info("  🔍 ロールチェック中: {} ({})", roleName, roleId);
            
            if (guildRoleMapping.containsKey(roleId)) {
                logger.info("  ✅ 設定済みロール発見: {}", roleName);
                
                Object roleConfig = guildRoleMapping.get(roleId);
                if (roleConfig instanceof Map<?,?> roleConfigMap) {
                    Object serversObj = roleConfigMap.get("serverAccess");
                    logger.info("  📖 サーバーアクセス設定: {}", serversObj);
                    
                    if (serversObj instanceof Map<?,?> serversMap) {
                        Object accessFlag = serversMap.get(serverName);
                        logger.info("  🎯 サーバー {} のアクセス設定: {}", serverName, accessFlag);
                        
                        if (Boolean.TRUE.equals(accessFlag)) {
                            logger.info("  ✅ アクセス許可: role={}, server={}", roleName, serverName);
                            return true;
                        } else {
                            logger.info("  ❌ アクセス拒否: role={}, server={} (設定値: {})", roleName, serverName, accessFlag);
                        }
                    } else {
                        logger.warn("  ⚠️ serverAccess設定が不正: {}", serversObj);
                    }
                } else {
                    logger.warn("  ⚠️ ロール設定が不正: {}", roleConfig);
                }
            } else {
                logger.info("  ℹ️ 未設定ロール: {} ({})", roleName, roleId);
            }
        }
        
        logger.info("  ❌ 結果: 設定ベース権限チェック失敗");
        return false;
    }
    
    /**
     * 自動検出モードでのロール権限チェック（基本的なアクセス権限）
     */
    private boolean checkAutoDetectedRolePermissions(Member member, String serverName, net.dv8tion.jda.api.entities.Guild guild) {
        logger.info("  🤖 自動検出権限チェック開始: server={}, guild={}", serverName, guild.getName());
        
        List<Role> memberRoles = member.getRoles();
        
        logger.info("  👤 メンバー: {} ({})", member.getUser().getName(), member.getUser().getId());
        logger.info("  🎭 所持ロール数: {}", memberRoles.size());
        
        // 基本ルール：何らかのロールを持っている場合は基本サーバーへのアクセスを許可
        if (!memberRoles.isEmpty()) {
            logger.info("  📋 ロール一覧:");
            for (Role role : memberRoles) {
                logger.info("    - {} ({})", role.getName(), role.getId());
            }
            
            // 一般的なサーバー名に対してアクセスを許可
            if (serverName.equals("seikatsu") || serverName.equals("sigen") || serverName.equals("lobby")) {
                logger.info("  ✅ 基本サーバーアクセス許可: {} (ロール所持による)", serverName);
                return true;
            }
            
            // 管理者権限のロールがある場合は全サーバーアクセス許可
            for (Role role : memberRoles) {
                String roleName = role.getName().toLowerCase();
                logger.info("  🔍 管理者ロールチェック: {} -> {}", role.getName(), roleName);
                
                if (roleName.contains("admin") || roleName.contains("管理") || 
                    roleName.contains("owner") || roleName.contains("mod")) {
                    logger.info("  👑 管理者ロール発見: {} -> 全サーバーアクセス許可", role.getName());
                    return true;
                }
            }
            
            logger.info("  ℹ️ 管理者ロールなし、対象外サーバー: {}", serverName);
        } else {
            logger.info("  ⚠️ ロール未所持: メンバーはロールを持っていません");
        }
        
        logger.info("  ❌ 結果: 自動検出権限チェック失敗");
        return false;
    }
    
    /**
     * プレイヤーの現在の権限状態をデバッグ用に取得（自動検出対応）
     */
    public String getPermissionDebugInfo(UUID playerUuid) {
        StringBuilder info = new StringBuilder();
        
        try {
            String discordUserId = databaseManager.getDiscordUserId(playerUuid).get();
            if (discordUserId == null) {
                return "Discord未連携";
            }
            
            info.append("Discord ID: ").append(discordUserId).append("\n");
            
            var jda = discordManager.getJda();
            if (jda == null) {
                return info.append("JDA未初期化").toString();
            }
            
            Map<String, Map<String, Object>> configuredGuilds = configManager.getDiscordGuilds();
            boolean autoDetectionEnabled = configManager.isAutoGuildDetectionEnabled();
            
            info.append("自動検出モード: ").append(autoDetectionEnabled ? "有効" : "無効").append("\n");
            info.append("設定済みサーバー数: ").append(configuredGuilds.size()).append("\n\n");
            
            java.util.List<net.dv8tion.jda.api.entities.Guild> targetGuilds = new java.util.ArrayList<>();
            
            if (autoDetectionEnabled && configuredGuilds.isEmpty()) {
                targetGuilds = jda.getGuilds();
                info.append("=== 完全自動モード (参加している全サーバー) ===\n");
            } else {
                for (String guildId : configuredGuilds.keySet()) {
                    var guild = jda.getGuildById(guildId);
                    if (guild != null) {
                        targetGuilds.add(guild);
                    }
                }
                info.append("=== 設定モード ===\n");
            }
            
            for (net.dv8tion.jda.api.entities.Guild guild : targetGuilds) {
                String guildId = guild.getId();
                info.append("--- ").append(guild.getName()).append(" (").append(guildId).append(") ---\n");
                
                try {
                    Member member = guild.retrieveMemberById(discordUserId).complete();
                    if (member == null) {
                        info.append("  メンバー: 未発見\n");
                        continue;
                    }
                    
                    info.append("  ロール: ");
                    List<Role> roles = member.getRoles();
                    if (roles.isEmpty()) {
                        info.append("なし");
                    } else {
                        for (Role role : roles) {
                            info.append(role.getName()).append(" (").append(role.getId()).append("), ");
                        }
                    }
                    info.append("\n");
                    
                    // 権限チェック結果
                    String[] testServers = {"seikatsu", "sigen", "lobby", "vip"};
                    info.append("  アクセス権限: ");
                    for (String serverName : testServers) {
                        boolean canAccess;
                        if (autoDetectionEnabled && configuredGuilds.isEmpty()) {
                            canAccess = checkAutoDetectedRolePermissions(member, serverName, guild);
                        } else {
                            canAccess = checkConfiguredRolePermissions(member, serverName, guildId);
                        }
                        info.append(serverName).append(canAccess ? "✅" : "❌").append(" ");
                    }
                    info.append("\n");
                    
                    // 設定状態の確認
                    if (!configuredGuilds.isEmpty() && configuredGuilds.containsKey(guildId)) {
                        Map<String, Object> guildRoleMapping = configManager.getGuildRoleMapping(guildId);
                        info.append("  設定済みロール数: ").append(guildRoleMapping.size()).append("\n");
                    }
                    
                } catch (Exception e) {
                    info.append("  エラー: ").append(e.getMessage()).append("\n");
                }
                info.append("\n");
            }
            
        } catch (Exception e) {
            info.append("全体エラー: ").append(e.getMessage());
        }
        
        return info.toString();
    }
} 