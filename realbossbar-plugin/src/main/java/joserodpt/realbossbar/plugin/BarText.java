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

import joserodpt.realbossbar.api.bossbar.ProgressAnimation;
import joserodpt.realbossbar.api.bossbar.RBossbar;
import joserodpt.realbossbar.api.config.TranslatableLine;
import joserodpt.realutils.text.Text;
import org.bukkit.boss.BarColor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** How a bar is described in chat, the editor and its inventory. */
public final class BarText {

    private BarText() {
    }

    /** The green or red dot before a bar's name. */
    public static String status(final RBossbar bar) {
        return (bar.isEnabled() ? TranslatableLine.BAR_STATUS_ON : TranslatableLine.BAR_STATUS_OFF).get();
    }

    /** A line of its settings: {@code Purple · Solid · Bounce 6s · all}. */
    public static String describe(final RBossbar bar) {
        final String animation = Text.beautifyEnumName(bar.getAnimation().name())
                + (bar.getAnimation() == ProgressAnimation.STATIC
                ? " " + Math.round(bar.getProgress() * 100) + "%" : " " + bar.getAnimationDuration() + "s");
        return color(bar.getColor()) + Text.beautifyEnumName(bar.getColor().name()) + " &7· "
                + Text.beautifyEnumName(bar.getStyle().name()) + " &7· " + animation + " &7· " + bar.getAudience();
    }

    /** The chat colour closest to a bar colour. */
    public static String color(final BarColor color) {
        switch (color) {
            case PINK:
                return "&d";
            case BLUE:
                return "&9";
            case RED:
                return "&c";
            case GREEN:
                return "&a";
            case YELLOW:
                return "&e";
            case PURPLE:
                return "&5";
            case WHITE:
            default:
                return "&f";
        }
    }

    /** Title frames as they are typed: split by {@code |}. */
    public static String joinFrames(final List<String> frames) {
        return String.join(" | ", frames);
    }

    public static List<String> splitFrames(final String typed) {
        return Arrays.stream(typed.split("\\|"))
                .map(String::trim)
                .filter(frame -> !frame.isEmpty())
                .collect(Collectors.toList());
    }

    /** A comma separated list of worlds, as typed. */
    public static List<String> splitList(final String typed) {
        return Arrays.stream(typed.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .collect(Collectors.toList());
    }
}
