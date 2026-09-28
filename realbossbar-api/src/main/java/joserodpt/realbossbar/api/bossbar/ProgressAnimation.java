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

/**
 * How a bar's progress moves over its animation duration.
 */
public enum ProgressAnimation {
    /** Stays at the progress it was given. */
    STATIC,
    /** Empties once, full to empty, then stays empty: a countdown. */
    DECREASE,
    /** Fills once, empty to full, then stays full. */
    INCREASE,
    /** Empties from full over and over, starting full again each time. */
    LOOP,
    /** Fills and empties, back and forth. */
    BOUNCE;

    /**
     * The progress {@code elapsed} ticks into the animation.
     *
     * @param base     what {@link #STATIC} shows
     * @param duration ticks for one run; at most one tick is used for anything shorter
     * @return a value from 0 to 1
     */
    public double progressAt(final long elapsed, final long duration, final double base) {
        final double length = Math.max(1L, duration);
        switch (this) {
            case DECREASE:
                return clamp(1D - elapsed / length);
            case INCREASE:
                return clamp(elapsed / length);
            case LOOP:
                return clamp(1D - (elapsed % (long) length) / length);
            case BOUNCE:
                final double phase = (elapsed % (long) (length * 2)) / length;
                return clamp(phase <= 1D ? phase : 2D - phase);
            case STATIC:
            default:
                return clamp(base);
        }
    }

    public static double clamp(final double progress) {
        if (Double.isNaN(progress)) {
            return 0D;
        }
        return Math.max(0D, Math.min(1D, progress));
    }
}
