package com.example.velocitydiscord;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {
    private final ConfigManager configManager;
    private final Logger logger;
    private HikariDataSource dataSource;

    public DatabaseManager(ConfigManager configManager, Logger logger) {
        this.configManager = configManager;
        this.logger = logger;
    }

    public void initialize() {
        String dbType = configManager.getDatabaseType();
        logger.info("{} データベースの初期化を開始します...", dbType);

        // ドライバーを明示的にロード
        if ("mysql".equalsIgnoreCase(dbType)) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                logger.info("MySQLドライバーをロードしました。");
            } catch (ClassNotFoundException e) {
                logger.error("MySQLドライバーが見つかりません。", e);
                return;
            }
        } else if ("sqlite".equalsIgnoreCase(dbType)) {
            try {
                Class.forName("org.sqlite.JDBC");
                logger.info("SQLiteドライバーをロードしました。");
            } catch (ClassNotFoundException e) {
                logger.error("SQLiteドライバーが見つかりません。", e);
                return;
            }
        }

        HikariConfig config = new HikariConfig();

        if ("mysql".equalsIgnoreCase(dbType)) {
            Map<String, Object> mysqlConfig = configManager.getMysqlConfig();
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s",
                    mysqlConfig.get("host"),
                    mysqlConfig.get("port"),
                    mysqlConfig.get("database")));
            config.setUsername((String) mysqlConfig.get("username"));
            config.setPassword((String) mysqlConfig.get("password"));
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useSSL", "false");
            config.addDataSourceProperty("allowPublicKeyRetrieval", "true");
        } else { // sqlite
            File dbFile = new File("plugins/VelocityDiscord/accounts.db");
            logger.info("SQLite database file path: {}", dbFile.getAbsolutePath());
            
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
                logger.info("Created database directory: {}", dbFile.getParentFile().getAbsolutePath());
            }
            try {
                if (!dbFile.exists()) {
                    dbFile.createNewFile();
                    logger.info("Created new database file: {}", dbFile.getAbsolutePath());
                } else {
                    logger.info("Using existing database file: {}", dbFile.getAbsolutePath());
                }
            } catch (IOException e) {
                logger.error("SQLiteデータベースファイルの作成に失敗しました。", e);
            }
            config.setDriverClassName("org.sqlite.JDBC");
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setMaximumPoolSize(1); // SQLiteは単一接続のみサポート
        }

        try {
            dataSource = new HikariDataSource(config);
            createTables();
            logger.info("{} データベースの初期化が完了しました。", dbType);
        } catch (Exception e) {
            logger.error("{} データベースの初期化中にエラーが発生しました。", dbType, e);
        }
    }

    private void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS linked_accounts ("
                   + "id INT AUTO_INCREMENT PRIMARY KEY,"
                   + "minecraft_uuid VARCHAR(36) NOT NULL UNIQUE,"
                   + "discord_id VARCHAR(255) NOT NULL UNIQUE"
                   + ");";
        if ("sqlite".equalsIgnoreCase(configManager.getDatabaseType())) {
            sql = "CREATE TABLE IF NOT EXISTS linked_accounts ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "minecraft_uuid TEXT NOT NULL UNIQUE,"
                + "discord_id TEXT NOT NULL UNIQUE"
                + ");";
        }
        
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.info("Database table 'linked_accounts' created/verified successfully");
            
        } catch (SQLException e) {
            logger.error("テーブルの作成に失敗しました", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("データベース接続をシャットダウンしました。");
        }
    }

    public CompletableFuture<Boolean> linkAccount(UUID minecraftUuid, String discordId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO linked_accounts (minecraft_uuid, discord_id) VALUES (?, ?) "
                       + "ON DUPLICATE KEY UPDATE discord_id = VALUES(discord_id);";
            if ("sqlite".equalsIgnoreCase(configManager.getDatabaseType())) {
                sql = "INSERT INTO linked_accounts (minecraft_uuid, discord_id) VALUES (?, ?) "
                    + "ON CONFLICT(minecraft_uuid) DO UPDATE SET discord_id=excluded.discord_id;";
            }
            
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, minecraftUuid.toString());
                pstmt.setString(2, discordId);
                pstmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                logger.error("アカウント連携中にエラーが発生しました", e);
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> isAccountLinked(UUID minecraftUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT 1 FROM linked_accounts WHERE minecraft_uuid = ?";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, minecraftUuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                logger.error("連携状態の確認中にエラーが発生しました", e);
                return false;
            }
        });
    }
    
    public CompletableFuture<String> getDiscordUserId(UUID minecraftUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT discord_id FROM linked_accounts WHERE minecraft_uuid = ?";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, minecraftUuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("discord_id");
                    }
                }
            } catch (SQLException e) {
                logger.error("DiscordユーザーIDの取得中にエラーが発生しました", e);
            }
            return null;
        });
    }
    
    public CompletableFuture<UUID> getMinecraftUuid(String discordId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT minecraft_uuid FROM linked_accounts WHERE discord_id = ?";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, discordId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return UUID.fromString(rs.getString("minecraft_uuid"));
                    }
                }
            } catch (SQLException e) {
                logger.error("Minecraft UUIDの取得中にエラーが発生しました", e);
            }
            return null;
        });
    }
    
    public CompletableFuture<Boolean> unlinkAccount(UUID minecraftUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM linked_accounts WHERE minecraft_uuid = ?";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, minecraftUuid.toString());
                int affected = pstmt.executeUpdate();
                return affected > 0;
            } catch (SQLException e) {
                logger.error("アカウント連携解除中にエラーが発生しました", e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> unlinkByDiscordId(String discordId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM linked_accounts WHERE discord_id = ?";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, discordId);
                int affected = pstmt.executeUpdate();
                return affected > 0;
            } catch (SQLException e) {
                logger.error("Discord IDによるアカウント連携解除中にエラーが発生しました", e);
                return false;
            }
        });
    }
    
    // デバッグ用：データベース情報を表示
    public void debugDatabaseContents() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // テーブル情報を確認
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM linked_accounts")) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    logger.info("Connected accounts: {}", count);
                }
            }
            
        } catch (SQLException e) {
            logger.error("データベース内容の確認中にエラーが発生しました", e);
        }
    }
} 