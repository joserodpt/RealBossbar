package joserodpt.realbossbar.api;

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

import com.google.common.base.Preconditions;
import joserodpt.realbossbar.api.managers.BossbarManagerAPI;
import joserodpt.realbossbar.api.managers.PlayerManagerAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * The way into RealBossbar from another plugin:
 *
 * <pre>{@code
 * RealBossbarAPI rbb = RealBossbarAPI.getInstance();
 * //the bar is saved to bossbars.yml, so from the second start on it is already there
 * RBossbar bar = rbb.getBossbarManager().getOrCreate("event", "&6Event starts soon!");
 * bar.setAnimation(ProgressAnimation.DECREASE, 60);
 * rbb.getBossbarManager().save(bar);
 * }</pre>
 *
 * <p>Wait for {@link joserodpt.realbossbar.api.event.RealBossbarPluginLoadedEvent} before calling it
 * on startup, rather than guessing at load order.</p>
 */
public abstract class RealBossbarAPI {

    private static RealBossbarAPI instance;

    /**
     * Gets instance of this API
     *
     * @return RealBossbarAPI API instance
     */
    public static RealBossbarAPI getInstance() {
        return instance;
    }

    /**
     * Sets the RealBossbarAPI instance.
     * <b>Note! This method may only be called once</b>
     *
     * @param instance the new instance to set
     */
    public static void setInstance(RealBossbarAPI instance) {
        Preconditions.checkNotNull(instance, "instance");
        Preconditions.checkArgument(RealBossbarAPI.instance == null, "Instance already set");
        RealBossbarAPI.instance = instance;
    }

    public abstract JavaPlugin getPlugin();

    public abstract BossbarManagerAPI getBossbarManager();

    public abstract PlayerManagerAPI getPlayerManager();

    public abstract void reload();

    public abstract String getVersion();

    public abstract Logger getLogger();
}
