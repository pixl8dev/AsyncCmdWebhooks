package com.olliejw.cmdwebhooks;

import com.olliejw.cmdwebhooks.Commands.Reload;
import com.olliejw.cmdwebhooks.Commands.Send;
import com.olliejw.cmdwebhooks.Events.OnCmd;
import com.olliejw.cmdwebhooks.Events.OnJoin;
import com.olliejw.cmdwebhooks.Events.OnLeave;
import com.olliejw.cmdwebhooks.Events.OnMsg;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public class CmdWebhooks extends JavaPlugin implements Listener {

    private String webhook;
    private static CmdWebhooks instance;
    private WebhookQueueManager webhookQueueManager;

    public static CmdWebhooks instance() {
        return instance;
    }

    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();
        
        // Initialize webhook queue manager
        this.webhookQueueManager = new WebhookQueueManager(this);

        //============== COMMANDS ==============//
        this.getCommand("cmdw-reload").setExecutor(new Reload(this));
        this.getCommand("cmdw-send").setExecutor(new Send(this));

        //============== STARTUP MESSAGE ==============//
        this.webhook = this.getConfig().getString("WebhookURL");
        String message;
        if (this.getConfig().getBoolean("Start.Enabled")) {
            this.sendDiscordMessage(this.getConfig().getString("Start.Message"));
        }

        //============== REGISTER EVENTS ==============//
        if (this.getConfig().getBoolean("Join.Enabled")) {
            message = this.getConfig().getString("Join.Message");
            if (!this.getConfig().getString("Join.Webhook").equals("DEFAULT")) {
                webhook = (this.getConfig().getString("Join.Webhook"));
            } else {
                webhook = (this.getConfig().getString("DefaultURL"));
            }
            this.getServer().getPluginManager().registerEvents(new OnJoin(webhook, message), this);
        }
        if (this.getConfig().getBoolean("Leave.Enabled")) {
            message = this.getConfig().getString("Leave.Message");
            if (!this.getConfig().getString("Leave.Webhook").equals("DEFAULT")) {
                webhook = (this.getConfig().getString("Leave.Webhook"));
            } else {
                webhook = (this.getConfig().getString("DefaultURL"));
            }
            this.getServer().getPluginManager().registerEvents(new OnLeave(webhook, message), this);
        }
        if (this.getConfig().getBoolean("Msg.Enabled")) {
            message = this.getConfig().getString("Msg.Message");
            if (!this.getConfig().getString("Msg.Webhook").equals("DEFAULT")) {
                webhook = (this.getConfig().getString("Msg.Webhook"));
            } else {
                webhook = (this.getConfig().getString("DefaultURL"));
            }
            this.getServer().getPluginManager().registerEvents(new OnMsg(webhook, message), this);
        }
        if (this.getConfig().getBoolean("Cmd.Enabled")) {
            message = this.getConfig().getString("Cmd.Message");
            if (!this.getConfig().getString("Cmd.Webhook").equals("DEFAULT")) {
                webhook = (this.getConfig().getString("Cmd.Webhook"));
            } else {
                webhook = (this.getConfig().getString("DefaultURL"));
            }
            this.getServer().getPluginManager().registerEvents(new OnCmd(webhook, message), this);
        }
        //================= END =================//
    }

    /**
     * Sends a message to Discord using the webhook queue system
     * @param message The message to send
     */
    public void sendDiscordMessage(String message) {
        sendDiscordMessage(this.webhook, message);
    }

    /**
     * Sends a message to a specific webhook URL using the queue system
     * @param webhookUrl The webhook URL to send the message to
     * @param message The message to send
     */
    public void sendDiscordMessage(String webhookUrl, String message) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            getLogger().warning("Cannot send webhook: Webhook URL is not set");
            return;
        }

        SendWebhook webhook = new SendWebhook(webhookUrl);
        
        // Apply global webhook settings if configured
        if (getConfig().contains("WebhookName") && !getConfig().getString("WebhookName").isEmpty()) {
            webhook.setUsername(getConfig().getString("WebhookName"));
        }
        
        if (getConfig().contains("WebhookAvatar") && !getConfig().getString("WebhookAvatar").isEmpty()) {
            webhook.setAvatarUrl(getConfig().getString("WebhookAvatar"));
        }
        
        // Queue the webhook for sending
        webhookQueueManager.queueWebhook(webhook, message, 0);
        getLogger().log(Level.INFO, "Queued webhook message for delivery");
    }

    public void onDisable() {
        File config = new File(getDataFolder(), "config.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(config);

        if (cfg.getBoolean("Stop.Enabled")) {
            this.sendDiscordMessage(cfg.getString("Stop.Message"));
        }
        
        // Shutdown the webhook queue manager
        if (webhookQueueManager != null) {
            webhookQueueManager.shutdown();
        }
    }
    
    /**
     * Get the WebhookQueueManager instance
     * @return The WebhookQueueManager instance
     */
    public WebhookQueueManager getWebhookQueueManager() {
        return webhookQueueManager;
    }
    
    /**
     * Get the plugin instance
     * @return The CmdWebhooks instance
     */
    public static CmdWebhooks getInstance() {
        return instance;
    }
}
