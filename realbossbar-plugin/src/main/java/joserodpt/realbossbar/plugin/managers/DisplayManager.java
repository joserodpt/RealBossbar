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

import joserodpt.realbossbar.api.bossbar.ProgressAnimation;
import joserodpt.realbossbar.api.bossbar.RBossbar;
import joserodpt.realbossbar.api.config.RBBConfig;
import joserodpt.realbossbar.api.event.BossbarHideEvent;
import joserodpt.realbossbar.api.event.BossbarShowEvent;
import joserodpt.realbossbar.plugin.Placeholders;
import joserodpt.realbossbar.plugin.RealBossbar;
import joserodpt.realutils.text.Text;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Puts the bars on players' screens and keeps them moving.
 *
 * <p>Every viewer gets a Bukkit bar of their own, since a title's placeholders can read differently
 * for each player. One task on the main thread moves every animation on, and applies whatever changed
 * since: titles, progress, colour and style. PlaceholderAPI expects the main thread, which is why this
 * isn't done asynchronously.</p>
 */
public class DisplayManager {

    private final RealBossbar rbb;
    /** Who sees each bar, and the Bukkit bar they see it through. */
    private final Map<RBossbar, Map<UUID, BossBar>> shown = new HashMap<>();
    /** The title frame each bar was showing at the last update, so a new frame is sent straight away. */
    private final Map<RBossbar, Integer> lastFrame = new HashMap<>();
    private BukkitTask task;
    private long ticks = 0L;

    public DisplayManager(final RealBossbar rbb) {
        this.rbb = rbb;
    }

    /** Starts the update task, or starts it over with the intervals in config.yml. */
    public void start() {
        this.stop();
        final int interval = Math.max(1, RBBConfig.file().getInt("RealBossbar.Update-Interval", 2));
        this.task = Bukkit.getScheduler().runTaskTimer(this.rbb.getPlugin(), () -> this.update(interval), interval, interval);
    }

    public void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    private void update(final int interval) {
        this.ticks += interval;
        final BossbarManager bars = this.rbb.getBossbarManager();

        for (final RBossbar bar : bars.getBossbars()) {
            bar.tick(interval);
        }
        bars.removeExpired();

        if (this.due(RBBConfig.file().getInt("RealBossbar.Visibility-Check", 20), interval)) {
            this.refreshAll();
        }

        final boolean placeholdersDue = this.due(RBBConfig.file().getInt("RealBossbar.Placeholder-Refresh", 20), interval);
        for (final Map.Entry<RBossbar, Map<UUID, BossBar>> entry : this.shown.entrySet()) {
            final RBossbar bar = entry.getKey();
            final int frame = bar.getFrameIndex();
            final Integer previous = this.lastFrame.put(bar, frame);
            final boolean titleDue = placeholdersDue || previous == null || previous != frame;

            for (final Map.Entry<UUID, BossBar> viewer : entry.getValue().entrySet()) {
                final Player p = Bukkit.getPlayer(viewer.getKey());
                if (p != null) {
                    this.apply(bar, p, viewer.getValue(), titleDue);
                }
            }
        }
    }

    /** Whether a task every {@code period} ticks would have run during the last {@code interval}. */
    private boolean due(final int period, final int interval) {
        return period <= interval || this.ticks % period < interval;
    }

    private void apply(final RBossbar bar, final Player p, final BossBar bossBar, final boolean titleDue) {
        if (titleDue) {
            final String title = this.title(bar, p);
            if (!title.equals(bossBar.getTitle())) {
                bossBar.setTitle(title);
            }
        }
        final double progress = this.progress(bar, p);
        if (Math.abs(progress - bossBar.getProgress()) > 1.0E-4) {
            bossBar.setProgress(progress);
        }
        if (bossBar.getColor() != bar.getColor()) {
            bossBar.setColor(bar.getColor());
        }
        if (bossBar.getStyle() != bar.getStyle()) {
            bossBar.setStyle(bar.getStyle());
        }
    }

    private String title(final RBossbar bar, final Player p) {
        return Text.color(Placeholders.apply(p, bar.getCurrentTitle()));
    }

    private double progress(final RBossbar bar, final Player p) {
        if (bar.hasProgressPlaceholder() && Placeholders.isEnabled()) {
            final Double value = parseProgress(Placeholders.apply(p, bar.getProgressPlaceholder()));
            if (value != null) {
                return value;
            }
        }
        return bar.getCurrentProgress();
    }

    /**
     * Reads a filled-in progress placeholder: {@code 15/20} as a fraction, or a number, which is taken
     * as a percentage when it is over 1.
     *
     * @return null when it isn't a number, so the bar falls back to its animation
     */
    static Double parseProgress(final String text) {
        if (text == null) {
            return null;
        }
        final String value = Text.strip(text).replace(",", ".").replace("%", "").trim();
        try {
            final int slash = value.indexOf('/');
            if (slash >= 0) {
                final double max = Double.parseDouble(value.substring(slash + 1).trim());
                return max == 0 ? 0D : ProgressAnimation.clamp(Double.parseDouble(value.substring(0, slash).trim()) / max);
            }
            final double number = Double.parseDouble(value);
            return ProgressAnimation.clamp(number > 1D ? number / 100D : number);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    // --- who sees what ---

    public void refreshAll() {
        for (final Player p : Bukkit.getOnlinePlayers()) {
            this.refresh(p);
        }
    }

    /** Shows the player every bar they should see now, and takes away those they shouldn't. */
    public void refresh(final Player p) {
        for (final RBossbar bar : this.rbb.getBossbarManager().getBossbars()) {
            final boolean should = this.shouldSee(p, bar);
            final boolean showing = this.isShowing(p, bar);
            if (should && !showing) {
                this.show(p, bar);
            } else if (!should && showing) {
                this.hide(p, bar);
            }
        }
    }

    private boolean shouldSee(final Player p, final RBossbar bar) {
        if (!bar.isEnabled()) {
            return false;
        }
        final String world = p.getWorld().getName();
        final String disabledRoute = bar.isTemporary() ? "Announcements.Disabled-Worlds" : "RealBossbar.Disabled-Worlds";
        for (final String disabled : RBBConfig.file().getStringList(disabledRoute)) {
            if (disabled.equalsIgnoreCase(world)) {
                return false;
            }
        }
        if (!bar.isAllowedIn(world)) {
            return false;
        }
        final PlayerManager players = this.rbb.getPlayerManager();
        if (players.isHidden(p, bar)) {
            return false;
        }
        return players.isForced(p, bar) || bar.getAudience().matches(p);
    }

    public boolean isShowing(final Player p, final RBossbar bar) {
        final Map<UUID, BossBar> viewers = this.shown.get(bar);
        return viewers != null && viewers.containsKey(p.getUniqueId());
    }

    /** How many players see the bar right now. */
    public int countViewers(final RBossbar bar) {
        final Map<UUID, BossBar> viewers = this.shown.get(bar);
        return viewers == null ? 0 : viewers.size();
    }

    private void show(final Player p, final RBossbar bar) {
        final BossbarShowEvent event = new BossbarShowEvent(p, bar);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        final BossBar bossBar = Bukkit.createBossBar(this.title(bar, p), bar.getColor(), bar.getStyle());
        bossBar.setProgress(this.progress(bar, p));
        bossBar.addPlayer(p);
        this.shown.computeIfAbsent(bar, b -> new HashMap<>()).put(p.getUniqueId(), bossBar);
    }

    private void hide(final Player p, final RBossbar bar) {
        final Map<UUID, BossBar> viewers = this.shown.get(bar);
        if (viewers == null) {
            return;
        }
        final BossBar bossBar = viewers.remove(p.getUniqueId());
        if (viewers.isEmpty()) {
            this.shown.remove(bar);
            this.lastFrame.remove(bar);
        }
        if (bossBar != null) {
            bossBar.removeAll();
            Bukkit.getPluginManager().callEvent(new BossbarHideEvent(p, bar));
        }
    }

    /** Takes every bar off a player leaving the server. */
    public void forget(final Player p) {
        for (final RBossbar bar : new ArrayList<>(this.shown.keySet())) {
            this.hide(p, bar);
        }
    }

    /** Takes a bar off every screen, as when it is deleted. */
    public void remove(final RBossbar bar) {
        final Map<UUID, BossBar> viewers = this.shown.remove(bar);
        this.lastFrame.remove(bar);
        if (viewers == null) {
            return;
        }
        for (final Map.Entry<UUID, BossBar> viewer : viewers.entrySet()) {
            viewer.getValue().removeAll();
            final Player p = Bukkit.getPlayer(viewer.getKey());
            if (p != null) {
                Bukkit.getPluginManager().callEvent(new BossbarHideEvent(p, bar));
            }
        }
    }

    /** Takes every bar off every screen, before a reload or when the plugin stops. */
    public void hideAll() {
        final List<RBossbar> bars = new ArrayList<>(this.shown.keySet());
        bars.forEach(this::remove);
    }
}
