package joserodpt.realbossbar.api.bossbar;

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
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * One bossbar: what it says, how it looks, how its progress moves, and who sees it where.
 *
 * <p>This is only the bar's settings and its animation clock. RealBossbar keeps a Bukkit bar per
 * viewer from it, since titles can hold placeholders that differ from player to player, and applies
 * every change on its next update: a setter here shows in game without anything else being called.
 * Changes to a saved bar are written to bossbars.yml by
 * {@link joserodpt.realbossbar.api.managers.BossbarManagerAPI#save(RBossbar)}.</p>
 *
 * <p>Only touch it from the main thread.</p>
 */
public class RBossbar {

    /** What a bar may be called: it is its key in bossbars.yml and an argument to the commands. */
    public static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_-]{1,32}");
    public static final String ALL_WORLDS = "*";

    private final String name;
    private final boolean persistent;
    private boolean enabled = true;
    private List<String> titles = new ArrayList<>();
    private int titleInterval = 40;
    private BarColor color = BarColor.PURPLE;
    private BarStyle style = BarStyle.SOLID;
    private double progress = 1D;
    private String progressPlaceholder = "";
    private ProgressAnimation animation = ProgressAnimation.STATIC;
    private int animationDuration = 10;
    private Audience audience = Audience.all();
    private List<String> worlds = new ArrayList<>(Collections.singletonList(ALL_WORLDS));
    private List<String> disabledWorlds = new ArrayList<>();
    /** Ticks until the bar removes itself, or 0 to stay. */
    private long lifetime = 0L;
    /** Ticks since the animation started, which the title frames count by too. */
    private long elapsed = 0L;

    /**
     * @param persistent whether it is saved to bossbars.yml. A bar another plugin makes for itself
     *                   is usually not, and then only lasts until the server stops.
     */
    public RBossbar(final String name, final String title, final boolean persistent) {
        this.name = name;
        this.persistent = persistent;
        if (title != null) {
            this.titles.add(title);
        }
    }

    public static boolean isValidName(final String name) {
        return name != null && VALID_NAME.matcher(name).matches();
    }

    /** Moves the animation and title frames on. Called by RealBossbar on every update. */
    public void tick(final int ticks) {
        this.elapsed += ticks;
    }

    /** Starts the animation, and the title frames, over from the beginning. */
    public void resetAnimation() {
        this.elapsed = 0L;
    }

    /** The progress the animation is at now, from 0 to 1. Per-player placeholders aren't read here. */
    public double getCurrentProgress() {
        return this.animation.progressAt(this.elapsed, this.animationDuration * 20L, this.progress);
    }

    /** Which title frame is showing, counting from 0. */
    public int getFrameIndex() {
        if (this.titles.isEmpty()) {
            return 0;
        }
        return (int) ((this.elapsed / Math.max(1, this.titleInterval)) % this.titles.size());
    }

    /** The title frame showing now, colour codes and placeholders still in it. */
    public String getCurrentTitle() {
        return this.titles.isEmpty() ? "" : this.titles.get(this.getFrameIndex());
    }

    /** Whether a bar with a lifetime has used it up, and is due to be removed. */
    public boolean isExpired() {
        return this.lifetime > 0 && this.elapsed >= this.lifetime;
    }

    /**
     * Whether the bar may show in this world: listed in its worlds, and not in its disabled ones. No
     * worlds at all reads as every world, the same as {@code *}.
     */
    public boolean isAllowedIn(final World world) {
        return this.isAllowedIn(world.getName());
    }

    public boolean isAllowedIn(final String world) {
        if (containsIgnoreCase(this.disabledWorlds, world)) {
            return false;
        }
        return this.isInAllWorlds() || containsIgnoreCase(this.worlds, world);
    }

    /** Whether its worlds are every world: {@code *}, or none listed at all. */
    private boolean isInAllWorlds() {
        return this.worlds.isEmpty() || this.worlds.contains(ALL_WORLDS);
    }

    /**
     * Lets the bar show in a world again, or keeps it out of one. Enabling a world the bar was never
     * listed for adds it to the list.
     */
    public void setWorldEnabled(final String world, final boolean enabled) {
        this.disabledWorlds.removeIf(w -> w.equalsIgnoreCase(world));
        if (enabled) {
            if (!this.isInAllWorlds() && !containsIgnoreCase(this.worlds, world)) {
                this.worlds.add(world);
            }
        } else {
            this.disabledWorlds.add(world);
        }
    }

    private static boolean containsIgnoreCase(final List<String> list, final String value) {
        for (final String entry : list) {
            if (entry.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    // --- bossbars.yml ---

    public void serialize(final Section section) {
        section.set("Enabled", this.enabled);
        section.set("Titles", new ArrayList<>(this.titles));
        section.set("Title-Interval", this.titleInterval);
        section.set("Color", this.color.name());
        section.set("Style", this.style.name());
        section.set("Progress", this.progress);
        section.set("Progress-Placeholder", this.progressPlaceholder);
        section.set("Animation", this.animation.name());
        section.set("Animation-Duration", this.animationDuration);
        section.set("Audience", this.audience.toString());
        section.set("Worlds", new ArrayList<>(this.worlds));
        section.set("Disabled-Worlds", new ArrayList<>(this.disabledWorlds));
    }

    /**
     * Reads a bar back from its section in bossbars.yml. A value that is missing or can't be read
     * keeps its default, and {@code problems} is told about the ones that couldn't be read.
     */
    public static RBossbar deserialize(final String name, final Section section, final List<String> problems) {
        final RBossbar bar = new RBossbar(name, null, true);
        bar.read(section, problems);
        return bar;
    }

    /**
     * Reads the bar's settings back from its section in bossbars.yml into this same object, as a
     * reload does, so a reference another plugin holds stays the bar that shows. What is missing
     * goes back to its default, and the animation starts over.
     */
    public void read(final Section section, final List<String> problems) {
        this.enabled = section.getBoolean("Enabled", true);
        this.titles = new ArrayList<>();
        if (section.isList("Titles")) {
            this.titles = new ArrayList<>(section.getStringList("Titles"));
        } else if (section.contains("Titles")) {
            this.titles.add(section.getString("Titles"));
        }
        this.titleInterval = Math.max(1, section.getInt("Title-Interval", 40));
        this.color = parseEnum(BarColor.class, section.getString("Color"), BarColor.PURPLE, "Color", problems);
        this.style = parseEnum(BarStyle.class, section.getString("Style"), BarStyle.SOLID, "Style", problems);
        this.progress = ProgressAnimation.clamp(section.getDouble("Progress", 1D));
        this.progressPlaceholder = section.getString("Progress-Placeholder", "");
        this.animation = parseEnum(ProgressAnimation.class, section.getString("Animation"), ProgressAnimation.STATIC, "Animation", problems);
        this.animationDuration = Math.max(1, section.getInt("Animation-Duration", 10));
        final Audience audience = Audience.parse(section.getString("Audience", "all"));
        if (audience == null) {
            problems.add("Audience '" + section.getString("Audience") + "' can't be read, so it shows to everyone");
        }
        this.audience = audience == null ? Audience.all() : audience;
        this.worlds = section.isList("Worlds")
                ? new ArrayList<>(section.getStringList("Worlds")) : new ArrayList<>(Collections.singletonList(ALL_WORLDS));
        this.disabledWorlds = section.isList("Disabled-Worlds")
                ? new ArrayList<>(section.getStringList("Disabled-Worlds")) : new ArrayList<>();
        this.resetAnimation();
    }

    private static <E extends Enum<E>> E parseEnum(final Class<E> type, final String value, final E fallback,
                                                   final String key, final List<String> problems) {
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException e) {
            problems.add(key + " '" + value + "' is not one of " + java.util.Arrays.toString(type.getEnumConstants())
                    + ", so it is " + fallback.name());
            return fallback;
        }
    }

    // --- getters and setters ---

    public String getName() {
        return this.name;
    }

    /** Whether it is saved to bossbars.yml, rather than living only until the server stops. */
    public boolean isPersistent() {
        return this.persistent;
    }

    /** Whether it is a temporary bar, such as an announcement, that removes itself. */
    public boolean isTemporary() {
        return this.lifetime > 0;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /** The title frames, cycled through every {@link #getTitleInterval()} ticks. */
    public List<String> getTitles() {
        return Collections.unmodifiableList(this.titles);
    }

    public void setTitles(final List<String> titles) {
        this.titles = new ArrayList<>(titles);
    }

    /** Replaces every frame with this one title. */
    public void setTitle(final String title) {
        this.titles = new ArrayList<>(Collections.singletonList(title));
    }

    public int getTitleInterval() {
        return this.titleInterval;
    }

    public void setTitleInterval(final int ticks) {
        this.titleInterval = Math.max(1, ticks);
    }

    public BarColor getColor() {
        return this.color;
    }

    public void setColor(final BarColor color) {
        this.color = color;
    }

    public BarStyle getStyle() {
        return this.style;
    }

    public void setStyle(final BarStyle style) {
        this.style = style;
    }

    /** What a {@link ProgressAnimation#STATIC} bar shows, from 0 to 1. */
    public double getProgress() {
        return this.progress;
    }

    /** Sets what a {@link ProgressAnimation#STATIC} bar shows. Other animations keep moving as they were. */
    public void setProgress(final double progress) {
        this.progress = ProgressAnimation.clamp(progress);
    }

    /**
     * A placeholder the progress is read from for each player instead of the animation, such as
     * {@code %player_health%/20}, or empty for none. Needs PlaceholderAPI.
     */
    public String getProgressPlaceholder() {
        return this.progressPlaceholder;
    }

    public void setProgressPlaceholder(final String placeholder) {
        this.progressPlaceholder = placeholder == null ? "" : placeholder.trim();
    }

    public boolean hasProgressPlaceholder() {
        return !this.progressPlaceholder.isEmpty();
    }

    public ProgressAnimation getAnimation() {
        return this.animation;
    }

    /** Seconds one run of the animation takes. */
    public int getAnimationDuration() {
        return this.animationDuration;
    }

    /** Changes the animation and starts it over. */
    public void setAnimation(final ProgressAnimation animation, final int durationSeconds) {
        this.animation = animation;
        this.animationDuration = Math.max(1, durationSeconds);
        this.resetAnimation();
    }

    public void setAnimation(final ProgressAnimation animation) {
        this.setAnimation(animation, this.animationDuration);
    }

    public Audience getAudience() {
        return this.audience;
    }

    public void setAudience(final Audience audience) {
        this.audience = audience == null ? Audience.all() : audience;
    }

    /** The worlds it shows in, {@code *} (or no world at all) meaning all of them. */
    public List<String> getWorlds() {
        return Collections.unmodifiableList(this.worlds);
    }

    public void setWorlds(final List<String> worlds) {
        this.worlds = new ArrayList<>(worlds);
    }

    /** The worlds it never shows in, even where {@link #getWorlds()} would allow it. */
    public List<String> getDisabledWorlds() {
        return Collections.unmodifiableList(this.disabledWorlds);
    }

    public void setDisabledWorlds(final List<String> worlds) {
        this.disabledWorlds = new ArrayList<>(worlds);
    }

    /** Ticks the bar lasts before removing itself, or 0 when it stays. */
    public long getLifetime() {
        return this.lifetime;
    }

    public void setLifetime(final long ticks) {
        this.lifetime = Math.max(0L, ticks);
    }
}
