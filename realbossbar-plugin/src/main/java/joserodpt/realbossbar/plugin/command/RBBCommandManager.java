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

import com.mojang.brigadier.arguments.ArgumentType;
import joserodpt.realbossbar.api.bossbar.RBossbar;
import joserodpt.realbossbar.api.config.TranslatableLine;
import joserodpt.realbossbar.plugin.RealBossbar;
import joserodpt.realutils.command.LampExceptionHandler;
import org.bukkit.Bukkit;
import org.bukkit.World;
import revxrsal.commands.Lamp;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.BukkitLampConfig;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.brigadier.MinecraftArgumentType;
import revxrsal.commands.node.ParameterNode;

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the Lamp instance every RealBossbar command hangs off. Lamp registers the commands straight
 * onto the server's command map, which is why none of them appear in plugin.yml.
 */
public final class RBBCommandManager {

    private final Lamp<BukkitCommandActor> lamp;

    public RBBCommandManager(final RealBossbar rbb) {
        final Map<RBBSuggestion, SuggestionProvider<BukkitCommandActor>> suggestions = suggestions(rbb);

        //Brigadier stays on. Lamp's own matcher treats leftover input as merely a worse match, so
        //`/rbb reload junk` would quietly fall back to the bare `/rbb` handler; Brigadier's tree
        //refuses it outright. Where it can't attach Lamp falls back on its own and
        //the exception handler's messages are what players see instead.
        final BukkitLampConfig<BukkitCommandActor> config = BukkitLampConfig.<BukkitCommandActor>builder(rbb.getPlugin())
                .argumentTypes(types -> types.addTypeFactory(RBBCommandManager::wordArgument))
                .build();

        this.lamp = BukkitLamp.builder(config)
                .exceptionHandler(new LampExceptionHandler(
                        TranslatableLine.SYSTEM_ERROR_COMMAND::send,
                        TranslatableLine.SYSTEM_ERROR_PERMISSION::send,
                        TranslatableLine.SYSTEM_PLAYER_ONLY::send,
                        TranslatableLine.SYSTEM_ERROR_USAGE::get))
                .suggestionProviders(providers -> providers.addProviderForAnnotation(
                        SuggestFrom.class, annotation -> suggestions.get(annotation.value())))
                .build();

        this.lamp.register(new BossbarCMD(rbb));
    }

    /**
     * Brigadier's word type only takes {@code [A-Za-z0-9_.+-]} unquoted, so an audience such as
     * {@code perm:vip.bar} would stop at the {@code :}. GameProfileArgument reads up to the next space
     * whatever is in it, and only resolves a profile when asked to, which the handlers never do: Lamp
     * re-reads the raw input itself. It is kept to the {@link SuggestFrom} arguments, whose suggestions
     * replace the player names the client would otherwise offer.
     */
    private static ArgumentType<?> wordArgument(final ParameterNode<BukkitCommandActor, ?> parameter) {
        if (parameter.type() != String.class || parameter.isGreedy() || !parameter.annotations().contains(SuggestFrom.class)) {
            return null;
        }
        //empty where the server's GameProfileArgument can't be found, leaving Lamp's plain string
        return MinecraftArgumentType.GAME_PROFILE.<Object>getIfPresent().orElse(null);
    }

    private static Map<RBBSuggestion, SuggestionProvider<BukkitCommandActor>> suggestions(final RealBossbar rbb) {
        final Map<RBBSuggestion, SuggestionProvider<BukkitCommandActor>> sources = new EnumMap<>(RBBSuggestion.class);

        sources.put(RBBSuggestion.BOSSBARS, context -> rbb.getBossbarManager().getPermanentBossbars().stream()
                .map(RBossbar::getName)
                .collect(Collectors.toList()));

        sources.put(RBBSuggestion.WORLDS, context -> Bukkit.getWorlds().stream()
                .map(World::getName)
                .collect(Collectors.toList()));

        sources.put(RBBSuggestion.AUDIENCES, SuggestionProvider.of("all", "perm:", "players:"));

        return sources;
    }

    public Lamp<BukkitCommandActor> getLamp() {
        return this.lamp;
    }
}
