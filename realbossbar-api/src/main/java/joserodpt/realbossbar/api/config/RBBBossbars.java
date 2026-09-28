package joserodpt.realbossbar.api.config;

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

import dev.dejvokep.boostedyaml.YamlDocument;
import joserodpt.realutils.config.YamlConfig;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * bossbars.yml, where every saved bar lives. The Bossbars section is the server owner's, so an
 * update never writes the bundled examples back into it, and a bar they deleted stays deleted.
 */
public class RBBBossbars {

    private static YamlConfig config;

    public static void setup(final JavaPlugin rbb) {
        config = YamlConfig.of(rbb, "bossbars.yml").versioned("Version").ignoring("1", "Bossbars")
                .useDefaults(false).load();
    }

    public static YamlDocument file() {
        return config.file();
    }

    public static void save() {
        config.save();
    }

    public static void reload() {
        config.reload();
    }
}
