package dev.beluvu.btiers;

import net.kyori.adventure.text.Component;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

public final class Main extends JavaPlugin implements Listener {
    Msg msg;
    TierStore store;
    NameTags tags;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        msg = new Msg(this);
        store = new TierStore(this);
        tags = new NameTags(this);

        TierCommands cmds = new TierCommands(this);
        for (String name : new String[]{"tierlist", "tiertop", "settier", "removetier"}) {
            PluginCommand c = getCommand(name);
            if (c != null) {
                TabExecutor ex = cmds;
                c.setExecutor(ex);
                c.setTabCompleter(ex);
            }
        }
        getServer().getPluginManager().registerEvents(this, this);
        tags.start();
    }

    @Override
    public void onDisable() {
        if (tags != null) tags.removeAll();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        tags.remove(e.getPlayer().getUniqueId());
    }

    List<String> categories() {
        return getConfig().getStringList("categories");
    }

    /** Category name as written in config.yml (case-insensitive lookup), or null. */
    String canonCategory(String input) {
        for (String c : categories()) {
            if (c.equalsIgnoreCase(input)) return c;
        }
        return null;
    }

    String colorOf(Tier t) {
        return getConfig().getString("tier-colors." + t.name(), "&f");
    }

    /** The floating text for a player: their highest tier, or null if they have none. */
    Component tierText(UUID id) {
        TierStore.Data d = store.get(id);
        if (d == null || d.tiers.isEmpty()) return null;

        String bestCat = null;
        Tier best = null;
        // configured categories first, so ties go to the one listed earlier in config.yml
        for (String c : categories()) {
            Tier t = d.tiers.get(c);
            if (t != null && (best == null || t.points > best.points)) {
                best = t;
                bestCat = c;
            }
        }
        if (best == null) {   // tiers of categories that were removed from the config
            for (var e : d.tiers.entrySet()) {
                if (best == null || e.getValue().points > best.points) {
                    best = e.getValue();
                    bestCat = e.getKey();
                }
            }
        }
        if (best == null) return null;

        String fmt = getConfig().getString("display.format", "{color}{category} {tier}");
        return Msg.LS.deserialize(fmt
                .replace("{color}", colorOf(best))
                .replace("{category}", bestCat)
                .replace("{tier}", best.name()));
    }
}
