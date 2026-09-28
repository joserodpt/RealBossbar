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
import joserodpt.realbossbar.api.config.RBBConfig;
import joserodpt.realbossbar.api.config.RBBLanguage;
import joserodpt.realbossbar.api.config.TranslatableLine;
import joserodpt.realbossbar.api.event.RealBossbarPluginLoadedEvent;
import joserodpt.realbossbar.plugin.command.RBBCommandManager;
import joserodpt.realbossbar.plugin.listener.PlayerListener;
import joserodpt.realpermissions.api.RealPermissionsAPI;
import joserodpt.realpermissions.api.pluginhook.ExternalPlugin;
import joserodpt.realpermissions.api.pluginhook.ExternalPluginPermission;
import joserodpt.realutils.RealUtils;
import joserodpt.realutils.dialog.Dialogs;
import joserodpt.realutils.gui.GUIBuilder;
import joserodpt.realutils.input.PlayerInput;
import joserodpt.realutils.text.ForestColorAPI;
import joserodpt.realutils.text.Text;
import joserodpt.realutils.update.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class RealBossbarPlugin extends JavaPlugin {

    /** 0 until the plugin is on SpigotMC; the update check is skipped until then. */
    public static final int SPIGOT_RESOURCE_ID = 0;
    public static final String SPIGOT_URL = "https://www.spigotmc.org/resources/" + SPIGOT_RESOURCE_ID + "/";
    /** 0 until the plugin is registered on bStats; no metrics are sent until then. */
    public static final int BSTATS_ID = 0;

    private static RealBossbarPlugin instance;

    private RealBossbar realBossbar;
    /** The newer version SpigotMC has, or null while this one is the latest (or it couldn't be asked). */
    private volatile String newVersion;

    public static RealBossbarPlugin getPlugin() {
        return instance;
    }

    @Override
    public void onEnable() {
        printASCII();
        final long start = System.currentTimeMillis();

        instance = this;
        //first: the GUIs' listeners, and the plugin RealUtils schedules and logs through
        RealUtils.setup(this);
        //hex colours, as &#FFAA00, in titles and messages alike
        Text.colorizer(ForestColorAPI::colorize);
        //read on every message, so a reloaded prefix applies
        Text.prefix(() -> RBBConfig.file().getString("RealBossbar.Prefix") + " &r");
        realBossbar = new RealBossbar(this);
        RealBossbarAPI.setInstance(realBossbar);

        //before the first bar is shown, so its title is filled in from the start
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Placeholders.setEnabled(true);
            new RealBossbarPlaceholderAPI(realBossbar).register();
            getLogger().info("Hooked onto PlaceholderAPI!");
        }

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerListener(realBossbar), this);
        pm.registerEvents(PlayerInput.getListener(), this);
        //bars are edited, and typed input asked for, in dialogs on servers that have them
        Dialogs.setup(this, () -> RBBConfig.file().getBoolean("RealBossbar.useDialogs", true));
        Dialogs.colorizer(ForestColorAPI::colorize);
        applyDialogLabels();
        PlayerInput.setup(this,
                p -> RBBLanguage.file().getStringList("System.Type-Input"),
                p -> RBBLanguage.file().getStringList("System.Type-Input-Dialog"),
                TranslatableLine.SYSTEM_INPUT_CANCELLED::send,
                TranslatableLine.SYSTEM_ERROR_OCCURRED::send);

        //Lamp owns the command tree: the suggestions, the permissions and the error messages
        new RBBCommandManager(realBossbar);

        getLogger().info("Loaded " + realBossbar.getBossbarManager().getBossbars().size() + " bossbars.");
        realBossbar.getDisplayManager().start();
        //a /reload leaves players online who never join again
        realBossbar.getDisplayManager().refreshAll();

        if (getServer().getPluginManager().getPlugin("RealPermissions") != null) {
            registerRealPermissions();
        }

        if (BSTATS_ID > 0) {
            new Metrics(this, BSTATS_ID);
        }

        if (SPIGOT_RESOURCE_ID > 0 && RBBConfig.file().getBoolean("RealBossbar.Check-for-Updates", true)) {
            new UpdateChecker(this, SPIGOT_RESOURCE_ID).getVersion(version -> {
                if (version != null && UpdateChecker.isNewer(version, this.getDescription().getVersion())) {
                    this.newVersion = version;
                    this.getLogger().warning("There is a new update available! Version: " + version + " -> " + SPIGOT_URL);
                } else {
                    this.getLogger().info("The plugin is updated to the latest version.");
                }
            });
        }

        Bukkit.getPluginManager().callEvent(new RealBossbarPluginLoadedEvent());

        getLogger().info("Finished loading in " + ((System.currentTimeMillis() - start) / 1000F) + " seconds.");
        getLogger().info("<------------------ RealBossbar vPT ------------------>".replace("PT", this.getDescription().getVersion()));
    }

    /** The dialogs' own buttons, from language.yml. Applied again on a reload. */
    void applyDialogLabels() {
        Dialogs.labels(TranslatableLine.SYSTEM_DIALOG_CONFIRM.get(), TranslatableLine.SYSTEM_DIALOG_CANCEL.get(),
                TranslatableLine.SYSTEM_DIALOG_CLOSE.get(), TranslatableLine.SYSTEM_DIALOG_BACK.get(), TranslatableLine.SYSTEM_DIALOG_SAVE.get());
        Dialogs.pageLabels(TranslatableLine.SYSTEM_DIALOG_PREVIOUS.get(), TranslatableLine.SYSTEM_DIALOG_NEXT.get());
    }

    /** Takes every editor inventory, dialog and chat prompt off players' screens, forgetting their callbacks. */
    void closeEditors() {
        //a prompt asked in a text box takes its own dialog away
        PlayerInput.cancelAll();
        for (final Player p : Bukkit.getOnlinePlayers()) {
            Dialogs.close(p.getUniqueId());
        }
        GUIBuilder.closeAll();
    }

    /** The newer version on SpigotMC, or null when there is none. */
    public String getNewVersion() {
        return this.newVersion;
    }

    /**
     * Publishes the plugin's permissions to RealPermissions, so they can be handed out from its GUI
     * instead of being typed from the wiki.
     */
    private void registerRealPermissions() {
        try {
            final List<ExternalPluginPermission> permissions = new ArrayList<>();
            permissions.add(new ExternalPluginPermission("realbossbar.admin",
                    "Create, edit, delete, show and hide bossbars, and reload the plugin.",
                    Arrays.asList("rbb create <name>", "rbb edit", "rbb reload")));
            permissions.add(new ExternalPluginPermission("realbossbar.announce",
                    "Send temporary bossbar announcements.", Collections.singletonList("rbb announce <audience> <seconds> <message>")));
            permissions.add(new ExternalPluginPermission("realbossbar.toggle",
                    "Hide and show bossbars for yourself.", Collections.singletonList("rbb toggle [bar]")));

            RealPermissionsAPI.getInstance().getHooksAPI().addHook(new ExternalPlugin(
                    this.getDescription().getName(), "&fReal&5Bossbar", this.getDescription().getDescription(),
                    Material.DRAGON_HEAD, permissions, this.getDescription().getVersion()));
        } catch (final Exception e) {
            getLogger().warning("Error while trying to register RealBossbar permissions onto RealPermissions.");
            e.printStackTrace();
        }
    }

    private void printASCII() {
        logWithColor("&5  ____            _ ____                 _");
        logWithColor("&5 |  _ \\ ___  __ _| | __ )  ___  ___ ___| |__   __ _ _ __");
        logWithColor("&5 | |_) / _ \\/ _` | |  _ \\ / _ \\/ __/ __| '_ \\ / _` | '__|");
        logWithColor("&5 |  _ <  __/ (_| | | |_) | (_) \\__ \\__ \\ |_) | (_| | |");
        logWithColor("&5 |_| \\_\\___|\\__,_|_|____/ \\___/|___/___/_.__/ \\__,_|_|");
        logWithColor("&5 &8Made by: &9JoseGamer_PT                  &8Version: &9" + this.getDescription().getVersion());
        logWithColor("");
    }

    public void logWithColor(String s) {
        getServer().getConsoleSender().sendMessage("[" + this.getDescription().getName() + "] " + Text.color(s));
    }

    @Override
    public void onDisable() {
        //the listeners behind them are going: an editor left open would let its items be taken
        closeEditors();
        //after closing them, since dialogs can't be taken off screens once it has run
        Dialogs.shutdown();
        if (realBossbar != null) {
            realBossbar.getDisplayManager().stop();
            //Bukkit bars outlive the plugin otherwise, stuck on screen until the player leaves
            realBossbar.getDisplayManager().hideAll();
        }
    }
}
