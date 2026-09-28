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

import dev.dejvokep.boostedyaml.block.implementation.Section;
import joserodpt.realbossbar.api.bossbar.Audience;
import joserodpt.realbossbar.api.bossbar.ProgressAnimation;
import joserodpt.realbossbar.api.bossbar.RBossbar;
import joserodpt.realbossbar.api.config.RBBBossbars;
import joserodpt.realbossbar.api.config.RBBConfig;
import joserodpt.realbossbar.api.managers.BossbarManagerAPI;
import joserodpt.realbossbar.plugin.RealBossbar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class BossbarManager implements BossbarManagerAPI {

    private static final String ROOT = "Bossbars";

    private final RealBossbar rbb;
    /**
     * By lower-case name, in the order bossbars.yml lists them, then the order they were added.
     * PlaceholderAPI may read it off the main thread, so going through it is done holding its lock.
     */
    private final Map<String, RBossbar> bars = Collections.synchronizedMap(new LinkedHashMap<>());
    private int announcements = 0;

    public BossbarManager(final RealBossbar rbb) {
        this.rbb = rbb;
    }

    /**
     * Reads every bar in bossbars.yml. A saved bar held before is read into the same object, so
     * references other plugins and open screens hold stay live; saved bars no longer in the file are
     * dropped. Bars registered by other plugins, and announcements still running, are kept.
     */
    public void load() {
        final Map<String, RBossbar> before;
        synchronized (this.bars) {
            before = new LinkedHashMap<>(this.bars);
        }
        final Map<String, RBossbar> loaded = new LinkedHashMap<>();

        final Section root = RBBBossbars.file().getSection(ROOT);
        for (final Object key : root == null ? Collections.<Object>emptySet() : root.getKeys()) {
            final String name = String.valueOf(key);
            final Section section = root.getSection(name);
            if (section == null) {
                continue;
            }
            if (!RBossbar.isValidName(name)) {
                this.rbb.getLogger().warning("Skipped the bossbar '" + name + "': names can only have letters, numbers, - and _.");
                continue;
            }
            final RBossbar held = before.get(key(name));
            if (held != null && !held.isPersistent()) {
                this.rbb.getLogger().warning("Skipped the bossbar '" + name + "': another plugin registered one by that name.");
                continue;
            }
            final List<String> problems = new ArrayList<>();
            final RBossbar bar;
            if (held != null) {
                held.read(section, problems);
                bar = held;
            } else {
                bar = RBossbar.deserialize(name, section, problems);
            }
            problems.forEach(problem -> this.rbb.getLogger().warning("Bossbar '" + name + "': " + problem + "."));
            loaded.put(key(name), bar);
        }
        before.forEach((key, bar) -> {
            if (!bar.isPersistent()) {
                loaded.put(key, bar);
            }
        });

        synchronized (this.bars) {
            this.bars.clear();
            this.bars.putAll(loaded);
        }
    }

    private static String key(final String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    @Override
    public Collection<RBossbar> getBossbars() {
        synchronized (this.bars) {
            return Collections.unmodifiableList(new ArrayList<>(this.bars.values()));
        }
    }

    @Override
    public Collection<RBossbar> getPermanentBossbars() {
        return this.getBossbars().stream().filter(bar -> !bar.isTemporary()).collect(Collectors.toList());
    }

    @Override
    public RBossbar getBossbar(final String name) {
        return name == null ? null : this.bars.get(key(name));
    }

    @Override
    public RBossbar create(final String name, final String title) {
        if (!RBossbar.isValidName(name) || this.bars.containsKey(key(name))) {
            return null;
        }
        final RBossbar bar = new RBossbar(name, title, true);
        this.bars.put(key(name), bar);
        this.save(bar);
        return bar;
    }

    @Override
    public void register(final RBossbar bar) {
        final RBossbar held = this.bars.get(key(bar.getName()));
        if (held == bar) {
            return;
        }
        if (held != null) {
            //a saved bar, usually the copy bossbars.yml kept from the last time this one was registered
            if (!held.isPersistent() || !bar.isPersistent()) {
                throw new IllegalArgumentException("There's already a bossbar called " + bar.getName());
            }
            this.rbb.getDisplayManager().remove(held);
        }
        this.bars.put(key(bar.getName()), bar);
        if (bar.isPersistent()) {
            this.save(bar);
        } else {
            this.rbb.getDisplayManager().refreshAll();
        }
    }

    @Override
    public boolean delete(final String name) {
        final RBossbar bar = this.bars.remove(key(name));
        if (bar == null) {
            return false;
        }
        this.rbb.getDisplayManager().remove(bar);
        this.rbb.getPlayerManager().forget(bar);
        if (bar.isPersistent()) {
            RBBBossbars.file().remove(ROOT + "." + bar.getName());
            RBBBossbars.save();
        }
        return true;
    }

    @Override
    public void save(final RBossbar bar) {
        //a bar deleted while its editor was open would otherwise be written back
        if (bar.isPersistent() && this.bars.get(key(bar.getName())) == bar) {
            final String route = ROOT + "." + bar.getName();
            RBBBossbars.file().remove(route);
            bar.serialize(RBBBossbars.file().createSection(route));
            RBBBossbars.save();
        }
        //audience, worlds or enabled may have changed who should see it
        this.rbb.getDisplayManager().refreshAll();
        //and a new title shows now, not at the next frame or placeholder refresh
        this.rbb.getDisplayManager().redraw(bar);
    }

    @Override
    public RBossbar announce(final Audience audience, final String message, final BarColor color, final BarStyle style, final int seconds) {
        String name;
        do {
            name = "announcement-" + (++this.announcements);
        } while (this.bars.containsKey(name));

        final RBossbar bar = new RBossbar(name, message, false);
        bar.setColor(color == null ? configEnum(BarColor.class, "Announcements.Color", BarColor.YELLOW) : color);
        bar.setStyle(style == null ? configEnum(BarStyle.class, "Announcements.Style", BarStyle.SOLID) : style);
        bar.setAudience(audience);
        bar.setAnimation(ProgressAnimation.DECREASE, Math.max(1, seconds));
        bar.setLifetime(Math.max(1, seconds) * 20L);
        this.bars.put(name, bar);
        this.rbb.getDisplayManager().refreshAll();
        return bar;
    }

    /** Takes every bar that ran out off the screen, and forgets it. */
    public void removeExpired() {
        final List<RBossbar> expired = this.getBossbars().stream().filter(RBossbar::isExpired).collect(Collectors.toList());
        for (final RBossbar bar : expired) {
            this.delete(bar.getName());
        }
    }

    private static <E extends Enum<E>> E configEnum(final Class<E> type, final String route, final E fallback) {
        try {
            return Enum.valueOf(type, RBBConfig.file().getString(route, fallback.name()).trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException e) {
            return fallback;
        }
    }
}
