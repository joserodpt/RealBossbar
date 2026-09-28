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

import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Who a bar is for: everyone, whoever has a permission, or a list of players by name.
 *
 * <p>Written the same way in bossbars.yml and in commands: {@code all}, {@code perm:<permission>} or
 * {@code players:<name>,<name>}.</p>
 */
public final class Audience {

    public enum Type {ALL, PERMISSION, PLAYERS}

    private static final Audience ALL = new Audience(Type.ALL, null, Collections.emptySet());

    private final Type type;
    private final String permission;
    /** Lower case, so a name matches however it was typed. */
    private final Set<String> players;

    private Audience(final Type type, final String permission, final Set<String> players) {
        this.type = type;
        this.permission = permission;
        this.players = players;
    }

    public static Audience all() {
        return ALL;
    }

    public static Audience permission(final String permission) {
        return new Audience(Type.PERMISSION, permission, Collections.emptySet());
    }

    public static Audience players(final Collection<String> names) {
        return new Audience(Type.PLAYERS, null, Collections.unmodifiableSet(names.stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.<String, Set<String>>toCollection(LinkedHashSet::new))));
    }

    /**
     * Reads an audience as it is written in bossbars.yml.
     *
     * @return null if it is none of the three forms
     */
    public static Audience parse(final String text) {
        if (text == null) {
            return null;
        }
        final String trimmed = text.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("all") || trimmed.equals("*")) {
            return ALL;
        }
        final int colon = trimmed.indexOf(':');
        if (colon < 0) {
            return null;
        }
        final String kind = trimmed.substring(0, colon).toLowerCase(Locale.ROOT);
        final String value = trimmed.substring(colon + 1).trim();
        if (value.isEmpty()) {
            return null;
        }
        switch (kind) {
            case "perm":
            case "permission":
                return permission(value);
            case "players":
            case "player":
                return players(Arrays.asList(value.split(",")));
            default:
                return null;
        }
    }

    public boolean matches(final Player p) {
        switch (this.type) {
            case PERMISSION:
                return p.hasPermission(this.permission);
            case PLAYERS:
                return this.players.contains(p.getName().toLowerCase(Locale.ROOT));
            case ALL:
            default:
                return true;
        }
    }

    public Type getType() {
        return this.type;
    }

    /** The permission, or null unless this is a {@link Type#PERMISSION} audience. */
    public String getPermission() {
        return this.permission;
    }

    /** The players' names in lower case; empty unless this is a {@link Type#PLAYERS} audience. */
    public Set<String> getPlayers() {
        return this.players;
    }

    /** As {@link #parse} reads it back. */
    @Override
    public String toString() {
        switch (this.type) {
            case PERMISSION:
                return "perm:" + this.permission;
            case PLAYERS:
                return "players:" + String.join(",", this.players);
            case ALL:
            default:
                return "all";
        }
    }
}
