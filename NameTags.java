package dev.beluvu.btiers;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shows the tier as a floating text above each player's head using a separate text display
 * entity. It never touches teams, prefixes or the nametag itself, so LuckPerms prefixes
 * (and any tab/chat plugin) keep working exactly as before.
 */
final class NameTags {
    private static final String TAG = "btiers_display";

    private final Main plugin;
    private final Map<UUID, TextDisplay> displays = new HashMap<>();

    NameTags(Main plugin) {
        this.plugin = plugin;
    }

    void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 1L);
    }

    private boolean visible(Player p) {
        return p.isOnline() && !p.isDead()
                && p.getGameMode() != GameMode.SPECTATOR
                && !p.isInvisible()
                && !p.hasPotionEffect(PotionEffectType.INVISIBILITY);
    }

    private void tick() {
        if (!plugin.getConfig().getBoolean("display.enabled", true)) {
            removeAll();
            return;
        }
        double yOffset = plugin.getConfig().getDouble("display.y-offset", 0.85);

        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            Component text = visible(p) ? plugin.tierText(id) : null;
            if (text == null) {
                remove(id);
                continue;
            }
            Location target = p.getLocation().add(0, p.getHeight() + yOffset, 0);
            TextDisplay td = displays.get(id);
            if (td == null || !td.isValid()) {
                displays.put(id, spawn(p, target, text));
            } else {
                Location cur = td.getLocation();
                if (cur.getWorld() != target.getWorld() || cur.distanceSquared(target) > 0.0001) {
                    td.teleport(target);
                }
            }
        }

        // displays of players who are no longer online
        for (UUID id : new ArrayList<>(displays.keySet())) {
            if (Bukkit.getPlayer(id) == null) remove(id);
        }
    }

    private TextDisplay spawn(Player owner, Location at, Component text) {
        World w = at.getWorld();
        TextDisplay td = w.spawn(at, TextDisplay.class, d -> {
            d.setPersistent(false);
            d.addScoreboardTag(TAG);
            d.text(text);
            d.setBillboard(Display.Billboard.CENTER);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            d.setShadowed(true);
            d.setTeleportDuration(2);
        });
        owner.hideEntity(plugin, td);     // you don't see your own tier, others do
        return td;
    }

    /** Rebuild this player's display (after their tiers changed). */
    void refresh(UUID id) {
        remove(id);
    }

    void refreshAll() {
        removeAll();
    }

    void remove(UUID id) {
        TextDisplay td = displays.remove(id);
        if (td != null) td.remove();
    }

    void removeAll() {
        for (TextDisplay td : displays.values()) td.remove();
        displays.clear();
        // sweep anything of ours that might be left over
        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntitiesByClass(TextDisplay.class)) {
                if (e.getScoreboardTags().contains(TAG)) e.remove();
            }
        }
    }
}
