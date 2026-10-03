package dev.beluvu.btiers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

final class Msg {
    static final LegacyComponentSerializer LS = LegacyComponentSerializer.legacyAmpersand();
    private final Main plugin;

    Msg(Main plugin) {
        this.plugin = plugin;
    }

    /** Text from messages.<key> with {placeholders} replaced (kv = key, value, key, value...). */
    String raw(String key, String... kv) {
        String s = plugin.getConfig().getString("messages." + key, key);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            s = s.replace("{" + kv[i] + "}", kv[i + 1]);
        }
        return s;
    }

    void send(CommandSender to, String key, String... kv) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        to.sendMessage(LS.deserialize(prefix + raw(key, kv)));
    }

    /** Same but without the prefix (for list/top lines). */
    void line(CommandSender to, String key, String... kv) {
        to.sendMessage(LS.deserialize(raw(key, kv)));
    }
}
