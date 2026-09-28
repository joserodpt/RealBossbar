package joserodpt.realbossbar.plugin;

/*
 *   ____            _ ____                 _
 *  |  _ \ ___  __ _| | __ )  ___  ___ ___| |__   __ _ _ __
 *  | |_) / _ \/ _` | |  _ \ / _ \/ __/ __| '_ \ / _` | '__|
 *  |  _ <  __/ (_| | | |_) | (_) \__ \__ \ |_) | (_| | |
 *  |_| \_\___|\__,_|_|____/ \___/|___/___/_.__/ \__,_|_|
 *
 * Licensed under the MIT License
 * @author José Rodrigues © 2026
 * @link https://github.com/joserodpt/RealBossbar
 */

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

/**
 * Fills in PlaceholderAPI placeholders when it is installed, and leaves text as it is otherwise. Nothing
 * else touches a PlaceholderAPI class, so the plugin loads without it.
 */
public final class Placeholders {

    private static boolean enabled = false;

    private Placeholders() {
    }

    static void setEnabled(final boolean enabled) {
        Placeholders.enabled = enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static String apply(final Player p, final String text) {
        if (!enabled || text == null || text.indexOf('%') < 0) {
            return text;
        }
        return PlaceholderAPI.setPlaceholders(p, text);
    }
}
