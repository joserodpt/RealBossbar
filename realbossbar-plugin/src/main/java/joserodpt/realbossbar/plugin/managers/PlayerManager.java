package joserodpt.realbossbar.plugin.managers;

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
import joserodpt.realbossbar.api.managers.PlayerManagerAPI;
import joserodpt.realbossbar.plugin.RealBossbar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What each player hid, and what was shown to them by hand. Kept until the server stops, so it
 * survives leaving and joining again, but not a restart.
 */
public class PlayerManager implements PlayerManagerAPI {

    private final RealBossbar rbb;
    private final Set<UUID> hidingAll = new HashSet<>();
    /** Bar names, in lower case, each player hid or had hidden from them. */
    private final Map<UUID, Set<String>> hidden = new HashMap<>();
    /** Bar names, in lower case, shown to each player outside the bar's audience. */
    private final Map<UUID, Set<String>> forced = new HashMap<>();

    public PlayerManager(final RealBossbar rbb) {
        this.rbb = rbb;
    }

    private static String key(final RBossbar bar) {
        return bar.getName().toLowerCase(Locale.ROOT);
    }

    private static Set<String> of(final Map<UUID, Set<String>> map, final Player p) {
        return map.computeIfAbsent(p.getUniqueId(), uuid -> new HashSet<>());
    }

    @Override
    public void show(final Player p, final RBossbar bar) {
        of(this.hidden, p).remove(key(bar));
        of(this.forced, p).add(key(bar));
        this.refresh(p);
    }

    @Override
    public void hide(final Player p, final RBossbar bar) {
        of(this.forced, p).remove(key(bar));
        of(this.hidden, p).add(key(bar));
        this.refresh(p);
    }

    @Override
    public boolean toggle(final Player p) {
        final boolean nowShown = !this.hidingAll.add(p.getUniqueId());
        if (nowShown) {
            this.hidingAll.remove(p.getUniqueId());
        }
        this.refresh(p);
        return nowShown;
    }

    @Override
    public boolean toggle(final Player p, final RBossbar bar) {
        final Set<String> hiddenBars = of(this.hidden, p);
        final boolean nowShown = hiddenBars.remove(key(bar));
        if (!nowShown) {
            hiddenBars.add(key(bar));
        }
        this.refresh(p);
        return nowShown;
    }

    @Override
    public boolean isHidingAll(final Player p) {
        return this.hidingAll.contains(p.getUniqueId());
    }

    /** Whether the player hid this bar, or every bar, or had it hidden from them. */
    public boolean isHidden(final Player p, final RBossbar bar) {
        if (this.hidingAll.contains(p.getUniqueId())) {
            return true;
        }
        final Set<String> bars = this.hidden.get(p.getUniqueId());
        return bars != null && bars.contains(key(bar));
    }

    /** Whether the bar was shown to the player by hand, outside its audience. */
    public boolean isForced(final Player p, final RBossbar bar) {
        final Set<String> bars = this.forced.get(p.getUniqueId());
        return bars != null && bars.contains(key(bar));
    }

    @Override
    public boolean isVisible(final Player p, final RBossbar bar) {
        return this.rbb.getDisplayManager().isShowing(p, bar);
    }

    @Override
    public void refresh(final Player p) {
        this.rbb.getDisplayManager().refresh(p);
    }

    /** Drops a deleted bar from every player's choices, so a new bar by the same name starts clean. */
    public void forget(final RBossbar bar) {
        final String key = key(bar);
        this.hidden.values().forEach(bars -> bars.remove(key));
        this.forced.values().forEach(bars -> bars.remove(key));
    }
}
