package com.example.velocitydiscord;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * サーバー状態監視クラス
 * 各登録されたサーバーへ定期的にpingを送信し、状態変化をDiscordに通知する
 */
public class ServerStatusMonitor implements Runnable {
    
    private final ProxyServer server;
    private final Logger logger;
    private final ConfigManager configManager;
    private final DiscordManager discordManager;
    private final ScheduledExecutorService scheduler;
    
    private ScheduledFuture<?> task;
    private int pingTimeout;
    private int scanDelay;
    
    // サーバー名 -> 前回の状態（true: オンライン, false: オフライン）
    private final Map<String, Boolean> serverStatus = new HashMap<>();
    
    public ServerStatusMonitor(ProxyServer server, Logger logger, ConfigManager configManager, 
                             DiscordManager discordManager, ScheduledExecutorService scheduler) {
        this.server = server;
        this.logger = logger;
        this.configManager = configManager;
        this.discordManager = discordManager;
        this.scheduler = scheduler;
        
        // 設定値の読み込み
        this.pingTimeout = configManager.getServerStatusPingTimeout();
        this.scanDelay = configManager.getServerStatusScanDelay();
        
        // 監視タスクの開始
        startMonitoring();
    }
    
    /**
     * 監視タスクを開始する
     */
    private void startMonitoring() {
        if (task != null && !task.isCancelled()) {
            task.cancel(true);
        }
        
        task = scheduler.scheduleAtFixedRate(this, 5, scanDelay, TimeUnit.SECONDS);
        logger.info("サーバー状態監視を開始しました (間隔: {}秒, タイムアウト: {}ms)", scanDelay, pingTimeout);
    }
    
    /**
     * 監視タスクを停止する
     */
    public void stopMonitoring() {
        if (task != null && !task.isCancelled()) {
            task.cancel(true);
            logger.info("サーバー状態監視を停止しました");
        }
    }
    
    @Override
    public void run() {
        try {
            // 登録されているすべてのサーバーをチェック
            Map<String, RegisteredServer> servers = new HashMap<>();
            for (RegisteredServer registeredServer : server.getAllServers()) {
                servers.put(registeredServer.getServerInfo().getName(), registeredServer);
            }
            
            // プレイヤーが接続中のサーバーは自動的にオンライン状態とする
            server.getAllPlayers().forEach(player -> {
                player.getCurrentServer().ifPresent(serverConnection -> {
                    String serverName = serverConnection.getServerInfo().getName();
                    if (servers.containsKey(serverName)) {
                        boolean wasOffline = serverStatus.containsKey(serverName) && !serverStatus.get(serverName);
                        if (wasOffline) {
                            announceServerStatusChange(serverName, true);
                        }
                        serverStatus.put(serverName, true);
                        servers.remove(serverName); // ping対象から除外
                    }
                });
            });
            
            // 残りのサーバーにpingを送信
            for (Map.Entry<String, RegisteredServer> entry : servers.entrySet()) {
                String serverName = entry.getKey();
                RegisteredServer registeredServer = entry.getValue();
                SocketAddress address = registeredServer.getServerInfo().getAddress();
                
                boolean isOnline = isServerReachable(address);
                Boolean previousStatus = serverStatus.get(serverName);
                
                if (previousStatus == null || previousStatus != isOnline) {
                    announceServerStatusChange(serverName, isOnline);
                }
                
                serverStatus.put(serverName, isOnline);
            }
            
        } catch (Exception e) {
            logger.error("サーバー状態監視中にエラーが発生しました", e);
        }
    }
    
    /**
     * サーバーに接続可能かどうかをチェックする
     */
    private boolean isServerReachable(SocketAddress address) {
        try (Socket socket = new Socket()) {
            socket.connect(address, pingTimeout);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * サーバー状態変化をDiscordに通知する
     */
    private void announceServerStatusChange(String serverName, boolean isOnline) {
        try {
            if (!configManager.isServerStatusEnabled()) {
                return;
            }
            
            // Embed形式でDiscordに通知
            discordManager.sendServerStatusMessage("", serverName, isOnline);
            
            logger.info("サーバー状態変化を通知: {} -> {}", configManager.getServerDisplayName(serverName), 
                isOnline ? "ONLINE" : "OFFLINE");
            
        } catch (Exception e) {
            logger.error("サーバー状態変化の通知中にエラーが発生しました", e);
        }
    }
    
    /**
     * 設定をリロードする
     */
    public void reloadConfig() {
        this.pingTimeout = configManager.getServerStatusPingTimeout();
        this.scanDelay = configManager.getServerStatusScanDelay();
        
        // 監視タスクを再開
        startMonitoring();
        logger.info("サーバー状態監視の設定をリロードしました");
    }
    
    /**
     * 現在のサーバー状態を取得する
     */
    public Map<String, Boolean> getCurrentServerStatus() {
        return new HashMap<>(serverStatus);
    }
} 