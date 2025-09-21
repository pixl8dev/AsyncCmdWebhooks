package com.olliejw.cmdwebhooks.Events;

import com.olliejw.cmdwebhooks.CmdWebhooks;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class OnLeave implements Listener {
    private final String url;
    private final String message;
    private final CmdWebhooks plugin;

    public OnLeave(String url, String message) {
        this.plugin = CmdWebhooks.getInstance();
        this.url = url;
        this.message = message;
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        String playerName = ChatColor.stripColor(event.getPlayer().getDisplayName());
        String toSend = String.format(this.message, playerName)
                .replace("[player]", playerName);
                
        // Use the queue system to send the webhook
        plugin.sendDiscordMessage(url, toSend);
    }
}