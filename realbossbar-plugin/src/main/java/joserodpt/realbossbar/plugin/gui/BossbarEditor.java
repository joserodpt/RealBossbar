package joserodpt.realbossbar.plugin.gui;

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
import joserodpt.realbossbar.api.config.RBBLanguage;
import joserodpt.realbossbar.api.config.TranslatableLine;
import joserodpt.realbossbar.plugin.BarText;
import joserodpt.realbossbar.plugin.RealBossbar;
import joserodpt.realutils.dialog.DialogForm;
import joserodpt.realutils.dialog.DialogMenu;
import joserodpt.realutils.dialog.Dialogs;
import joserodpt.realutils.dialog.PagedDialogMenu;
import joserodpt.realutils.gui.GUIBuilder;
import joserodpt.realutils.gui.Pagination;
import joserodpt.realutils.input.PlayerInput;
import joserodpt.realutils.item.Items;
import joserodpt.realutils.text.Text;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static joserodpt.realbossbar.api.config.TranslatableLine.TranslatableLinePlaceholder.INFO;
import static joserodpt.realbossbar.api.config.TranslatableLine.TranslatableLinePlaceholder.NAME;
import static joserodpt.realbossbar.api.config.TranslatableLine.TranslatableLinePlaceholder.VALUE;

/**
 * The in-game bar editor. On servers with dialogs (1.21.6 and up) it is a set of dialogs; everywhere
 * else it is two inventories, with anything typed asked for in chat. Every change is saved to
 * bossbars.yml as it is made.
 */
public final class BossbarEditor {

    private static final int LIST_PAGE = 45;

    private final RealBossbar rbb;

    public BossbarEditor(final RealBossbar rbb) {
        this.rbb = rbb;
    }

    private List<RBossbar> bars() {
        return new ArrayList<>(this.rbb.getBossbarManager().getPermanentBossbars());
    }

    /** Saves the change, unless the bar was deleted while its editor was open, and says whether it did. */
    private boolean save(final RBossbar bar) {
        if (this.rbb.getBossbarManager().getBossbar(bar.getName()) != bar) {
            return false;
        }
        this.rbb.getBossbarManager().save(bar);
        return true;
    }

    /** Saves and goes back to the bar's dialog, or, if the bar is gone, says so and goes back to the list. */
    private void saveAndOpen(final Player p, final RBossbar bar, final int page) {
        if (this.save(bar)) {
            this.openBar(p, bar, page);
        } else {
            this.gone(p, bar);
            this.openList(p, page);
        }
    }

    /** The inventory version of {@link #saveAndOpen}. */
    private void saveAndOpenInventory(final Player p, final RBossbar bar, final int page) {
        if (this.save(bar)) {
            this.openBarInventory(p, bar, page);
        } else {
            this.gone(p, bar);
            this.openListInventory(p, page);
        }
    }

    private void gone(final Player p, final RBossbar bar) {
        TranslatableLine.BAR_NOT_FOUND.with(NAME, bar.getName()).send(p);
    }

    /** Where a slider starts: the value, kept within the slider as the dialog keeps it. */
    private static int within(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String label(final RBossbar bar) {
        return BarText.status(bar) + " &f" + bar.getName();
    }

    private static List<String> lines(final String route, final String info) {
        return RBBLanguage.file().getStringList(route).stream()
                .map(line -> line.replace("%info%", info))
                .collect(Collectors.toList());
    }

    // --- the list of bars ---

    public void openList(final Player p, final int page) {
        final boolean shown = new PagedDialogMenu<>(TranslatableLine.EDITOR_LIST_TITLE.get(),
                TranslatableLine.EDITOR_LIST_DESCRIPTION.get(), this.bars())
                .button(TranslatableLine.EDITOR_CREATE.get(), null, () -> this.askCreate(p))
                .entries(BossbarEditor::label, bar -> Text.color(BarText.describe(bar)), (bar, at) -> this.openBar(p, bar, at))
                .icon(Material.DRAGON_HEAD)
                .open(p, page, () -> { }, () -> this.openListInventory(p, page));
        if (!shown) {
            this.openListInventory(p, page);
        }
    }

    private void askCreate(final Player p) {
        new PlayerInput(p, true,
                RBBLanguage.file().getStringList("Editor.Create-Prompt"),
                Dialogs.isSupported() ? RBBLanguage.file().getStringList("Editor.Create-Dialog") : Collections.emptyList(),
                name -> {
                    if (!RBossbar.isValidName(name)) {
                        TranslatableLine.BAR_INVALID_NAME.send(p);
                    } else if (this.rbb.getBossbarManager().getBossbar(name) != null) {
                        TranslatableLine.BAR_ALREADY_EXISTS.with(NAME, name).send(p);
                    } else {
                        final RBossbar bar = this.rbb.getBossbarManager().create(name, "&f" + name);
                        TranslatableLine.BAR_CREATED.with(NAME, bar.getName()).send(p);
                        this.openBar(p, bar, 0);
                        return;
                    }
                    this.openList(p, 0);
                },
                input -> this.openList(p, 0));
    }

    private void openListInventory(final Player p, final int page) {
        final Pagination<RBossbar> pages = new Pagination<>(LIST_PAGE, this.bars());
        final int shown = pages.exists(page) ? page : Math.max(0, pages.totalPages() - 1);
        final GUIBuilder gui = new GUIBuilder(TranslatableLine.EDITOR_LIST_TITLE.get(), 54, p.getUniqueId());

        if (!pages.isEmpty()) {
            int slot = 0;
            for (final RBossbar bar : pages.getPage(shown)) {
                gui.setItem(Items.createItem(wool(bar.getColor()), 1, label(bar), lines("Editor.Items.Bar-Lore", BarText.describe(bar))),
                        slot++, e -> this.openBarInventory(p, bar, shown));
            }
        }
        if (pages.exists(shown - 1)) {
            gui.setItem(Items.createItem(Material.ARROW, 1, TranslatableLine.GUI_PREVIOUS.get()), 45,
                    e -> this.openListInventory(p, shown - 1));
        }
        gui.setItem(Items.createItem(Material.NETHER_STAR, 1, TranslatableLine.EDITOR_CREATE.get()), 48, e -> this.askCreate(p));
        gui.setItem(Items.createItem(Material.BARRIER, 1, TranslatableLine.GUI_CLOSE.get()), 50, e -> p.closeInventory());
        if (pages.exists(shown + 1)) {
            gui.setItem(Items.createItem(Material.ARROW, 1, TranslatableLine.GUI_NEXT.get()), 53,
                    e -> this.openListInventory(p, shown + 1));
        }
        gui.openInventory(p);
    }

    // --- one bar, in dialogs ---

    /** @param page the list page to go back to */
    public void openBar(final Player p, final RBossbar bar, final int page) {
        if (!Dialogs.isSupported()) {
            this.openBarInventory(p, bar, page);
            return;
        }
        final String name = bar.getName();
        final DialogMenu menu = new DialogMenu(TranslatableLine.EDITOR_TITLE.with(NAME, name).get(),
                TranslatableLine.EDITOR_DESCRIPTION.with(INFO, BarText.describe(bar)).get())
                .columns(2)
                .icon(Material.DRAGON_HEAD)
                .option(TranslatableLine.EDITOR_ENABLED.with(VALUE, BarText.status(bar)).get(), null, () -> {
                    bar.setEnabled(!bar.isEnabled());
                    this.saveAndOpen(p, bar, page);
                })
                .option(TranslatableLine.EDITOR_TITLES.get(), null, () -> this.editTitles(p, bar, page))
                .option(TranslatableLine.EDITOR_COLOR.with(VALUE, BarText.color(bar.getColor()) + Text.beautifyEnumName(bar.getColor().name())).get(), null,
                        () -> this.pick(p, bar, page, TranslatableLine.EDITOR_PICK_COLOR, BarColor.values(),
                                color -> BarText.color(color) + Text.beautifyEnumName(color.name()), bar::setColor))
                .option(TranslatableLine.EDITOR_STYLE.with(VALUE, Text.beautifyEnumName(bar.getStyle().name())).get(), null,
                        () -> this.pick(p, bar, page, TranslatableLine.EDITOR_PICK_STYLE, BarStyle.values(),
                                style -> "&f" + Text.beautifyEnumName(style.name()), bar::setStyle))
                .option(TranslatableLine.EDITOR_ANIMATION.with(VALUE, Text.beautifyEnumName(bar.getAnimation().name())).get(), null,
                        () -> this.pick(p, bar, page, TranslatableLine.EDITOR_PICK_ANIMATION, ProgressAnimation.values(),
                                animation -> "&f" + Text.beautifyEnumName(animation.name()), bar::setAnimation))
                .option(TranslatableLine.EDITOR_PROGRESS.get(), null, () -> this.editProgress(p, bar, page))
                .option(TranslatableLine.EDITOR_AUDIENCE.with(VALUE, bar.getAudience()).get(), null, () -> this.editAudience(p, bar, page))
                .option(TranslatableLine.EDITOR_WORLDS.get(), null, () -> this.editWorlds(p, bar, page))
                .option(TranslatableLine.EDITOR_DELETE.get(), null, () -> this.confirmDelete(p, bar, page))
                .close(TranslatableLine.EDITOR_BACK.get());
        if (!menu.open(p, () -> this.openList(p, page), () -> this.openBarInventory(p, bar, page))) {
            this.openBarInventory(p, bar, page);
        }
    }

    private <E extends Enum<E>> void pick(final Player p, final RBossbar bar, final int page, final TranslatableLine title,
                                          final E[] values, final java.util.function.Function<E, String> label, final Consumer<E> set) {
        final DialogMenu menu = new DialogMenu(title.with(NAME, bar.getName()).get(), "").columns(2);
        for (final E value : values) {
            menu.option(label.apply(value), null, () -> {
                set.accept(value);
                this.saveAndOpen(p, bar, page);
            });
        }
        menu.close(TranslatableLine.EDITOR_BACK.get())
                .open(p, () -> this.openBar(p, bar, page), () -> this.openBarInventory(p, bar, page));
    }

    private void editTitles(final Player p, final RBossbar bar, final int page) {
        final int interval = within(bar.getTitleInterval(), 1, 200);
        new DialogForm(TranslatableLine.EDITOR_TITLES_DIALOG.with(NAME, bar.getName()).get(),
                TranslatableLine.EDITOR_TITLES_DESCRIPTION.get())
                .text("titles", TranslatableLine.EDITOR_TITLES_FIELD.get(), BarText.joinFrames(bar.getTitles()), 2048)
                .slider("interval", TranslatableLine.EDITOR_TITLE_INTERVAL_FIELD.get(), 1, 200, 1, interval)
                .open(p, answers -> {
                    final List<String> frames = BarText.splitFrames(answers.text("titles", ""));
                    if (!frames.isEmpty()) {
                        bar.setTitles(frames);
                    }
                    //against where the slider started, so an interval over 200 isn't cut to 200 untouched
                    final Double moved = answers.moved("interval", interval, 1);
                    if (moved != null) {
                        bar.setTitleInterval((int) Math.round(moved));
                    }
                    this.saveAndOpen(p, bar, page);
                }, () -> this.openBar(p, bar, page), () -> this.openBarInventory(p, bar, page));
    }

    private void editProgress(final Player p, final RBossbar bar, final int page) {
        //where the sliders start, which a value out of their range is moved into
        final int progress = within((int) Math.round(bar.getProgress() * 100), 0, 100);
        final int duration = within(bar.getAnimationDuration(), 1, 300);
        new DialogForm(TranslatableLine.EDITOR_PROGRESS_DIALOG.with(NAME, bar.getName()).get(),
                TranslatableLine.EDITOR_PROGRESS_DESCRIPTION.get())
                .slider("progress", TranslatableLine.EDITOR_PROGRESS_FIELD.get(), 0, 100, 1, progress)
                .slider("duration", TranslatableLine.EDITOR_DURATION_FIELD.get(), 1, 300, 1, duration)
                .text("placeholder", TranslatableLine.EDITOR_PLACEHOLDER_FIELD.get(), bar.getProgressPlaceholder(), 256)
                .open(p, answers -> {
                    final Double moved = answers.moved("progress", progress, 1);
                    if (moved != null) {
                        bar.setProgress(moved / 100D);
                    }
                    final Double seconds = answers.moved("duration", duration, 1);
                    if (seconds != null) {
                        bar.setAnimation(bar.getAnimation(), (int) Math.round(seconds));
                    }
                    bar.setProgressPlaceholder(answers.text("placeholder", bar.getProgressPlaceholder()));
                    this.saveAndOpen(p, bar, page);
                }, () -> this.openBar(p, bar, page), () -> this.openBarInventory(p, bar, page));
    }

    private void editAudience(final Player p, final RBossbar bar, final int page) {
        new DialogForm(TranslatableLine.EDITOR_AUDIENCE_DIALOG.with(NAME, bar.getName()).get(),
                TranslatableLine.EDITOR_AUDIENCE_DESCRIPTION.get())
                .text("audience", TranslatableLine.EDITOR_AUDIENCE_FIELD.get(), bar.getAudience().toString(), 1024)
                .open(p, answers -> {
                    if (this.setAudience(p, bar, answers.text("audience", bar.getAudience().toString()))) {
                        this.openBar(p, bar, page);
                    } else {
                        this.openList(p, page);
                    }
                }, () -> this.openBar(p, bar, page), () -> this.openBarInventory(p, bar, page));
    }

    /** @return false if the bar is gone, after saying so; a typo only says what couldn't be read */
    private boolean setAudience(final Player p, final RBossbar bar, final String typed) {
        final Audience audience = Audience.parse(typed);
        if (audience == null) {
            TranslatableLine.BAR_INVALID_AUDIENCE.with(VALUE, typed).send(p);
            return true;
        }
        bar.setAudience(audience);
        if (!this.save(bar)) {
            this.gone(p, bar);
            return false;
        }
        return true;
    }

    private void editWorlds(final Player p, final RBossbar bar, final int page) {
        new DialogForm(TranslatableLine.EDITOR_WORLDS_DIALOG.with(NAME, bar.getName()).get(),
                TranslatableLine.EDITOR_WORLDS_DESCRIPTION.get())
                .text("worlds", TranslatableLine.EDITOR_WORLDS_FIELD.get(), String.join(", ", bar.getWorlds()), 1024)
                .text("disabled", TranslatableLine.EDITOR_DISABLED_WORLDS_FIELD.get(), String.join(", ", bar.getDisabledWorlds()), 1024)
                .open(p, answers -> {
                    setWorlds(bar, answers.text("worlds", String.join(",", bar.getWorlds())));
                    bar.setDisabledWorlds(BarText.splitList(answers.text("disabled", String.join(",", bar.getDisabledWorlds()))));
                    this.saveAndOpen(p, bar, page);
                }, () -> this.openBar(p, bar, page), () -> this.openBarInventory(p, bar, page));
    }

    /** No worlds at all reads as every world: a bar meant to show nowhere is disabled instead. */
    private static void setWorlds(final RBossbar bar, final String typed) {
        final List<String> worlds = BarText.splitList(typed);
        bar.setWorlds(worlds.isEmpty() ? Collections.singletonList(RBossbar.ALL_WORLDS) : worlds);
    }

    private void confirmDelete(final Player p, final RBossbar bar, final int page) {
        final boolean asked = Dialogs.confirm(p, TranslatableLine.EDITOR_TITLE.with(NAME, bar.getName()).get(),
                TranslatableLine.EDITOR_DELETE_CONFIRM.with(NAME, bar.getName()).get(),
                TranslatableLine.EDITOR_DELETE.get(), null,
                () -> this.delete(p, bar, page), () -> this.openBar(p, bar, page));
        if (!asked) {
            this.openBarInventory(p, bar, page);
        }
    }

    private void delete(final Player p, final RBossbar bar, final int page) {
        if (this.rbb.getBossbarManager().getBossbar(bar.getName()) == bar && this.rbb.getBossbarManager().delete(bar.getName())) {
            TranslatableLine.BAR_DELETED.with(NAME, bar.getName()).send(p);
        }
        this.openList(p, page);
    }

    // --- one bar, in an inventory ---

    private void openBarInventory(final Player p, final RBossbar bar, final int page) {
        final GUIBuilder gui = new GUIBuilder(TranslatableLine.EDITOR_TITLE.with(NAME, bar.getName()).get(), 27, p.getUniqueId(),
                GUIBuilder.placeholder(org.bukkit.DyeColor.BLACK, "&r"));
        final Runnable reopen = () -> this.openBarInventory(p, bar, page);
        final String cycle = TranslatableLine.GUI_CLICK_CYCLE.get();
        final String type = TranslatableLine.GUI_CLICK_TYPE.get();

        gui.setItem(Items.createItem(bar.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE, 1,
                TranslatableLine.EDITOR_ENABLED.with(VALUE, BarText.status(bar)).get(), Collections.singletonList(cycle)), 10, e -> {
            bar.setEnabled(!bar.isEnabled());
            this.saveAndOpenInventory(p, bar, page);
        });

        final List<String> titleLore = new ArrayList<>();
        bar.getTitles().forEach(frame -> titleLore.add("&7- &r" + frame));
        titleLore.add("");
        titleLore.add(type);
        gui.setItem(Items.createItem(Material.NAME_TAG, 1, TranslatableLine.EDITOR_TITLES.get(), titleLore), 11,
                e -> this.askChat(p, "Editor.Type-Title", typed -> {
                    final List<String> frames = BarText.splitFrames(typed);
                    if (frames.isEmpty()) {
                        reopen.run();
                        return;
                    }
                    bar.setTitles(frames);
                    this.saveAndOpenInventory(p, bar, page);
                }, reopen));

        gui.setItem(Items.createItem(wool(bar.getColor()), 1, TranslatableLine.EDITOR_COLOR
                .with(VALUE, BarText.color(bar.getColor()) + Text.beautifyEnumName(bar.getColor().name())).get(), Collections.singletonList(cycle)), 12, e -> {
            bar.setColor(cycle(bar.getColor(), BarColor.values(), e.getClick()));
            this.saveAndOpenInventory(p, bar, page);
        });

        gui.setItem(Items.createItem(Material.ITEM_FRAME, 1, TranslatableLine.EDITOR_STYLE
                .with(VALUE, Text.beautifyEnumName(bar.getStyle().name())).get(), Collections.singletonList(cycle)), 13, e -> {
            bar.setStyle(cycle(bar.getStyle(), BarStyle.values(), e.getClick()));
            this.saveAndOpenInventory(p, bar, page);
        });

        gui.setItem(Items.createItem(Material.CLOCK, 1, TranslatableLine.EDITOR_ANIMATION
                .with(VALUE, Text.beautifyEnumName(bar.getAnimation().name())).get(), Collections.singletonList(cycle)), 14, e -> {
            bar.setAnimation(cycle(bar.getAnimation(), ProgressAnimation.values(), e.getClick()));
            this.saveAndOpenInventory(p, bar, page);
        });

        gui.setItem(Items.createItem(Material.EXPERIENCE_BOTTLE, 1, TranslatableLine.GUI_PROGRESS
                .with(VALUE, Math.round(bar.getProgress() * 100)).get(), RBBLanguage.file().getStringList("Editor.Items.Click-Progress")), 15, e -> {
            final double step = e.getClick().isRightClick() ? -0.1D : 0.1D;
            bar.setProgress(Math.round((bar.getProgress() + step) * 10D) / 10D);
            this.saveAndOpenInventory(p, bar, page);
        });

        gui.setItem(Items.createItem(Material.REPEATER, 1, TranslatableLine.GUI_DURATION
                .with(VALUE, bar.getAnimationDuration()).get(), RBBLanguage.file().getStringList("Editor.Items.Click-Duration")), 16, e -> {
            final int step = (e.getClick().isShiftClick() ? 10 : 1) * (e.getClick().isRightClick() ? -1 : 1);
            bar.setAnimation(bar.getAnimation(), bar.getAnimationDuration() + step);
            this.saveAndOpenInventory(p, bar, page);
        });

        gui.setItem(Items.createItem(Material.PLAYER_HEAD, 1, TranslatableLine.EDITOR_AUDIENCE
                .with(VALUE, bar.getAudience()).get(), Collections.singletonList(type)), 19,
                e -> this.askChat(p, "Editor.Type-Audience", typed -> {
                    if (this.setAudience(p, bar, typed)) {
                        reopen.run();
                    } else {
                        this.openListInventory(p, page);
                    }
                }, reopen));

        gui.setItem(Items.createItem(Material.GRASS_BLOCK, 1, TranslatableLine.EDITOR_WORLDS.get(), Arrays.asList(
                        TranslatableLine.GUI_WORLDS.with(VALUE, String.join(", ", bar.getWorlds())).get(),
                        TranslatableLine.GUI_DISABLED_WORLDS.with(VALUE, bar.getDisabledWorlds().isEmpty() ? "-" : String.join(", ", bar.getDisabledWorlds())).get(),
                        "", type)), 20,
                e -> this.askChat(p, "Editor.Type-Worlds", typed -> {
                    setWorlds(bar, typed);
                    this.saveAndOpenInventory(p, bar, page);
                }, reopen));

        gui.setItem(Items.createItem(Material.ARROW, 1, TranslatableLine.EDITOR_BACK.get()), 22, e -> this.openListInventory(p, page));

        gui.setItem(Items.createItem(Material.BARRIER, 1, TranslatableLine.EDITOR_DELETE.get(),
                Collections.singletonList(TranslatableLine.GUI_CLICK_DELETE.get())), 26, e -> {
            if (e.getClick().isShiftClick()) {
                if (this.rbb.getBossbarManager().getBossbar(bar.getName()) == bar && this.rbb.getBossbarManager().delete(bar.getName())) {
                    TranslatableLine.BAR_DELETED.with(NAME, bar.getName()).send(p);
                }
                this.openListInventory(p, page);
            }
        });

        gui.openInventory(p);
    }

    /**
     * Asks in chat only: this is the version for servers without dialogs. {@code typed} opens the
     * next screen itself; {@code cancelled} is opened when the player cancels.
     */
    private void askChat(final Player p, final String titlesRoute, final Consumer<String> typed, final Runnable cancelled) {
        new PlayerInput(p, false, RBBLanguage.file().getStringList(titlesRoute), Collections.emptyList(),
                typed::accept, input -> cancelled.run());
    }

    /** The next constant, or the one before on a right-click. */
    private static <E extends Enum<E>> E cycle(final E current, final E[] values, final ClickType click) {
        final int step = click.isRightClick() ? values.length - 1 : 1;
        return values[(current.ordinal() + step) % values.length];
    }

    private static Material wool(final BarColor color) {
        switch (color) {
            case PINK:
                return Material.PINK_WOOL;
            case BLUE:
                return Material.BLUE_WOOL;
            case RED:
                return Material.RED_WOOL;
            case GREEN:
                return Material.LIME_WOOL;
            case YELLOW:
                return Material.YELLOW_WOOL;
            case PURPLE:
                return Material.PURPLE_WOOL;
            case WHITE:
            default:
                return Material.WHITE_WOOL;
        }
    }
}
