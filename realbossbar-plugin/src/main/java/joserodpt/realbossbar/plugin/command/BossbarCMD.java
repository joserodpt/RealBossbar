package joserodpt.realbossbar.plugin.command;

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
import joserodpt.realbossbar.api.bossbar.ProgressAnimation;
import joserodpt.realbossbar.api.bossbar.RBossbar;
import joserodpt.realbossbar.api.config.TranslatableLine;
import joserodpt.realbossbar.plugin.BarText;
import joserodpt.realbossbar.plugin.RealBossbar;
import joserodpt.realutils.BuildInfo;
import joserodpt.realutils.text.Text;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.CommandPlaceholder;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Single;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.bukkit.parameters.EntitySelector;

import java.util.Collection;
import java.util.List;

import static joserodpt.realbossbar.api.config.TranslatableLine.TranslatableLinePlaceholder.INFO;
import static joserodpt.realbossbar.api.config.TranslatableLine.TranslatableLinePlaceholder.NAME;
import static joserodpt.realbossbar.api.config.TranslatableLine.TranslatableLinePlaceholder.VALUE;
import static joserodpt.realbossbar.api.config.TranslatableLine.TranslatableLinePlaceholder.WORLD;

/**
 * Every {@code /rbb} subcommand. Those that only make sense in game take a {@link Player} rather than
 * a {@link CommandSender}, which is what tells the console it can't run them. {@code /bossbar} is left
 * to vanilla.
 */
@Command({"realbossbar", "rbb"})
public class BossbarCMD {

    private static final String ADMIN = "realbossbar.admin";

    private final RealBossbar rbb;

    public BossbarCMD(final RealBossbar rbb) {
        this.rbb = rbb;
    }

    /** The bar by that name, or null after telling the sender there's none. */
    private RBossbar find(final CommandSender s, final String name) {
        final RBossbar bar = this.rbb.getBossbarManager().getBossbar(name);
        if (bar == null) {
            TranslatableLine.BAR_NOT_FOUND.with(NAME, name).send(s);
        }
        return bar;
    }

    private void updated(final CommandSender s, final RBossbar bar, final String what) {
        this.rbb.getBossbarManager().save(bar);
        TranslatableLine.BAR_UPDATED.with(NAME, bar.getName()).with(VALUE, what).send(s);
    }

    @CommandPlaceholder
    @SuppressWarnings("unused")
    public void about(final CommandSender s) {
        BuildInfo.sendAbout(s, this.rbb.getPlugin(), "&fReal&5Bossbar");
    }

    @Subcommand("reload")
    @CommandPermission(ADMIN)
    @SuppressWarnings("unused")
    public void reload(final CommandSender s) {
        this.rbb.reload();
        TranslatableLine.SYSTEM_RELOADED.send(s);
    }

    @Subcommand("list")
    @CommandPermission(ADMIN)
    @SuppressWarnings("unused")
    public void list(final CommandSender s) {
        final Collection<RBossbar> bars = this.rbb.getBossbarManager().getPermanentBossbars();
        if (bars.isEmpty()) {
            TranslatableLine.BAR_LIST_EMPTY.send(s);
            return;
        }
        TranslatableLine.BAR_LIST_HEADER.with(VALUE, bars.size()).send(s);
        for (final RBossbar bar : bars) {
            Text.sendRaw(s, TranslatableLine.BAR_LIST_ENTRY.with(NAME, bar.getName()).with(VALUE, BarText.status(bar))
                    .with(INFO, BarText.describe(bar)).get());
        }
    }

    @Subcommand("edit")
    @CommandPermission(ADMIN)
    @SuppressWarnings("unused")
    public void edit(final Player p, @Optional @Single @SuggestFrom(RBBSuggestion.BOSSBARS) final String bar) {
        if (bar == null) {
            this.rbb.getEditor().openList(p, 0);
            return;
        }
        final RBossbar found = this.find(p, bar);
        if (found != null) {
            this.rbb.getEditor().openBar(p, found, 0);
        }
    }

    @Subcommand("create")
    @CommandPermission(ADMIN)
    @SuppressWarnings("unused")
    public void create(final CommandSender s, @Single final String name, @Optional final String title) {
        if (!RBossbar.isValidName(name)) {
            TranslatableLine.BAR_INVALID_NAME.send(s);
            return;
        }
        if (this.rbb.getBossbarManager().getBossbar(name) != null) {
            TranslatableLine.BAR_ALREADY_EXISTS.with(NAME, name).send(s);
            return;
        }
        final List<String> frames = title == null ? null : BarText.splitFrames(title);
        //only bars, such as "|", would leave it with no title at all
        if (frames != null && frames.isEmpty()) {
            TranslatableLine.SYSTEM_ERROR_USAGE.send(s);
            return;
        }
        final RBossbar bar = this.rbb.getBossbarManager().create(name, "&f" + name);
        if (frames != null) {
            bar.setTitles(frames);
            this.rbb.getBossbarManager().save(bar);
        }
        TranslatableLine.BAR_CREATED.with(NAME, bar.getName()).send(s);
    }

    @Subcommand("delete")
    @CommandPermission(ADMIN)
    @SuppressWarnings("unused")
    public void delete(final CommandSender s, @Single @SuggestFrom(RBBSuggestion.BOSSBARS) final String bar) {
        final RBossbar found = this.find(s, bar);
        if (found != null && this.rbb.getBossbarManager().delete(found.getName())) {
            TranslatableLine.BAR_DELETED.with(NAME, found.getName()).send(s);
        }
    }

    @Subcommand("title")
    @CommandPermission(ADMIN)
    @SuppressWarnings("unused")
    public void title(final CommandSender s, @SuggestFrom(RBBSuggestion.BOSSBARS) final String bar, final String title) {
        final RBossbar found = this.find(s, bar);
        if (found == null) {
            return;
        }
        final List<String> frames = BarText.splitFrames(title);
        if (frames.isEmpty()) {
            TranslatableLine.SYSTEM_ERROR_USAGE.send(s);
            return;
        }
        found.setTitles(frames);
        this.updated(s, found, "title &r" + BarText.joinFrames(frames));
    }

    @Subcommand("color")
    @CommandPermission(ADMIN)
    @SuppressWarnings("unused")
    public void color(final CommandSender s, @SuggestFrom(RBBSuggestion.BOSSBARS) final String bar, final BarColor color) {
        final RBossbar found = this.find(s, bar);
        if (found != null) {
            found.setColor(color);
            this.updated(s, found, "colour " + BarText.color(color) + color.name());
        }
    }

    @Subcommand("style")
    @CommandPermission(ADMIN)
    @SuppressWarnings("unused")
    public void style(final CommandSender s, @SuggestFrom(RBBSuggestion.BOSSBARS) final String bar, final BarStyle style) {
        final RBossbar found = this.find(s, bar);
        if (found != null) {
            found.setStyle(style);
            this.updated(s, found, "style &b" + style.name());
        }
    }

    /** Sets the progress by hand, which stops the animation and any progress placeholder. */
    @Subcommand("progress")
    @CommandPermission(ADMIN)
    @SuppressWarnings("unused")
    public void progress(final CommandSender s, @SuggestFrom(RBBSuggestion.BOSSBARS) final String bar, final double percent) {
        if (percent < 0 || percent > 100) {
            TranslatableLine.BAR_INVALID_PROGRESS.send(s);
            return;
        }
        final RBossbar found = this.find(s, bar);
        if (found != null) {
            found.setProgress(percent / 100D);
            found.setProgressPlaceholder("");
            found.setAnimation(ProgressAnimation.STATIC);
            this.updated(s, found, "progress &b" + Text.formatNumber(percent) + "%");
        }
    }

    @Subcommand("animation")
    @CommandPermission(ADMIN)
    @SuppressWarnings("unused")
    public void animation(final CommandSender s, @SuggestFrom(RBBSuggestion.BOSSBARS) final String bar,
                          final ProgressAnimation animation, @Optional final Integer seconds) {
        final RBossbar found = this.find(s, bar);
        if (found != null) {
            found.setAnimation(animation, seconds == null ? found.getAnimationDuration() : seconds);
            this.updated(s, found, "animation &b" + animation.name() + " &f(" + found.getAnimationDuration() + "s)");
        }
    }

    @Subcommand("restart")
    @CommandPermission(ADMIN)
    @SuppressWarnings("unused")
    public void restart(final CommandSender s, @Single @SuggestFrom(RBBSuggestion.BOSSBARS) final String bar) {
        final RBossbar found = this.find(s, bar);
        if (found != null) {
            found.resetAnimation();
            TranslatableLine.BAR_RESET.with(NAME, found.getName()).send(s);
        }
    }

    @Subcommand("audience")
    @CommandPermission(ADMIN)
    @SuppressWarnings("unused")
    public void audience(final CommandSender s, @SuggestFrom(RBBSuggestion.BOSSBARS) final String bar,
                         @Single @SuggestFrom(RBBSuggestion.AUDIENCES) final String audience) {
        final RBossbar found = this.find(s, bar);
        if (found == null) {
            return;
        }
        final Audience parsed = Audience.parse(audience);
        if (parsed == null) {
            TranslatableLine.BAR_INVALID_AUDIENCE.with(VALUE, audience).send(s);
            return;
        }
        found.setAudience(parsed);
        this.updated(s, found, "audience &b" + parsed);
    }

    @Subcommand("world enable")
    @CommandPermission(ADMIN)
    @SuppressWarnings("unused")
    public void worldEnable(final CommandSender s, @SuggestFrom(RBBSuggestion.BOSSBARS) final String bar,
                            @Single @SuggestFrom(RBBSuggestion.WORLDS) final String world) {
        this.setWorld(s, bar, world, true);
    }

    @Subcommand("world disable")
    @CommandPermission(ADMIN)
    @SuppressWarnings("unused")
    public void worldDisable(final CommandSender s, @SuggestFrom(RBBSuggestion.BOSSBARS) final String bar,
                             @Single @SuggestFrom(RBBSuggestion.WORLDS) final String world) {
        this.setWorld(s, bar, world, false);
    }

    private void setWorld(final CommandSender s, final String bar, final String world, final boolean enabled) {
        final RBossbar found = this.find(s, bar);
        if (found == null) {
            return;
        }
        found.setWorldEnabled(world, enabled);
        this.rbb.getBossbarManager().save(found);
        (enabled ? TranslatableLine.BAR_WORLD_ENABLED : TranslatableLine.BAR_WORLD_DISABLED)
                .with(NAME, found.getName()).with(WORLD, world).send(s);
    }

    @Subcommand("enable")
    @CommandPermission(ADMIN)
    @SuppressWarnings("unused")
    public void enable(final CommandSender s, @Single @SuggestFrom(RBBSuggestion.BOSSBARS) final String bar) {
        this.setEnabled(s, bar, true);
    }

    @Subcommand("disable")
    @CommandPermission(ADMIN)
    @SuppressWarnings("unused")
    public void disable(final CommandSender s, @Single @SuggestFrom(RBBSuggestion.BOSSBARS) final String bar) {
        this.setEnabled(s, bar, false);
    }

    private void setEnabled(final CommandSender s, final String bar, final boolean enabled) {
        final RBossbar found = this.find(s, bar);
        if (found == null) {
            return;
        }
        found.setEnabled(enabled);
        this.rbb.getBossbarManager().save(found);
        (enabled ? TranslatableLine.BAR_ENABLED : TranslatableLine.BAR_DISABLED).with(NAME, found.getName()).send(s);
    }

    /** Shows a bar to players outside its audience, until the server stops. */
    @Subcommand("show")
    @CommandPermission(ADMIN)
    @SuppressWarnings("unused")
    public void show(final CommandSender s, @SuggestFrom(RBBSuggestion.BOSSBARS) final String bar, final EntitySelector<Player> targets) {
        final RBossbar found = this.find(s, bar);
        if (found != null) {
            targets.forEach(p -> this.rbb.getPlayerManager().show(p, found));
            TranslatableLine.BAR_SHOWN.with(NAME, found.getName()).with(VALUE, targets.size()).send(s);
        }
    }

    @Subcommand("hide")
    @CommandPermission(ADMIN)
    @SuppressWarnings("unused")
    public void hide(final CommandSender s, @SuggestFrom(RBBSuggestion.BOSSBARS) final String bar, final EntitySelector<Player> targets) {
        final RBossbar found = this.find(s, bar);
        if (found != null) {
            targets.forEach(p -> this.rbb.getPlayerManager().hide(p, found));
            TranslatableLine.BAR_HIDDEN.with(NAME, found.getName()).with(VALUE, targets.size()).send(s);
        }
    }

    @Subcommand("announce")
    @CommandPermission("realbossbar.announce")
    @SuppressWarnings("unused")
    public void announce(final CommandSender s, @Single @SuggestFrom(RBBSuggestion.AUDIENCES) final String audience,
                         final int seconds, final String message) {
        final Audience parsed = Audience.parse(audience);
        if (parsed == null) {
            TranslatableLine.BAR_INVALID_AUDIENCE.with(VALUE, audience).send(s);
            return;
        }
        final RBossbar bar = this.rbb.getBossbarManager().announce(parsed, message, null, null, seconds);
        TranslatableLine.BAR_ANNOUNCED.with(VALUE, this.rbb.getDisplayManager().countViewers(bar)).send(s);
    }

    /** Hides every bar, or one, from the player running it, and shows them again the next time. */
    @Subcommand("toggle")
    @CommandPermission("realbossbar.toggle")
    @SuppressWarnings("unused")
    public void toggle(final Player p, @Optional @Single @SuggestFrom(RBBSuggestion.BOSSBARS) final String bar) {
        if (bar == null) {
            final boolean shown = this.rbb.getPlayerManager().toggle(p);
            (shown ? TranslatableLine.BAR_TOGGLE_ON : TranslatableLine.BAR_TOGGLE_OFF).send(p);
            return;
        }
        final RBossbar found = this.find(p, bar);
        if (found != null) {
            final boolean shown = this.rbb.getPlayerManager().toggle(p, found);
            if (!shown) {
                TranslatableLine.BAR_TOGGLE_BAR_OFF.with(NAME, found.getName()).send(p);
            } else if (this.rbb.getPlayerManager().isHidingAll(p)) {
                //no longer hidden by itself, but every bar still is
                TranslatableLine.BAR_TOGGLE_BAR_HIDING_ALL.with(NAME, found.getName()).send(p);
            } else {
                TranslatableLine.BAR_TOGGLE_BAR_ON.with(NAME, found.getName()).send(p);
            }
        }
    }
}
