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

import joserodpt.realbossbar.api.bossbar.Audience;
import joserodpt.realbossbar.api.bossbar.RBossbar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

import java.util.Collection;

public interface BossbarManagerAPI {

    /** Every bar RealBossbar knows about: saved ones, those other plugins registered, and announcements. */
    Collection<RBossbar> getBossbars();

    /** Only the bars that stay, leaving out announcements and other temporary bars. */
    Collection<RBossbar> getPermanentBossbars();

    /** @return the bar by that name, ignoring case, or null */
    RBossbar getBossbar(String name);

    /**
     * Makes a new bar, shown to everyone in every world, and saves it to bossbars.yml. A bar saved
     * there is loaded again on the next start, before other plugins enable, so a plugin making its
     * bar on every start wants {@link #getOrCreate} instead.
     *
     * @return the bar, or null if the name is taken or not {@link RBossbar#isValidName valid}
     */
    RBossbar create(String name, String title);

    /**
     * The bar by that name if there is one, such as the one bossbars.yml kept from an earlier start,
     * otherwise a new one made and saved as by {@link #create}.
     *
     * @return the bar, or null only if the name is not {@link RBossbar#isValidName valid}
     */
    default RBossbar getOrCreate(final String name, final String title) {
        final RBossbar existing = this.getBossbar(name);
        return existing != null ? existing : this.create(name, title);
    }

    /**
     * Adds a bar made elsewhere, usually by another plugin. One that isn't
     * {@link RBossbar#isPersistent() persistent} is never written to bossbars.yml and lasts until the
     * server stops; a reload keeps it.
     *
     * <p>A persistent one takes the place of a persistent bar by the same name, such as the copy
     * bossbars.yml kept from the last time it was registered, and is saved over it. A reload then
     * reads bossbars.yml back into this same object, so the reference stays live.</p>
     *
     * @throws IllegalArgumentException if a bar by that name already exists that it can't replace:
     *                                  either of the two isn't persistent
     */
    void register(RBossbar bar);

    /** Takes the bar off every screen and forgets it, removing it from bossbars.yml if it was saved there. */
    boolean delete(String name);

    /** Writes a persistent bar's settings to bossbars.yml. Changes show in game whether saved or not. */
    void save(RBossbar bar);

    /**
     * Shows a temporary bar that empties over {@code seconds} and then removes itself. It keeps out of
     * the worlds under Announcements.Disabled-Worlds and RealBossbar.Disabled-Worlds in config.yml.
     *
     * @param color null for the colour set in config.yml
     * @param style null for the style set in config.yml
     */
    RBossbar announce(Audience audience, String message, BarColor color, BarStyle style, int seconds);
}
