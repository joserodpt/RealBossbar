package joserodpt.realbossbar.api.managers;

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
import org.bukkit.entity.Player;

/**
 * What each player chose to see, and what was shown to them by hand. None of it is saved: it lasts
 * until the server stops.
 */
public interface PlayerManagerAPI {

    /**
     * Shows a bar to a player whether its audience includes them or not. The bar's worlds still
     * apply, and it stays hidden while it is disabled.
     */
    void show(Player p, RBossbar bar);

    /** Keeps a bar off a player's screen, whether its audience includes them or not. */
    void hide(Player p, RBossbar bar);

    /**
     * Hides every bar from a player, or shows them again.
     *
     * @return true if the bars now show
     */
    boolean toggle(Player p);

    /**
     * Hides one bar from a player, or shows it again.
     *
     * @return true if the bar now shows
     */
    boolean toggle(Player p, RBossbar bar);

    /** Whether the player has every bar hidden. */
    boolean isHidingAll(Player p);

    /** Whether the bar is on the player's screen right now. */
    boolean isVisible(Player p, RBossbar bar);

    /** Works out again which bars the player should see, rather than waiting for the next check. */
    void refresh(Player p);
}
