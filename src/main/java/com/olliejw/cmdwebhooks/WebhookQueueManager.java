package com.olliejw.cmdwebhooks;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.olliejw.cmdwebhooks.exceptions.RateLimitException;

public class WebhookQueueManager {
    private final CmdWebhooks plugin;
    private final Queue<QueuedWebhook> webhookQueue;
    private BukkitTask retryTask;
    private long lastRequestTime = 0;
    private static final long RATE_LIMIT_DELAY = 5000; // 5 seconds delay between retries
    private static final int MAX_RETRIES = 3;
    private static final int MAX_QUEUE_SIZE = 1000; // Maximum number of messages to queue

    public WebhookQueueManager(CmdWebhooks plugin) {
        this.plugin = plugin;
        this.webhookQueue = new ConcurrentLinkedQueue<>();
    }

    public void queueWebhook(SendWebhook webhook, String content, int retryCount) {
        if (webhookQueue.size() >= MAX_QUEUE_SIZE) {
            // If queue is full, log a warning and don't add the new message
            plugin.getLogger().warning("Webhook queue is full (" + MAX_QUEUE_SIZE + " messages), dropping message: " + 
                (content.length() > 50 ? content.substring(0, 47) + "..." : content));
            return;
        }
        webhookQueue.add(new QueuedWebhook(webhook, content, retryCount));
        processQueue();
    }

    private void processQueue() {
        if (retryTask != null || webhookQueue.isEmpty()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceLastRequest = currentTime - lastRequestTime;

        // If we've waited long enough since the last request, process immediately
        if (timeSinceLastRequest >= RATE_LIMIT_DELAY) {
            processNextWebhook();
        } else {
            // Otherwise, schedule the next webhook to be sent after the delay
            long delay = RATE_LIMIT_DELAY - timeSinceLastRequest;
            retryTask = Bukkit.getScheduler().runTaskLater(plugin, this::processNextWebhook, delay / 50); // Convert to ticks
        }
    }

    private void processNextWebhook() {
        // Clean up any expired messages first
        while (!webhookQueue.isEmpty() && webhookQueue.peek().isExpired()) {
            QueuedWebhook expired = webhookQueue.poll();
            plugin.getLogger().warning("Dropping expired webhook message: " + 
                (expired.content.length() > 50 ? expired.content.substring(0, 47) + "..." : expired.content));
        }

        if (webhookQueue.isEmpty()) {
            retryTask = null;
            return;
        }

        QueuedWebhook queued = webhookQueue.peek();
        if (queued == null) {
            webhookQueue.poll(); // Remove null entry if present
            processQueue();
            return;
        }

        lastRequestTime = System.currentTimeMillis();
        
        try {
            queued.webhook.setContent(queued.content);
            queued.webhook.execute();
            webhookQueue.poll(); // Remove successfully sent webhook
            plugin.getLogger().info("Successfully sent queued webhook");
        } catch (RateLimitException e) {
            // If we hit rate limit again, update the retry count and keep in queue
            if (queued.retryCount < MAX_RETRIES) {
                queued.retryCount++;
                plugin.getLogger().warning("Webhook rate limited. Attempt " + queued.retryCount + " of " + MAX_RETRIES);
            } else {
                plugin.getLogger().severe("Failed to send webhook after " + MAX_RETRIES + " attempts: " + e.getMessage());
                webhookQueue.poll(); // Remove after max retries
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to send webhook: " + e.getMessage());
            webhookQueue.poll(); // Remove on other errors
        }

        // Schedule next webhook in the queue
        retryTask = Bukkit.getScheduler().runTaskLater(plugin, this::processNextWebhook, RATE_LIMIT_DELAY / 50);
    }

    public void shutdown() {
        if (retryTask != null) {
            retryTask.cancel();
            retryTask = null;
        }
    }

    private static class QueuedWebhook {
        final SendWebhook webhook;
        final String content;
        final long timestamp;
        int retryCount;

        QueuedWebhook(SendWebhook webhook, String content, int retryCount) {
            this.webhook = webhook;
            this.content = content;
            this.retryCount = retryCount;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            // Messages older than 1 hour are considered expired
            return (System.currentTimeMillis() - timestamp) > 3600000; // 1 hour in milliseconds
        }
    }
}
