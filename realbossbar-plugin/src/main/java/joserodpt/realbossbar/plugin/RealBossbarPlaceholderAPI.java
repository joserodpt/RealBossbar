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

import joserodpt.realbossbar.api.bossbar.RBossbar;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code %realbossbar_count%}, {@code %realbossbar_progress_<bar>%}, {@code %realbossbar_animation_<bar>%}
 * and {@code %realbossbar_visible_<bar>%}.
 */
public class RealBossbarPlaceholderAPI extends PlaceholderExpansion {

    private final RealBossbar plugin;

    public RealBossbarPlaceholderAPI(final RealBossbar plugin) {
        this.plugin = plugin;
    }

    /**
     * Registered from inside the plugin, so it has to survive a PlaceholderAPI reload rather than
     * being unregistered with the external expansions.
     */
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return this.plugin.getPlugin().getDescription().getAuthors().toString();
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "realbossbar";
    }

    @Override
    @NotNull
    public String getVersion() {
        return this.plugin.getVersion();
    }

    /**
     * May be asked from another thread, as by a scoreboard updating asynchronously: the bar list and
     * who sees what are safe to read from any thread, and a bar's own values are only read, never set.
     */
    @Override
    public String onRequest(final OfflinePlayer player, final String identifier) {
        switch (identifier) {
            case "count":
                return String.valueOf(this.plugin.getBossbarManager().getPermanentBossbars().size());
            case "version":
                return this.plugin.getVersion();
            default:
                break;
        }

        if (identifier.startsWith("progress_")) {
            final RBossbar bar = this.plugin.getBossbarManager().getBossbar(identifier.substring("progress_".length()));
            return bar == null ? null : String.valueOf(Math.round(bar.getCurrentProgress() * 100));
        }
        if (identifier.startsWith("animation_")) {
            final RBossbar bar = this.plugin.getBossbarManager().getBossbar(identifier.substring("animation_".length()));
            return bar == null ? null : bar.getAnimation().name();
        }
        if (identifier.startsWith("visible_")) {
            final RBossbar bar = this.plugin.getBossbarManager().getBossbar(identifier.substring("visible_".length()));
            if (bar == null) {
                return null;
            }
            final Player online = player == null ? null : player.getPlayer();
            return String.valueOf(online != null && this.plugin.getPlayerManager().isVisible(online, bar));
        }

        return null;
    }
}
