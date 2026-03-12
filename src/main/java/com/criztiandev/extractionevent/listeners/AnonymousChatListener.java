package com.criztiandev.extractionevent.listeners;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
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

        // Cancel the original signed event — Minecraft will never validate the signature
        event.setCancelled(true);

        // Capture message content before leaving async context (Component is immutable, safe)
        Component message   = event.message();
        Component formatted = ANON_PREFIX.append(message);

        // Re-broadcast on the main thread (Bukkit collection must be read on main thread)
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player recipient : Bukkit.getOnlinePlayers()) {
                recipient.sendMessage(formatted);
            }
            // Log to console with real name so admins can audit
            String realName = plugin.getNameTagManager().getRealName(player.getUniqueId());
            plugin.getServer().getConsoleSender().sendMessage(
                    "[Chat/Anon] " + realName + ": "
                    + PlainTextComponentSerializer.plainText().serialize(message)
            );
        });
    }
}
