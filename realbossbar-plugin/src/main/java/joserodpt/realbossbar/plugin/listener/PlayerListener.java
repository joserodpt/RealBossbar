package joserodpt.realbossbar.plugin.listener;

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

import joserodpt.realbossbar.api.config.TranslatableLine;
import joserodpt.realbossbar.plugin.RealBossbar;
import joserodpt.realbossbar.plugin.RealBossbarPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import static joserodpt.realbossbar.api.config.TranslatableLine.TranslatableLinePlaceholder.NAME;
import static joserodpt.realbossbar.api.config.TranslatableLine.TranslatableLinePlaceholder.VALUE;

/**
 * Joining, leaving and changing world change which bars a player should see, so each is applied
 * straight away rather than at the next visibility check.
 */
public class PlayerListener implements Listener {

    private final RealBossbar rbb;

    public PlayerListener(final RealBossbar rbb) {
        this.rbb = rbb;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent e) {
        final Player p = e.getPlayer();
        //a tick later, once the client is ready to be sent bars
        Bukkit.getScheduler().runTask(this.rbb.getPlugin(), () -> {
            if (p.isOnline()) {
                this.rbb.getDisplayManager().refresh(p);
            }
        });

        final String newVersion = this.rbb.getPlugin().getNewVersion();
        if (newVersion != null && (p.isOp() || p.hasPermission("realbossbar.admin"))) {
            TranslatableLine.SYSTEM_NEW_UPDATE.with(VALUE, newVersion).with(NAME, RealBossbarPlugin.SPIGOT_URL).send(p);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(final PlayerQuitEvent e) {
        this.rbb.getDisplayManager().forget(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(final PlayerChangedWorldEvent e) {
        this.rbb.getDisplayManager().refresh(e.getPlayer());
    }
}
