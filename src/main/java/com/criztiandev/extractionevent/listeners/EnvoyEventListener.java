package com.criztiandev.extractionevent.listeners;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class EnvoyEventListener implements Listener {

    private final ExtractionEventPlugin plugin;

    // Cached per-event-class reflection methods — resolved ONCE at startup
    // Key: event Class, Value: ordered list of candidate entity-getter Methods
    private final java.util.Map<Class<?>, java.util.List<Method>> entityGetters = new java.util.HashMap<>();
    private final java.util.Map<Class<?>, Method>                 setCancelledCache = new java.util.HashMap<>();

    public EnvoyEventListener(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
        registerDynamicEvents();
    }

    private void registerDynamicEvents() {
        Plugin aePlugin = Bukkit.getPluginManager().getPlugin("AdvancedEnchantments");
        if (aePlugin == null) {
            plugin.getLogger().warning("AdvancedEnchantments not found! Envoy module is bypassed.");
            return;
        }

        // Try to register for both common CE trigger events
        String[] eventClasses = {
            "net.advancedplugins.ae.api.EnchantApplyEvent",
            "net.advancedplugins.ae.impl.effects.api.EffectsActivateEvent",
            "net.advancedplugins.ae.api.EffectsActivateEvent" // For older API versions
        };

        boolean registered = false;
        String[] entityGetterNames = {"getPlayer", "getMainEntity", "getAttacker", "getVictim", "getEntity"};
        
        for (String className : eventClasses) {
            try {
                Class<? extends Event> clazz = Class.forName(className).asSubclass(Event.class);
                Bukkit.getPluginManager().registerEvent(clazz, this, EventPriority.HIGHEST, new EventExecutor() {
                    @Override
                    public void execute(Listener listener, Event event) {
                        onAEEvent(event);
                    }
                }, plugin);
                plugin.getLogger().info("Successfully hooked into AdvancedEnchantments event: " + clazz.getSimpleName());
                registered = true;
                
                // Pre-resolve entity-getter methods for this class
                java.util.List<Method> getters = new java.util.ArrayList<>();
                for (String name : entityGetterNames) {
                    try { getters.add(clazz.getMethod(name)); } catch (NoSuchMethodException ignored) {}
                }
                entityGetters.put(clazz, getters);
                
                // Pre-resolve setCancelled
                try { setCancelledCache.put(clazz, clazz.getMethod("setCancelled", boolean.class)); } catch (NoSuchMethodException ignored) {}
                
            } catch (ClassNotFoundException ignored) {
                // Ignore gracefully
            }
        }
        
        if (!registered) {
            plugin.getLogger().warning("Failed to hook into any AdvancedEnchantments event API. Envoy events will not cancel CE triggers.");
        }
    }

    private void onAEEvent(Event event) {
        // Fast-fail check: if no shift is active and no region explicitly disables, skip all coordinate math
        if (!plugin.getWarzoneShiftManager().isAnyShiftActive()
                && !plugin.getRegionManager().isAnyEnvoyEnabled()) return;

        try {
            org.bukkit.entity.Entity targetEntity = null;
            java.util.List<Method> getters = entityGetters.get(event.getClass());

            if (getters != null) {
                for (Method m : getters) {
                    Object obj = m.invoke(event);
                    if (obj instanceof org.bukkit.entity.Entity e) {
                        targetEntity = e;
                        break;
                    }
                }
            } else {
                // Fallback: class not pre-cached — scan live and cache for next time
                String[] names = {"getPlayer", "getMainEntity", "getAttacker", "getVictim", "getEntity"};
                java.util.List<Method> discovered = new java.util.ArrayList<>();
                for (String name : names) {
                    try {
                        Method m = event.getClass().getMethod(name);
                        discovered.add(m);
                        if (targetEntity == null) {
                            Object obj = m.invoke(event);
                            if (obj instanceof org.bukkit.entity.Entity e) targetEntity = e;
                        }
                    } catch (NoSuchMethodException ignored) {}
                }
                entityGetters.put(event.getClass(), discovered);
                try { setCancelledCache.putIfAbsent(event.getClass(), event.getClass().getMethod("setCancelled", boolean.class)); } catch (NoSuchMethodException ignored) {}
            }

            if (targetEntity == null) return;

            Location loc = targetEntity.getLocation();
            LevRegion region = plugin.getRegionManager().getRegionAt(loc);
            if (region == null) return;

            boolean isShiftMode    = plugin.getWarzoneShiftManager().isShiftActive(region.getId());
            boolean isRegionExplicit = region.isEnvoyEventEnabled();

            if (isShiftMode || isRegionExplicit) {
                Method sc = setCancelledCache.get(event.getClass());
                if (sc != null) sc.invoke(event, true);
            }
        } catch (Exception ex) {
            // Silent — never crash on a TPS-critical code path
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase();
        
        // Fast command check for AE commands
        boolean isAeCommand = message.startsWith("/gkit") || message.startsWith("/ae gkit") 
                           || message.startsWith("/ce") || message.startsWith("/enchanter")
                           || message.startsWith("/advancedenchantments");
                           
        if (!isAeCommand) return;

        Player player = event.getPlayer();
        if (player.hasPermission("extractionevent.admin") && !plugin.isTestMode(player.getUniqueId())) {
            return;
        }

        // Fast-fail check — single field read, no stream scan
        if (!plugin.getWarzoneShiftManager().isAnyShiftActive()
                && !plugin.getRegionManager().isAnyEnvoyEnabled()) return;

        Location loc = player.getLocation();
        LevRegion region = plugin.getRegionManager().getRegionAt(loc);
        if (region == null) return;

        boolean isShiftMode = plugin.getWarzoneShiftManager().isShiftActive(region.getId());
        boolean isRegionExplicit = region.isEnvoyEventEnabled();

        if (isShiftMode || isRegionExplicit) {
            event.setCancelled(true);
            player.sendMessage("§c✖ §7Custom enchants and GKits are currently disabled in this region!");
        }
    }
}
