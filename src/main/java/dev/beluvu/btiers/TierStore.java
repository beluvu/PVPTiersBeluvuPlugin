package dev.beluvu.btiers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class TierStore {

    static final class Data {
        String name = "?";
        final Map<String, Tier> tiers = new LinkedHashMap<>();   // category -> tier
    }

    record Target(UUID id, String name) {
    }

    record Ranked(String name, int points, Tier tier) {
    }

    private final Main plugin;
    private final File file;
    private final Map<UUID, Data> data = new HashMap<>();

    TierStore(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "tiers.yml");
        load();
    }

    void load() {
        data.clear();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = y.getConfigurationSection("players");
        if (players == null) return;
        for (String key : players.getKeys(false)) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ConfigurationSection s = players.getConfigurationSection(key);
            if (s == null) continue;
            Data d = new Data();
            d.name = s.getString("name", "?");
            ConfigurationSection ts = s.getConfigurationSection("tiers");
            if (ts != null) {
                for (String cat : ts.getKeys(false)) {
                    Tier t = Tier.parse(ts.getString(cat));
                    if (t != null) d.tiers.put(cat, t);
                }
            }
            if (!d.tiers.isEmpty()) data.put(id, d);
        }
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, Data> e : data.entrySet()) {
            String base = "players." + e.getKey();
            y.set(base + ".name", e.getValue().name);
            for (Map.Entry<String, Tier> t : e.getValue().tiers.entrySet()) {
                y.set(base + ".tiers." + t.getKey(), t.getValue().name());
            }
        }
        try {
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save tiers.yml: " + ex.getMessage());
        }
    }

    Data get(UUID id) {
        return data.get(id);
    }

    Target findByName(String name) {
        for (Map.Entry<UUID, Data> e : data.entrySet()) {
            if (e.getValue().name.equalsIgnoreCase(name)) return new Target(e.getKey(), e.getValue().name);
        }
        return null;
    }

    void set(UUID id, String name, String category, Tier tier) {
        Data d = data.computeIfAbsent(id, k -> new Data());
        d.name = name;
        d.tiers.put(category, tier);
        save();
    }

    /** category == null removes every tier of the player. Returns true if something was removed. */
    boolean remove(UUID id, String category) {
        Data d = data.get(id);
        if (d == null) return false;
        boolean changed;
        if (category == null) {
            changed = !d.tiers.isEmpty();
            d.tiers.clear();
        } else {
            changed = d.tiers.remove(category) != null;
        }
        if (d.tiers.isEmpty()) data.remove(id);
        if (changed) save();
        return changed;
    }

    /** category == null: overall ranking by total points; otherwise ranking inside that category. */
    List<Ranked> top(String category, int limit) {
        List<Ranked> out = new ArrayList<>();
        for (Data d : data.values()) {
            if (category == null) {
                int sum = 0;
                for (Tier t : d.tiers.values()) sum += t.points;
                if (sum > 0) out.add(new Ranked(d.name, sum, null));
            } else {
                Tier t = d.tiers.get(category);
                if (t != null) out.add(new Ranked(d.name, t.points, t));
            }
        }
        out.sort(Comparator.comparingInt(Ranked::points).reversed().thenComparing(Ranked::name, String.CASE_INSENSITIVE_ORDER));
        return out.size() > limit ? out.subList(0, limit) : out;
    }
}
