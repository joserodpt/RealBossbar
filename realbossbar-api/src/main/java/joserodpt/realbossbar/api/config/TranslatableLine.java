package joserodpt.realbossbar.api.config;

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

import dev.dejvokep.boostedyaml.YamlDocument;
import joserodpt.realutils.text.LanguageLine;
import joserodpt.realutils.text.LanguageMessage;
import joserodpt.realutils.text.Placeholder;

/**
 * Every line the plugin says to a player, as a constant pointing at its route in language.yml.
 *
 * <p>Placeholders are filled with {@link #with(Placeholder, Object)}, which hands back a new
 * {@link LanguageMessage} rather than changing the constant:</p>
 *
 * <pre>{@code
 * TranslatableLine.BAR_CREATED.with(NAME, bar.getName()).send(p);
 * }</pre>
 */
public enum TranslatableLine implements LanguageLine {
    BAR_CREATED("Bossbar.Created"),
    BAR_DELETED("Bossbar.Deleted"),
    BAR_NOT_FOUND("Bossbar.Not-Found"),
    BAR_ALREADY_EXISTS("Bossbar.Already-Exists"),
    BAR_INVALID_NAME("Bossbar.Invalid-Name"),
    BAR_UPDATED("Bossbar.Updated"),
    BAR_ENABLED("Bossbar.Enabled"),
    BAR_DISABLED("Bossbar.Disabled"),
    BAR_RESET("Bossbar.Reset"),
    BAR_WORLD_ENABLED("Bossbar.World-Enabled"),
    BAR_WORLD_DISABLED("Bossbar.World-Disabled"),
    BAR_INVALID_AUDIENCE("Bossbar.Invalid-Audience"),
    BAR_INVALID_PROGRESS("Bossbar.Invalid-Progress"),
    BAR_SHOWN("Bossbar.Shown"),
    BAR_HIDDEN("Bossbar.Hidden"),
    BAR_TOGGLE_ON("Bossbar.Toggle-On"),
    BAR_TOGGLE_OFF("Bossbar.Toggle-Off"),
    BAR_TOGGLE_BAR_ON("Bossbar.Toggle-Bar-On"),
    BAR_TOGGLE_BAR_OFF("Bossbar.Toggle-Bar-Off"),
    BAR_TOGGLE_BAR_HIDING_ALL("Bossbar.Toggle-Bar-Hiding-All"),
    BAR_ANNOUNCED("Bossbar.Announced"),
    BAR_LIST_HEADER("Bossbar.List-Header"),
    BAR_LIST_ENTRY("Bossbar.List-Entry"),
    BAR_LIST_EMPTY("Bossbar.List-Empty"),
    BAR_STATUS_ON("Bossbar.Status-On"),
    BAR_STATUS_OFF("Bossbar.Status-Off"),
    EDITOR_LIST_TITLE("Editor.List-Title"),
    EDITOR_CREATE("Editor.Create"),
    EDITOR_TITLE("Editor.Title"),
    EDITOR_ENABLED("Editor.Enabled"),
    EDITOR_TITLES("Editor.Titles"),
    EDITOR_COLOR("Editor.Color"),
    EDITOR_STYLE("Editor.Style"),
    EDITOR_ANIMATION("Editor.Animation"),
    EDITOR_PROGRESS("Editor.Progress"),
    EDITOR_DURATION("Editor.Duration"),
    EDITOR_PLACEHOLDER("Editor.Placeholder"),
    EDITOR_AUDIENCE("Editor.Audience"),
    EDITOR_WORLDS("Editor.Worlds"),
    EDITOR_RESTART("Editor.Restart"),
    EDITOR_DELETE("Editor.Delete"),
    EDITOR_DELETE_TITLE("Editor.Delete-Title"),
    EDITOR_DELETE_CONFIRM("Editor.Delete-Confirm"),
    EDITOR_BACK("Editor.Back"),
    EDITOR_PICK_COLOR("Editor.Pick-Color"),
    EDITOR_PICK_STYLE("Editor.Pick-Style"),
    EDITOR_PICK_ANIMATION("Editor.Pick-Animation"),
    EDITOR_TITLES_TITLE("Editor.Titles-Title"),
    EDITOR_FRAME("Editor.Frame"),
    EDITOR_ADD_FRAME("Editor.Add-Frame"),
    EDITOR_REPLACE_FRAMES("Editor.Replace-Frames"),
    EDITOR_TITLE_INTERVAL("Editor.Title-Interval"),
    EDITOR_LAST_FRAME("Editor.Last-Frame"),
    EDITOR_AUDIENCE_TITLE("Editor.Audience-Title"),
    EDITOR_AUDIENCE_ALL("Editor.Audience-All"),
    EDITOR_AUDIENCE_PERMISSION("Editor.Audience-Permission"),
    EDITOR_AUDIENCE_PLAYERS("Editor.Audience-Players"),
    EDITOR_PLAYERS_TITLE("Editor.Players-Title"),
    EDITOR_ADD_PLAYER("Editor.Add-Player"),
    EDITOR_LAST_PLAYER("Editor.Last-Player"),
    EDITOR_INVALID_PLAYER("Editor.Invalid-Player"),
    EDITOR_WORLDS_TITLE("Editor.Worlds-Title"),
    EDITOR_ALL_WORLDS("Editor.All-Worlds"),
    EDITOR_TYPE_WORLDS("Editor.Type-Worlds"),
    GUI_PREVIOUS("Editor.Items.Previous"),
    GUI_NEXT("Editor.Items.Next"),
    GUI_CLOSE("Editor.Items.Close"),
    GUI_YES("Editor.Items.Yes"),
    GUI_NO("Editor.Items.No"),
    GUI_CLICK_TOGGLE("Editor.Items.Click-Toggle"),
    GUI_CLICK_CHANGE("Editor.Items.Click-Change"),
    GUI_CLICK_TYPE("Editor.Items.Click-Type"),
    GUI_CLICK_SELECT("Editor.Items.Click-Select"),
    GUI_SELECTED("Editor.Items.Selected"),
    GUI_CURRENT_TITLE("Editor.Items.Current-Title"),
    GUI_CLICK_PERMISSION("Editor.Items.Click-Permission"),
    GUI_CLICK_PLAYERS("Editor.Items.Click-Players"),
    GUI_PLAYER_IN("Editor.Items.Player-In"),
    GUI_PLAYER_OUT("Editor.Items.Player-Out"),
    GUI_WORLD_SHOWN("Editor.Items.World-Shown"),
    GUI_WORLD_UNLISTED("Editor.Items.World-Unlisted"),
    GUI_WORLD_DISABLED("Editor.Items.World-Disabled"),
    GUI_WORLD_UNLOADED("Editor.Items.World-Unloaded"),
    SYSTEM_NEW_UPDATE("System.New-Update"),
    SYSTEM_INPUT_CANCELLED("System.Input-Cancelled"),
    SYSTEM_ERROR_OCCURRED("System.Error-Occurred"),
    SYSTEM_RELOADED("System.Reloaded"),
    SYSTEM_PLAYER_ONLY("System.Player-Only"),
    SYSTEM_ERROR_PERMISSION("System.Error-Permission"),
    SYSTEM_ERROR_COMMAND("System.Error-Command"),
    SYSTEM_ERROR_USAGE("System.Error-Usage");

    private final String configPath;

    TranslatableLine(String configPath) {
        this.configPath = configPath;
    }

    @Override
    public String getPath() {
        return this.configPath;
    }

    @Override
    public YamlDocument getLanguageFile() {
        return RBBLanguage.file();
    }

    /** The tokens a line in language.yml may contain. {@code NAME} is written {@code %name%}. */
    public enum TranslatableLinePlaceholder implements Placeholder {
        NAME, VALUE, PLAYER, WORLD, INFO
    }
}
