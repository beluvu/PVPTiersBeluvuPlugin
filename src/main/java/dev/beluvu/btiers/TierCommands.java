package dev.beluvu.btiers;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class TierCommands implements TabExecutor {
    private final Main plugin;

    TierCommands(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        switch (c.getName().toLowerCase(Locale.ROOT)) {
            case "tierlist" -> tierlist(s, a);
            case "tiertop" -> tiertop(s, a);
            case "settier" -> settier(s, a);
            case "removetier" -> removetier(s, a);
            default -> {
            }
        }
        return true;
    }

    private TierStore.Target resolve(String input) {
        Player p = Bukkit.getPlayerExact(input);
        if (p != null) return new TierStore.Target(p.getUniqueId(), p.getName());
        OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(input);
        if (op != null) {
            return new TierStore.Target(op.getUniqueId(), op.getName() != null ? op.getName() : input);
        }
        return plugin.store.findByName(input);
    }

    // ---------- /tierlist [player] ----------

    private void tierlist(CommandSender s, String[] a) {
        TierStore.Target t;
        if (a.length == 0) {
            if (!(s instanceof Player p)) {
                plugin.msg.send(s, "usage-tierlist");
                return;
            }
            t = new TierStore.Target(p.getUniqueId(), p.getName());
        } else {
            t = resolve(a[0]);
            if (t == null) {
                plugin.msg.send(s, "player-not-found");
                return;
            }
        }
        TierStore.Data d = plugin.store.get(t.id());
        if (d == null || d.tiers.isEmpty()) {
            plugin.msg.send(s, "list-empty");
            return;
        }
        plugin.msg.line(s, "list-header", "player", t.name());
        for (Map.Entry<String, Tier> e : d.tiers.entrySet()) {
            plugin.msg.line(s, "list-line", "category", e.getKey(),
                    "tier", e.getValue().name(), "color", plugin.colorOf(e.getValue()));
        }
    }

    // ---------- /tiertop [category] ----------

    private void tiertop(CommandSender s, String[] a) {
        String category = null;
        if (a.length > 0) {
            category = plugin.canonCategory(a[0]);
            if (category == null) {
                plugin.msg.send(s, "category-unknown", "categories", String.join(", ", plugin.categories()));
                return;
            }
        }
        List<TierStore.Ranked> top = plugin.store.top(category, 10);
        if (top.isEmpty()) {
            plugin.msg.send(s, "top-empty");
            return;
        }
        if (category == null) plugin.msg.line(s, "top-header-all");
        else plugin.msg.line(s, "top-header-category", "category", category);
        int pos = 1;
        for (TierStore.Ranked r : top) {
            if (category == null) {
                plugin.msg.line(s, "top-line-all", "pos", String.valueOf(pos),
                        "player", r.name(), "points", String.valueOf(r.points()));
            } else {
                plugin.msg.line(s, "top-line-category", "pos", String.valueOf(pos),
                        "player", r.name(), "tier", r.tier().name(), "color", plugin.colorOf(r.tier()));
            }
            pos++;
        }
    }

    // ---------- /settier <player> [category] <tier> ----------

    private void settier(CommandSender s, String[] a) {
        if (!s.hasPermission("btiers.admin")) {
            plugin.msg.send(s, "no-permission");
            return;
        }
        List<String> cats = plugin.categories();
        // with a single category configured, the category can be left out
        boolean shortForm = a.length == 2 && cats.size() == 1;
        if (a.length != 3 && !shortForm) {
            plugin.msg.send(s, "usage-settier");
            return;
        }
        TierStore.Target t = resolve(a[0]);
        if (t == null) {
            plugin.msg.send(s, "player-not-found");
            return;
        }
        String category = plugin.canonCategory(shortForm ? cats.get(0) : a[1]);
        if (category == null) {
            plugin.msg.send(s, "category-unknown", "categories", String.join(", ", cats));
            return;
        }
        Tier tier = Tier.parse(shortForm ? a[1] : a[2]);
        if (tier == null) {
            plugin.msg.send(s, "tier-unknown", "tiers", tierNames());
            return;
        }
        plugin.store.set(t.id(), t.name(), category, tier);
        plugin.tags.refresh(t.id());
        plugin.msg.send(s, "set", "player", t.name(), "tier", tier.name(), "category", category);
    }

    // ---------- /removetier <player> <category|all> ----------

    private void removetier(CommandSender s, String[] a) {
        if (!s.hasPermission("btiers.admin")) {
            plugin.msg.send(s, "no-permission");
            return;
        }
        List<String> cats = plugin.categories();
        boolean shortForm = a.length == 1 && cats.size() == 1;
        if (a.length != 2 && !shortForm) {
            plugin.msg.send(s, "usage-removetier");
            return;
        }
        TierStore.Target t = resolve(a[0]);
        if (t == null) {
            plugin.msg.send(s, "player-not-found");
            return;
        }
        String arg = shortForm ? cats.get(0) : a[1];
        String category = null;      // null = all
        if (!arg.equalsIgnoreCase("all")) {
            category = plugin.canonCategory(arg);
            if (category == null) {
                plugin.msg.send(s, "category-unknown", "categories", String.join(", ", cats));
                return;
            }
        }
        if (!plugin.store.remove(t.id(), category)) {
            plugin.msg.send(s, "nothing-to-remove");
            return;
        }
        plugin.tags.refresh(t.id());
        plugin.msg.send(s, "removed", "player", t.name(), "category", category == null ? "all" : category);
    }

    private static String tierNames() {
        List<String> out = new ArrayList<>();
        for (Tier t : Tier.values()) out.add(t.name());
        return String.join(", ", out);
    }

    // ---------- tab completion ----------

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        List<String> out = new ArrayList<>();
        String name = c.getName().toLowerCase(Locale.ROOT);
        boolean admin = s.hasPermission("btiers.admin");

        switch (name) {
            case "tierlist" -> {
                if (a.length == 1) addPlayers(out);
            }
            case "tiertop" -> {
                if (a.length == 1) out.addAll(plugin.categories());
            }
            case "settier" -> {
                if (!admin) return out;
                if (a.length == 1) addPlayers(out);
                else if (a.length == 2) out.addAll(plugin.categories());
                else if (a.length == 3) for (Tier t : Tier.values()) out.add(t.name());
            }
            case "removetier" -> {
                if (!admin) return out;
                if (a.length == 1) addPlayers(out);
                else if (a.length == 2) {
                    out.addAll(plugin.categories());
                    out.add("all");
                }
            }
            default -> {
            }
        }
        String last = a[a.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }

    private void addPlayers(List<String> out) {
        for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
    }
}
