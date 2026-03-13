package com.criztiandev.extractionevent.listeners;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.managers.AdminMonitorManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class AnonymousChatListener implements Listener {

    // Pre-built prefix component — avoids allocation on every chat message
    private static final Component ANON_PREFIX = Component.text("<")
            .append(Component.text("Anonymous", NamedTextColor.DARK_GRAY, TextDecoration.BOLD))
            .append(Component.text("> "));

    private final ExtractionEventPlugin plugin;

    public AnonymousChatListener(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * For anonymized players, CANCEL the signed chat event entirely and re-broadcast
     * the message manually. This is the ONLY correct fix for the 1.19+ "Chat validation
     * error" — the error is triggered when anything modifies a signed chat component
     * after it has left the client. By cancelling and re-sending as an unsigned server
     * message we bypass signature validation completely.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getNameTagManager().isAnonymized(player)) return;

        event.setCancelled(true);

        Component message   = event.message();
        Component formatted = ANON_PREFIX.append(message);

        String realName = plugin.getNameTagManager().getRealName(player.getUniqueId());
        Component adminFormatted = Component.text("[Monitor] ", NamedTextColor.DARK_RED)
                .append(Component.text(realName, NamedTextColor.RED))
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(message);

        Bukkit.getScheduler().runTask(plugin, () -> {
            AdminMonitorManager monitor = plugin.getAdminMonitorManager();
            for (Player recipient : Bukkit.getOnlinePlayers()) {
                boolean sees = monitor.has(recipient, AdminMonitorManager.Feature.CHAT);
                recipient.sendMessage(sees ? adminFormatted : formatted);
            }
            plugin.getServer().getConsoleSender().sendMessage(
                    "[Chat/Anon] " + realName + ": "
                    + PlainTextComponentSerializer.plainText().serialize(message)
            );
        });
    }
}
