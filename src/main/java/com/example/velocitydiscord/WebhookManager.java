package com.example.velocitydiscord;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class WebhookManager {
    
    private final String webhookUrl;
    private final Logger logger;
    private final HttpClient httpClient;
    private final Gson gson;
    
    public WebhookManager(String webhookUrl, Logger logger) {
        this.webhookUrl = webhookUrl;
        this.logger = logger;
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }
    
    /**
     * プレイヤーのスキンアイコン付きでメッセージを送信
     */
    public CompletableFuture<Boolean> sendMessageWithSkin(String playerName, String message, String serverName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("YOUR_WEBHOOK_URL")) {
                    logger.warn("Webhook URLが設定されていません");
                    return false;
                }
                
                // MinecraftスキンのアイコンURLを生成
                String avatarUrl = String.format("https://mc-heads.net/avatar/%s", playerName);
                
                // Webhookペイロードを作成（プレイヤーの名前とアイコンで送信）
                JsonObject payload = new JsonObject();
                payload.addProperty("content", message);
                payload.addProperty("username", playerName);
                payload.addProperty("avatar_url", avatarUrl);
                
                // HTTPリクエストを作成
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(webhookUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();
                
                // リクエストを送信
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 204) {
                    return true;
                } else {
                    logger.error("Webhook送信に失敗しました。ステータスコード: {}, レスポンス: {}", 
                        response.statusCode(), response.body());
                    return false;
                }
                
            } catch (IOException | InterruptedException e) {
                logger.error("Webhook送信中にエラーが発生しました", e);
                return false;
            }
        });
    }
    
    /**
     * 通常のメッセージを送信（アイコンなし）
     */
    public CompletableFuture<Boolean> sendMessage(String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("YOUR_WEBHOOK_URL")) {
                    logger.warn("Webhook URLが設定されていません");
                    return false;
                }
                
                JsonObject payload = new JsonObject();
                payload.addProperty("content", message);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(webhookUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 204) {
                    return true;
                } else {
                    logger.error("Webhook送信に失敗しました。ステータスコード: {}, レスポンス: {}", 
                        response.statusCode(), response.body());
                    return false;
                }
                
            } catch (IOException | InterruptedException e) {
                logger.error("Webhook送信中にエラーが発生しました", e);
                return false;
            }
        });
    }
} 