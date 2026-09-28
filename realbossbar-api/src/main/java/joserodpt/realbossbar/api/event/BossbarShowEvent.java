package joserodpt.realbossbar.api.event;

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
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a bar is about to appear on a player's screen. Cancelling it keeps the bar off for now;
 * RealBossbar asks again on its next visibility check, so a plugin that wants a bar gone for good
 * keeps cancelling it, or uses {@link joserodpt.realbossbar.api.managers.PlayerManagerAPI#hide}.
 */
public class BossbarShowEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final RBossbar bossbar;
    private boolean cancelled;

    public BossbarShowEvent(final Player player, final RBossbar bossbar) {
        this.player = player;
        this.bossbar = bossbar;
    }

    public Player getPlayer() {
        return this.player;
    }

    public RBossbar getBossbar() {
        return this.bossbar;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
