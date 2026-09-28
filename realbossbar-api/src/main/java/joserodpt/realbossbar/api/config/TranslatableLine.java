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
    BAR_ANNOUNCED("Bossbar.Announced"),
    BAR_LIST_HEADER("Bossbar.List-Header"),
    BAR_LIST_ENTRY("Bossbar.List-Entry"),
    BAR_LIST_EMPTY("Bossbar.List-Empty"),
    BAR_STATUS_ON("Bossbar.Status-On"),
    BAR_STATUS_OFF("Bossbar.Status-Off"),
    EDITOR_LIST_TITLE("Editor.List-Title"),
    EDITOR_LIST_DESCRIPTION("Editor.List-Description"),
    EDITOR_CREATE("Editor.Create"),
    EDITOR_TITLE("Editor.Title"),
    EDITOR_DESCRIPTION("Editor.Description"),
    EDITOR_ENABLED("Editor.Enabled"),
    EDITOR_TITLES("Editor.Titles"),
    EDITOR_TITLES_DIALOG("Editor.Titles-Dialog"),
    EDITOR_TITLES_DESCRIPTION("Editor.Titles-Description"),
    EDITOR_TITLES_FIELD("Editor.Titles-Field"),
    EDITOR_TITLE_INTERVAL_FIELD("Editor.Title-Interval-Field"),
    EDITOR_COLOR("Editor.Color"),
    EDITOR_STYLE("Editor.Style"),
    EDITOR_ANIMATION("Editor.Animation"),
    EDITOR_PROGRESS("Editor.Progress"),
    EDITOR_PROGRESS_DIALOG("Editor.Progress-Dialog"),
    EDITOR_PROGRESS_DESCRIPTION("Editor.Progress-Description"),
    EDITOR_PROGRESS_FIELD("Editor.Progress-Field"),
    EDITOR_DURATION_FIELD("Editor.Duration-Field"),
    EDITOR_PLACEHOLDER_FIELD("Editor.Placeholder-Field"),
    EDITOR_AUDIENCE("Editor.Audience"),
    EDITOR_AUDIENCE_DIALOG("Editor.Audience-Dialog"),
    EDITOR_AUDIENCE_DESCRIPTION("Editor.Audience-Description"),
    EDITOR_AUDIENCE_FIELD("Editor.Audience-Field"),
    EDITOR_WORLDS("Editor.Worlds"),
    EDITOR_WORLDS_DIALOG("Editor.Worlds-Dialog"),
    EDITOR_WORLDS_DESCRIPTION("Editor.Worlds-Description"),
    EDITOR_WORLDS_FIELD("Editor.Worlds-Field"),
    EDITOR_DISABLED_WORLDS_FIELD("Editor.Disabled-Worlds-Field"),
    EDITOR_DELETE("Editor.Delete"),
    EDITOR_DELETE_CONFIRM("Editor.Delete-Confirm"),
    EDITOR_PICK_COLOR("Editor.Pick-Color"),
    EDITOR_PICK_STYLE("Editor.Pick-Style"),
    EDITOR_PICK_ANIMATION("Editor.Pick-Animation"),
    EDITOR_BACK("Editor.Back"),
    GUI_PREVIOUS("Editor.Items.Previous"),
    GUI_NEXT("Editor.Items.Next"),
    GUI_CLOSE("Editor.Items.Close"),
    GUI_CLICK_CYCLE("Editor.Items.Click-Cycle"),
    GUI_CLICK_TYPE("Editor.Items.Click-Type"),
    GUI_CLICK_DELETE("Editor.Items.Click-Delete"),
    GUI_DURATION("Editor.Items.Duration"),
    GUI_PROGRESS("Editor.Items.Progress"),
    GUI_WORLDS("Editor.Items.Worlds"),
    GUI_DISABLED_WORLDS("Editor.Items.Disabled-Worlds"),
    SYSTEM_NEW_UPDATE("System.New-Update"),
    SYSTEM_DIALOG_CONFIRM("System.Dialog-Confirm"),
    SYSTEM_DIALOG_CANCEL("System.Dialog-Cancel"),
    SYSTEM_DIALOG_SAVE("System.Dialog-Save"),
    SYSTEM_DIALOG_BACK("System.Dialog-Back"),
    SYSTEM_DIALOG_CLOSE("System.Dialog-Close"),
    SYSTEM_DIALOG_PREVIOUS("System.Dialog-Previous"),
    SYSTEM_DIALOG_NEXT("System.Dialog-Next"),
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
