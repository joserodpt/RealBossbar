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
import joserodpt.realutils.gui.GUIBuilder;
import joserodpt.realutils.gui.Pagination;
import joserodpt.realutils.input.PlayerInput;
import joserodpt.realutils.item.Items;
import joserodpt.realutils.text.Text;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static joserodpt.realbossbar.api.config.TranslatableLine.TranslatableLinePlaceholder.NAME;
import static joserodpt.realbossbar.api.config.TranslatableLine.TranslatableLinePlaceholder.VALUE;

/**
 * The in-game bar editor: a set of inventories, with anything typed asked for in chat. Every change
 * is saved to bossbars.yml as it is made.
 */
public final class BossbarEditor {

    /** The slots a paged screen lists its entries in: every row but the last. */
    private static final int PAGE = 45;
    /** A player's name in an audience: a comma would split it in two, and a space can't be typed in one. */
    private static final Pattern PLAYER_NAME = Pattern.compile("[^,\\s]{1,32}");

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

    /** Saves and opens {@code next}, or, if the bar is gone, says so and goes back to the list. */
    private void saveAndOpen(final Player p, final RBossbar bar, final int page, final Runnable next) {
        if (this.save(bar)) {
            next.run();
        } else {
            this.gone(p, bar);
            this.openList(p, page);
        }
    }

    private void gone(final Player p, final RBossbar bar) {
        TranslatableLine.BAR_NOT_FOUND.with(NAME, bar.getName()).send(p);
    }

    /**
     * Asks in chat. {@code typed} opens the next screen itself; {@code cancelled} is opened when the
     * player cancels.
     *
     * @param clear strip colour codes from what is typed
     */
    private void askChat(final Player p, final String titlesRoute, final boolean clear, final Consumer<String> typed, final Runnable cancelled) {
        new PlayerInput(p, clear, RBBLanguage.file().getStringList(titlesRoute), Collections.emptyList(),
                typed::accept, input -> cancelled.run());
    }

    // --- building blocks ---

    private static ItemStack filler() {
        return GUIBuilder.placeholder(DyeColor.BLACK, Text.color("&r"));
    }

    /** A screen with every slot filled, for the buttons to be laid over. */
    private static GUIBuilder screen(final String title, final int size, final Player p) {
        return new GUIBuilder(title, size, p.getUniqueId(), filler());
    }

    /** A screen that lists entries over its first five rows, with its buttons along the last. */
    private static GUIBuilder pagedScreen(final String title, final Player p) {
        final GUIBuilder gui = new GUIBuilder(title, 54, p.getUniqueId());
        for (int slot = PAGE; slot < 54; slot++) {
            gui.setItem(filler(), slot);
        }
        return gui;
    }

    /** The page to show: the one asked for, or the last there is when it has gone. */
    private static int shownPage(final Pagination<?> pages, final int page) {
        return pages.exists(page) ? page : Math.max(0, pages.totalPages() - 1);
    }

    private static void pageButtons(final GUIBuilder gui, final Pagination<?> pages, final int shown, final IntConsumer open) {
        if (pages.exists(shown - 1)) {
            gui.setItem(Items.createItem(Material.ARROW, 1, TranslatableLine.GUI_PREVIOUS.get()), 45, e -> open.accept(shown - 1));
        }
        if (pages.exists(shown + 1)) {
            gui.setItem(Items.createItem(Material.ARROW, 1, TranslatableLine.GUI_NEXT.get()), 53, e -> open.accept(shown + 1));
        }
    }

    /** A button that glows while {@code selected}. */
    private static ItemStack item(final Material material, final int amount, final String name, final List<String> lore, final boolean selected) {
        return selected ? Items.createItemLoreEnchanted(material, amount, name, lore) : Items.createItem(material, amount, name, lore);
    }

    private static String label(final RBossbar bar) {
        return BarText.status(bar) + " &f" + bar.getName();
    }

    private static List<String> lines(final String route) {
        return RBBLanguage.file().getStringList(route);
    }

    private static List<String> lines(final String route, final String value, final String info) {
        return lines(route).stream()
                .map(line -> line.replace("%value%", value).replace("%info%", info))
                .collect(Collectors.toList());
    }

    /** These lines, then a blank one, then {@code last}. */
    private static List<String> lore(final List<String> lines, final String last) {
        final List<String> lore = new ArrayList<>(lines);
        if (!lore.isEmpty()) {
            lore.add("");
        }
        lore.add(last);
        return lore;
    }

    private static String listed(final List<String> entries) {
        return entries.isEmpty() ? "-" : String.join(", ", entries);
    }

    /** +1 or -1 on a left or right click, {@code big} times that with shift. */
    private static int step(final ClickType click, final int big) {
        return (click.isShiftClick() ? big : 1) * (click.isRightClick() ? -1 : 1);
    }

    // --- the list of bars ---

    public void openList(final Player p, final int page) {
        final Pagination<RBossbar> pages = new Pagination<>(PAGE, this.bars());
        final int shown = shownPage(pages, page);
        final GUIBuilder gui = pagedScreen(TranslatableLine.EDITOR_LIST_TITLE.get(), p);

        if (!pages.isEmpty()) {
            int slot = 0;
            for (final RBossbar bar : pages.getPage(shown)) {
                gui.setItem(Items.createItem(wool(bar.getColor()), 1, label(bar), lines("Editor.Items.Bar-Lore", "", BarText.describe(bar))),
                        slot++, e -> {
                            if (e.getClick().isRightClick()) {
                                bar.setEnabled(!bar.isEnabled());
                                this.saveAndOpen(p, bar, shown, () -> this.openList(p, shown));
                            } else {
                                this.openBar(p, bar, shown);
                            }
                        });
            }
        }
        pageButtons(gui, pages, shown, at -> this.openList(p, at));
        gui.setItem(Items.createItem(Material.NETHER_STAR, 1, TranslatableLine.EDITOR_CREATE.get()), 48, e -> this.askCreate(p));
        gui.setItem(Items.createItem(Material.BARRIER, 1, TranslatableLine.GUI_CLOSE.get()), 50, e -> p.closeInventory());
        gui.openInventory(p);
    }

    private void askCreate(final Player p) {
        this.askChat(p, "Editor.Create-Prompt", true, name -> {
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
        }, () -> this.openList(p, 0));
    }

    // --- one bar ---

    /** @param page the list page to go back to */
    public void openBar(final Player p, final RBossbar bar, final int page) {
        final GUIBuilder gui = screen(TranslatableLine.EDITOR_TITLE.with(NAME, bar.getName()).get(), 45, p);
        final Runnable reopen = () -> this.openBar(p, bar, page);
        final String change = TranslatableLine.GUI_CLICK_CHANGE.get();

        gui.setItem(Items.createItem(Material.DRAGON_HEAD, 1, label(bar), Arrays.asList(BarText.describe(bar),
                TranslatableLine.GUI_CURRENT_TITLE.with(VALUE, bar.getCurrentTitle()).get())), 4);

        gui.setItem(Items.createItem(bar.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE, 1,
                TranslatableLine.EDITOR_ENABLED.with(VALUE, BarText.status(bar)).get(),
                Collections.singletonList(TranslatableLine.GUI_CLICK_TOGGLE.get())), 10, e -> {
            bar.setEnabled(!bar.isEnabled());
            this.saveAndOpen(p, bar, page, reopen);
        });

        final List<String> frames = bar.getTitles().stream().map(frame -> "&7- &r" + frame).collect(Collectors.toList());
        gui.setItem(Items.createItem(Material.NAME_TAG, 1, TranslatableLine.EDITOR_TITLES.get(), lore(frames, change)), 12,
                e -> this.openTitles(p, bar, page, 0));

        gui.setItem(Items.createItem(wool(bar.getColor()), 1, TranslatableLine.EDITOR_COLOR.with(VALUE, colorName(bar.getColor())).get(),
                Collections.singletonList(change)), 14,
                e -> this.pick(p, bar, page, TranslatableLine.EDITOR_PICK_COLOR, BarColor.values(), bar.getColor(),
                        BossbarEditor::wool, color -> 1, BossbarEditor::colorName, color -> Collections.emptyList(), bar::setColor));

        gui.setItem(Items.createItem(Material.ITEM_FRAME, segments(bar.getStyle()), TranslatableLine.EDITOR_STYLE
                .with(VALUE, Text.beautifyEnumName(bar.getStyle().name())).get(), Collections.singletonList(change)), 16,
                e -> this.pick(p, bar, page, TranslatableLine.EDITOR_PICK_STYLE, BarStyle.values(), bar.getStyle(),
                        style -> Material.ITEM_FRAME, BossbarEditor::segments, style -> "&f" + Text.beautifyEnumName(style.name()),
                        style -> Collections.emptyList(), bar::setStyle));

        gui.setItem(Items.createItem(icon(bar.getAnimation()), 1, TranslatableLine.EDITOR_ANIMATION
                .with(VALUE, Text.beautifyEnumName(bar.getAnimation().name())).get(), lore(describe(bar.getAnimation()), change)), 19,
                e -> this.pick(p, bar, page, TranslatableLine.EDITOR_PICK_ANIMATION, ProgressAnimation.values(), bar.getAnimation(),
                        BossbarEditor::icon, animation -> 1, animation -> "&f" + Text.beautifyEnumName(animation.name()),
                        BossbarEditor::describe, bar::setAnimation));

        gui.setItem(Items.createItem(Material.EXPERIENCE_BOTTLE, 1, TranslatableLine.EDITOR_PROGRESS
                .with(VALUE, Math.round(bar.getProgress() * 100)).get(), lines("Editor.Items.Click-Progress")), 21, e -> {
            //ten points at a time, and one with shift
            final int step = (e.getClick().isRightClick() ? -1 : 1) * (e.getClick().isShiftClick() ? 1 : 10);
            final long percent = Math.round(bar.getProgress() * 100) + step;
            bar.setProgress(Math.max(0, Math.min(100, percent)) / 100D);
            this.saveAndOpen(p, bar, page, reopen);
        });

        gui.setItem(Items.createItem(Material.REPEATER, 1, TranslatableLine.EDITOR_DURATION
                .with(VALUE, bar.getAnimationDuration()).get(), lines("Editor.Items.Click-Duration")), 23, e -> {
            bar.setAnimation(bar.getAnimation(), bar.getAnimationDuration() + step(e.getClick(), 10));
            this.saveAndOpen(p, bar, page, reopen);
        });

        gui.setItem(Items.createItem(Material.COMPARATOR, 1, TranslatableLine.EDITOR_PLACEHOLDER
                .with(VALUE, bar.hasProgressPlaceholder() ? bar.getProgressPlaceholder() : "-").get(),
                lines("Editor.Items.Click-Placeholder")), 25, e -> {
            if (e.getClick().isRightClick()) {
                bar.setProgressPlaceholder("");
                this.saveAndOpen(p, bar, page, reopen);
                return;
            }
            this.askChat(p, "Editor.Type-Placeholder", false, typed -> {
                bar.setProgressPlaceholder(typed);
                this.saveAndOpen(p, bar, page, reopen);
            }, reopen);
        });

        gui.setItem(Items.createItem(Material.PLAYER_HEAD, 1, TranslatableLine.EDITOR_AUDIENCE.with(VALUE, bar.getAudience()).get(),
                Collections.singletonList(change)), 29, e -> this.openAudience(p, bar, page));

        gui.setItem(Items.createItem(Material.GRASS_BLOCK, 1, TranslatableLine.EDITOR_WORLDS.get(), lore(lines("Editor.Items.Worlds-Lore",
                listed(bar.getWorlds()), listed(bar.getDisabledWorlds())), change)), 31, e -> this.openWorlds(p, bar, page, 0));

        gui.setItem(Items.createItem(Material.COMPASS, 1, TranslatableLine.EDITOR_RESTART.get()), 33, e -> {
            bar.resetAnimation();
            TranslatableLine.BAR_RESET.with(NAME, bar.getName()).send(p);
        });

        gui.setItem(Items.createItem(Material.OAK_DOOR, 1, TranslatableLine.EDITOR_BACK.get()), 40, e -> this.openList(p, page));
        gui.setItem(Items.createItem(Material.TNT, 1, TranslatableLine.EDITOR_DELETE.get()), 44, e -> this.confirmDelete(p, bar, page));

        gui.openInventory(p);
    }

    /** Picks one of {@code values}, the current one glowing, and goes back to the bar. */
    private <E extends Enum<E>> void pick(final Player p, final RBossbar bar, final int page, final TranslatableLine title,
                                          final E[] values, final E current, final Function<E, Material> icon, final ToIntFunction<E> amount,
                                          final Function<E, String> label, final Function<E, List<String>> lore, final Consumer<E> set) {
        final GUIBuilder gui = screen(title.with(NAME, bar.getName()).get(), 27, p);
        //centred on the middle row
        int slot = 9 + (9 - values.length) / 2;
        for (final E value : values) {
            final boolean selected = value == current;
            gui.setItem(item(icon.apply(value), amount.applyAsInt(value), label.apply(value), lore(lore.apply(value),
                    (selected ? TranslatableLine.GUI_SELECTED : TranslatableLine.GUI_CLICK_SELECT).get()), selected), slot++, e -> {
                set.accept(value);
                this.saveAndOpen(p, bar, page, () -> this.openBar(p, bar, page));
            });
        }
        gui.setItem(Items.createItem(Material.OAK_DOOR, 1, TranslatableLine.EDITOR_BACK.get()), 22, e -> this.openBar(p, bar, page));
        gui.openInventory(p);
    }

    private void confirmDelete(final Player p, final RBossbar bar, final int page) {
        final GUIBuilder gui = screen(TranslatableLine.EDITOR_DELETE_TITLE.with(NAME, bar.getName()).get(), 27, p);
        gui.setItem(Items.createItem(Material.LIME_CONCRETE, 1, TranslatableLine.GUI_YES.get()), 11, e -> {
            if (this.rbb.getBossbarManager().getBossbar(bar.getName()) == bar && this.rbb.getBossbarManager().delete(bar.getName())) {
                TranslatableLine.BAR_DELETED.with(NAME, bar.getName()).send(p);
            }
            this.openList(p, page);
        });
        gui.setItem(Items.createItem(Material.TNT, 1, TranslatableLine.EDITOR_DELETE_CONFIRM.with(NAME, bar.getName()).get()), 13);
        gui.setItem(Items.createItem(Material.RED_CONCRETE, 1, TranslatableLine.GUI_NO.get()), 15, e -> this.openBar(p, bar, page));
        gui.openInventory(p);
    }

    // --- its titles ---

    /** @param at the page of frames to show */
    private void openTitles(final Player p, final RBossbar bar, final int page, final int at) {
        final List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < bar.getTitles().size(); i++) {
            indexes.add(i);
        }
        final Pagination<Integer> pages = new Pagination<>(PAGE, indexes);
        final int shown = shownPage(pages, at);
        final Runnable reopen = () -> this.openTitles(p, bar, page, shown);
        final GUIBuilder gui = pagedScreen(TranslatableLine.EDITOR_TITLES_TITLE.with(NAME, bar.getName()).get(), p);

        if (!pages.isEmpty()) {
            int slot = 0;
            for (final int index : pages.getPage(shown)) {
                final List<String> lore = new ArrayList<>(Collections.singletonList("&r" + bar.getTitles().get(index)));
                lore.add("");
                lore.addAll(lines("Editor.Items.Click-Frame"));
                gui.setItem(Items.createItem(Material.PAPER, 1, TranslatableLine.EDITOR_FRAME.with(VALUE, index + 1).get(), lore),
                        slot++, e -> this.clickFrame(p, bar, page, index, e.getClick(), reopen));
            }
        }

        pageButtons(gui, pages, shown, next -> this.openTitles(p, bar, page, next));
        gui.setItem(Items.createItem(Material.WRITABLE_BOOK, 1, TranslatableLine.EDITOR_ADD_FRAME.get(),
                Collections.singletonList(TranslatableLine.GUI_CLICK_TYPE.get())), 47,
                e -> this.askChat(p, "Editor.Type-Frame", false, typed -> {
                    final List<String> added = BarText.splitFrames(typed);
                    if (added.isEmpty()) {
                        reopen.run();
                        return;
                    }
                    final List<String> titles = new ArrayList<>(bar.getTitles());
                    titles.addAll(added);
                    bar.setTitles(titles);
                    //to the page the new frame is on
                    this.saveAndOpen(p, bar, page, () -> this.openTitles(p, bar, page, (titles.size() - 1) / PAGE));
                }, reopen));
        gui.setItem(Items.createItem(Material.BOOK, 1, TranslatableLine.EDITOR_REPLACE_FRAMES.get(), lines("Editor.Items.Click-Frames")), 48,
                e -> this.askChat(p, "Editor.Type-Title", false, typed -> {
                    final List<String> frames = BarText.splitFrames(typed);
                    if (frames.isEmpty()) {
                        reopen.run();
                        return;
                    }
                    bar.setTitles(frames);
                    this.saveAndOpen(p, bar, page, () -> this.openTitles(p, bar, page, 0));
                }, reopen));
        gui.setItem(Items.createItem(Material.OAK_DOOR, 1, TranslatableLine.EDITOR_BACK.get()), 49, e -> this.openBar(p, bar, page));
        gui.setItem(Items.createItem(Material.CLOCK, 1, TranslatableLine.EDITOR_TITLE_INTERVAL.with(VALUE, bar.getTitleInterval()).get(),
                lines("Editor.Items.Click-Interval")), 50, e -> {
            bar.setTitleInterval(bar.getTitleInterval() + step(e.getClick(), 10));
            this.saveAndOpen(p, bar, page, reopen);
        });
        gui.openInventory(p);
    }

    /** Changes the frame on a left-click, moves it earlier on a right-click, and removes it on a shift-right-click. */
    private void clickFrame(final Player p, final RBossbar bar, final int page, final int index, final ClickType click, final Runnable reopen) {
        final List<String> titles = new ArrayList<>(bar.getTitles());
        if (index >= titles.size()) {
            reopen.run();
            return;
        }
        if (click.isRightClick() && click.isShiftClick()) {
            if (titles.size() <= 1) {
                TranslatableLine.EDITOR_LAST_FRAME.send(p);
                return;
            }
            titles.remove(index);
        } else if (click.isRightClick()) {
            if (index == 0) {
                return;
            }
            Collections.swap(titles, index, index - 1);
        } else if (click.isLeftClick()) {
            this.askChat(p, "Editor.Type-Frame", false, typed -> {
                final List<String> now = new ArrayList<>(bar.getTitles());
                final List<String> frames = BarText.splitFrames(typed);
                if (frames.isEmpty() || index >= now.size()) {
                    reopen.run();
                    return;
                }
                //a frame typed with | in it becomes several, in its place
                now.remove(index);
                now.addAll(index, frames);
                bar.setTitles(now);
                this.saveAndOpen(p, bar, page, reopen);
            }, reopen);
            return;
        } else {
            return;
        }
        bar.setTitles(titles);
        this.saveAndOpen(p, bar, page, reopen);
    }

    // --- its audience ---

    private void openAudience(final Player p, final RBossbar bar, final int page) {
        final Audience audience = bar.getAudience();
        final Runnable toBar = () -> this.openBar(p, bar, page);
        final GUIBuilder gui = screen(TranslatableLine.EDITOR_AUDIENCE_TITLE.with(NAME, bar.getName()).get(), 27, p);

        gui.setItem(this.audienceItem(Material.BEACON, TranslatableLine.EDITOR_AUDIENCE_ALL, audience, Audience.Type.ALL,
                TranslatableLine.GUI_CLICK_SELECT), 11, e -> {
            bar.setAudience(Audience.all());
            this.saveAndOpen(p, bar, page, toBar);
        });
        gui.setItem(this.audienceItem(Material.TRIPWIRE_HOOK, TranslatableLine.EDITOR_AUDIENCE_PERMISSION, audience, Audience.Type.PERMISSION,
                TranslatableLine.GUI_CLICK_PERMISSION), 13,
                e -> this.askChat(p, "Editor.Type-Permission", true, typed -> {
                    if (this.setAudience(p, bar, "perm:" + typed)) {
                        toBar.run();
                    } else {
                        this.openList(p, page);
                    }
                }, () -> this.openAudience(p, bar, page)));
        gui.setItem(this.audienceItem(Material.PLAYER_HEAD, TranslatableLine.EDITOR_AUDIENCE_PLAYERS, audience, Audience.Type.PLAYERS,
                TranslatableLine.GUI_CLICK_PLAYERS), 15, e -> this.openPlayers(p, bar, page, 0));
        gui.setItem(Items.createItem(Material.OAK_DOOR, 1, TranslatableLine.EDITOR_BACK.get()), 22, e -> toBar.run());
        gui.openInventory(p);
    }

    /** One kind of audience, glowing with the bar's own when it is that kind. */
    private ItemStack audienceItem(final Material material, final TranslatableLine name, final Audience audience, final Audience.Type type,
                                   final TranslatableLine click) {
        final boolean selected = audience.getType() == type;
        final List<String> lore = new ArrayList<>();
        if (selected) {
            lore.add("&b" + audience);
            lore.add(TranslatableLine.GUI_SELECTED.get());
        }
        //every kind but everyone asks for more, so it can be picked again to change it
        if (!selected || type != Audience.Type.ALL) {
            lore.add(click.get());
        }
        return item(material, 1, name.get(), lore, selected);
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

    /** The players of a players audience, and everyone online, to be added or taken out with a click. */
    private void openPlayers(final Player p, final RBossbar bar, final int page, final int at) {
        final Set<String> members = members(bar);
        //by lower case name, which is how the audience keeps them, to the name to show
        final Map<String, String> names = new LinkedHashMap<>();
        members.forEach(name -> names.put(name, name));
        Bukkit.getOnlinePlayers().stream()
                .sorted(Comparator.comparing(online -> online.getName().toLowerCase(Locale.ROOT)))
                .forEach(online -> names.put(online.getName().toLowerCase(Locale.ROOT), online.getName()));

        final Pagination<String> pages = new Pagination<>(PAGE, new ArrayList<>(names.keySet()));
        final int shown = shownPage(pages, at);
        final Runnable reopen = () -> this.openPlayers(p, bar, page, shown);
        final GUIBuilder gui = pagedScreen(TranslatableLine.EDITOR_PLAYERS_TITLE.with(NAME, bar.getName()).get(), p);

        if (!pages.isEmpty()) {
            int slot = 0;
            for (final String key : pages.getPage(shown)) {
                final boolean in = members.contains(key);
                final String name = (in ? "&a" : "&7") + names.get(key);
                final List<String> lore = Collections.singletonList((in ? TranslatableLine.GUI_PLAYER_IN : TranslatableLine.GUI_PLAYER_OUT).get());
                final Player online = Bukkit.getPlayerExact(key);
                gui.setItem(online == null ? Items.createItem(Material.PLAYER_HEAD, 1, name, lore) : Items.createHead(online, 1, name, lore),
                        slot++, e -> {
                            final Set<String> now = members(bar);
                            if (!now.remove(key)) {
                                now.add(key);
                            } else if (now.isEmpty()) {
                                //a players audience of no one can't be saved, and would read back as everyone
                                TranslatableLine.EDITOR_LAST_PLAYER.send(p);
                                return;
                            }
                            bar.setAudience(Audience.players(now));
                            this.saveAndOpen(p, bar, page, reopen);
                        });
            }
        }

        pageButtons(gui, pages, shown, next -> this.openPlayers(p, bar, page, next));
        gui.setItem(Items.createItem(Material.NAME_TAG, 1, TranslatableLine.EDITOR_ADD_PLAYER.get(),
                Collections.singletonList(TranslatableLine.GUI_CLICK_TYPE.get())), 48,
                e -> this.askChat(p, "Editor.Type-Player", true, typed -> {
                    if (!PLAYER_NAME.matcher(typed).matches()) {
                        TranslatableLine.EDITOR_INVALID_PLAYER.with(VALUE, typed).send(p);
                        reopen.run();
                        return;
                    }
                    final Set<String> now = members(bar);
                    now.add(typed);
                    bar.setAudience(Audience.players(now));
                    this.saveAndOpen(p, bar, page, reopen);
                }, reopen));
        gui.setItem(Items.createItem(Material.OAK_DOOR, 1, TranslatableLine.EDITOR_BACK.get()), 49, e -> this.openAudience(p, bar, page));
        gui.openInventory(p);
    }

    /** A copy of the players of its audience, in lower case, or none when it isn't a players audience. */
    private static Set<String> members(final RBossbar bar) {
        return bar.getAudience().getType() == Audience.Type.PLAYERS
                ? new LinkedHashSet<>(bar.getAudience().getPlayers()) : new LinkedHashSet<>();
    }

    // --- its worlds ---

    /** Every loaded world, and any the bar lists that aren't, to be turned on or off with a click. */
    private void openWorlds(final Player p, final RBossbar bar, final int page, final int at) {
        //by lower case name, since the bar matches worlds however they were typed, to the name to show
        final Map<String, String> names = new LinkedHashMap<>();
        Bukkit.getWorlds().forEach(world -> names.put(world.getName().toLowerCase(Locale.ROOT), world.getName()));
        for (final String listed : concat(bar.getWorlds(), bar.getDisabledWorlds())) {
            if (!listed.equals(RBossbar.ALL_WORLDS)) {
                names.putIfAbsent(listed.toLowerCase(Locale.ROOT), listed);
            }
        }

        final Pagination<String> pages = new Pagination<>(PAGE, new ArrayList<>(names.values()));
        final int shown = shownPage(pages, at);
        final Runnable reopen = () -> this.openWorlds(p, bar, page, shown);
        final GUIBuilder gui = pagedScreen(TranslatableLine.EDITOR_WORLDS_TITLE.with(NAME, bar.getName()).get(), p);

        if (!pages.isEmpty()) {
            int slot = 0;
            for (final String name : pages.getPage(shown)) {
                final World world = Bukkit.getWorld(name);
                final boolean allowed = bar.isAllowedIn(name);
                final List<String> lore = new ArrayList<>();
                lore.add((allowed ? TranslatableLine.GUI_WORLD_SHOWN
                        : containsIgnoreCase(bar.getDisabledWorlds(), name) ? TranslatableLine.GUI_WORLD_DISABLED
                        : TranslatableLine.GUI_WORLD_UNLISTED).get());
                if (world == null) {
                    lore.add(TranslatableLine.GUI_WORLD_UNLOADED.get());
                }
                lore.add("");
                lore.add(TranslatableLine.GUI_CLICK_TOGGLE.get());
                gui.setItem(item(icon(world), 1, "&f" + name, lore, allowed), slot++, e -> {
                    bar.setWorldEnabled(name, !bar.isAllowedIn(name));
                    this.saveAndOpen(p, bar, page, reopen);
                });
            }
        }

        pageButtons(gui, pages, shown, next -> this.openWorlds(p, bar, page, next));
        final boolean everyWorld = isInAllWorlds(bar);
        gui.setItem(item(Material.NETHER_STAR, 1, TranslatableLine.EDITOR_ALL_WORLDS.with(VALUE,
                (everyWorld ? TranslatableLine.GUI_YES : TranslatableLine.GUI_NO).get()).get(),
                lore(lines("Editor.Items.All-Worlds-Lore"), TranslatableLine.GUI_CLICK_TOGGLE.get()), everyWorld), 47, e -> {
            if (isInAllWorlds(bar)) {
                //the worlds it shows in now, so turning this off changes nothing until a world is added
                bar.setWorlds(Bukkit.getWorlds().stream().map(World::getName).filter(bar::isAllowedIn).collect(Collectors.toList()));
            } else {
                bar.setWorlds(Collections.singletonList(RBossbar.ALL_WORLDS));
            }
            this.saveAndOpen(p, bar, page, reopen);
        });
        gui.setItem(Items.createItem(Material.OAK_DOOR, 1, TranslatableLine.EDITOR_BACK.get()), 49, e -> this.openBar(p, bar, page));
        gui.setItem(Items.createItem(Material.WRITABLE_BOOK, 1, TranslatableLine.EDITOR_TYPE_WORLDS.get(), lines("Editor.Items.Type-Worlds-Lore")), 51,
                e -> this.askChat(p, "Editor.Type-Worlds", false, typed -> {
                    setWorlds(bar, typed);
                    this.saveAndOpen(p, bar, page, () -> this.openWorlds(p, bar, page, 0));
                }, reopen));
        gui.openInventory(p);
    }

    /** No worlds at all reads as every world: a bar meant to show nowhere is disabled instead. */
    private static void setWorlds(final RBossbar bar, final String typed) {
        final List<String> worlds = BarText.splitList(typed);
        bar.setWorlds(worlds.isEmpty() ? Collections.singletonList(RBossbar.ALL_WORLDS) : worlds);
    }

    private static boolean isInAllWorlds(final RBossbar bar) {
        return bar.getWorlds().isEmpty() || bar.getWorlds().contains(RBossbar.ALL_WORLDS);
    }

    private static boolean containsIgnoreCase(final List<String> list, final String value) {
        return list.stream().anyMatch(entry -> entry.equalsIgnoreCase(value));
    }

    private static List<String> concat(final List<String> first, final List<String> second) {
        final List<String> both = new ArrayList<>(first);
        both.addAll(second);
        return both;
    }

    // --- icons ---

    private static String colorName(final BarColor color) {
        return BarText.color(color) + Text.beautifyEnumName(color.name());
    }

    /** How many parts a style splits the bar into, shown as the item's amount. */
    private static int segments(final BarStyle style) {
        final String name = style.name();
        return name.startsWith("SEGMENTED_") ? Integer.parseInt(name.substring("SEGMENTED_".length())) : 1;
    }

    private static List<String> describe(final ProgressAnimation animation) {
        final String line = RBBLanguage.file().getString("Editor.Animations." + animation.name(), "");
        return line.isEmpty() ? Collections.emptyList() : Collections.singletonList(line);
    }

    private static Material icon(final ProgressAnimation animation) {
        switch (animation) {
            case DECREASE:
                return Material.HOPPER;
            case INCREASE:
                return Material.PISTON;
            case LOOP:
                return Material.REPEATER;
            case BOUNCE:
                return Material.SLIME_BALL;
            case STATIC:
            default:
                return Material.PAINTING;
        }
    }

    /** @param world null for one that isn't loaded */
    private static Material icon(final World world) {
        if (world == null) {
            return Material.MAP;
        }
        switch (world.getEnvironment()) {
            case NETHER:
                return Material.NETHERRACK;
            case THE_END:
                return Material.END_STONE;
            case NORMAL:
                return Material.GRASS_BLOCK;
            default:
                return Material.STONE;
        }
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
