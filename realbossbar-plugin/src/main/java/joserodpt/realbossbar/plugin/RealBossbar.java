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

import joserodpt.realbossbar.api.RealBossbarAPI;
import joserodpt.realbossbar.api.config.RBBBossbars;
import joserodpt.realbossbar.api.config.RBBConfig;
import joserodpt.realbossbar.api.config.RBBLanguage;
import joserodpt.realbossbar.plugin.gui.BossbarEditor;
import joserodpt.realbossbar.plugin.managers.BossbarManager;
import joserodpt.realbossbar.plugin.managers.DisplayManager;
import joserodpt.realbossbar.plugin.managers.PlayerManager;

import java.util.logging.Logger;

public class RealBossbar extends RealBossbarAPI {

    private final Logger logger;
    private final RealBossbarPlugin plugin;
    private final BossbarManager bossbarManager;
    private final PlayerManager playerManager;
    private final DisplayManager displayManager;
    private final BossbarEditor editor;

    public RealBossbar(final RealBossbarPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();

        RBBConfig.setup(plugin);
        RBBLanguage.setup(plugin);
        RBBBossbars.setup(plugin);

        this.bossbarManager = new BossbarManager(this);
        this.playerManager = new PlayerManager(this);
        this.displayManager = new DisplayManager(this);
        this.editor = new BossbarEditor(this);

        this.bossbarManager.load();
    }

    @Override
    public RealBossbarPlugin getPlugin() {
        return this.plugin;
    }

    @Override
    public BossbarManager getBossbarManager() {
        return this.bossbarManager;
    }

    @Override
    public PlayerManager getPlayerManager() {
        return this.playerManager;
    }

    /** Lives here rather than on RealBossbarAPI: rendering is the plugin's, not part of the API. */
    public DisplayManager getDisplayManager() {
        return this.displayManager;
    }

    public BossbarEditor getEditor() {
        return this.editor;
    }

    @Override
    public void reload() {
        //the editor's screens and prompts were built from the settings about to be read again
        this.plugin.closeEditors();
        //shown again below, with the settings read again
        this.displayManager.hideAll();

        RBBConfig.reload();
        RBBLanguage.reload();
        RBBBossbars.reload();

        this.bossbarManager.load();
        //Update-Interval may have changed
        this.displayManager.start();
        this.displayManager.refreshAll();
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

    @Override
    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }
}
