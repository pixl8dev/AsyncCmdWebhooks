package com.olliejw.cmdwebhooks.Commands;

import com.olliejw.cmdwebhooks.CmdWebhooks;
import com.olliejw.cmdwebhooks.SendWebhook;
import com.olliejw.cmdwebhooks.WebhookQueueManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Send implements CommandExecutor {
    private final CmdWebhooks plugin;
    private final WebhookQueueManager queueManager;

    public Send(final CmdWebhooks plugin) {
        this.plugin = plugin;
        this.queueManager = plugin.getWebhookQueueManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("cmdw-send")) {
            if (sender.hasPermission("cmdw.send")) {
                if (args.length == 0) {
                    sender.sendMessage(ChatColor.RED + "Usage: /cmdw-send <message>");
                    return true;
                }

                String url = plugin.getConfig().getString("SendURL");
                if (url == null || url.isEmpty()) {
                    sender.sendMessage(ChatColor.RED + "Webhook URL is not configured. Please check your config.yml");
                    return true;
                }

                SendWebhook webhook = new SendWebhook(url);
                StringBuilder sb = new StringBuilder();
                for (String arg : args) {
                    sb.append(arg).append(" ");
                }
                String toSend = sb.toString().trim();
                
                // Set up the webhook with any additional configuration
                if (plugin.getConfig().contains("WebhookName") && !plugin.getConfig().getString("WebhookName").isEmpty()) {
                    webhook.setUsername(plugin.getConfig().getString("WebhookName"));
                }
                
                if (plugin.getConfig().contains("WebhookAvatar") && !plugin.getConfig().getString("WebhookAvatar").isEmpty()) {
                    webhook.setAvatarUrl(plugin.getConfig().getString("WebhookAvatar"));
                }

                // Queue the webhook for sending
                queueManager.queueWebhook(webhook, toSend, 0);
                sender.sendMessage(ChatColor.GREEN + "Message queued for delivery!");

            } else {
                sender.sendMessage(ChatColor.RED + "You are not permitted to run this command");
            }
        }
        return true;
    }
}

