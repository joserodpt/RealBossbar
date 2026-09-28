<div align="center">

## RealBossbar

### Bossbars with animations, placeholders and per-world control.

[![Build](https://img.shields.io/github/actions/workflow/status/joserodpt/RealBossbar/maven.yml?branch=main)](https://github.com/joserodpt/RealBossbar/actions)
![Issues](https://img.shields.io/github/issues-raw/joserodpt/RealBossbar)
[![Stars](https://img.shields.io/github/stars/joserodpt/RealBossbar)](https://github.com/joserodpt/RealBossbar/stargazers)

<a href="/#"><img src="https://raw.githubusercontent.com/intergrav/devins-badges/v2/assets/compact/supported/spigot_46h.png" height="35"></a>
<a href="/#"><img src="https://raw.githubusercontent.com/intergrav/devins-badges/v2/assets/compact/supported/paper_46h.png" height="35"></a>
<a href="/#"><img src="https://raw.githubusercontent.com/intergrav/devins-badges/v2/assets/compact/supported/purpur_46h.png" height="35"></a>

</div>

**RealBossbar** puts bossbars at the top of your players' screens with a command or two. Each bar has its own title
(or several, cycled through), a colour and a style, and a progress that can stay put, count down, loop or bounce.
You decide who sees it (everyone, a permission, or a list of players) and in which worlds.

## Features

- **Bossbar management**: create, edit and delete bars with commands or the in-game editor.
- **Custom titles**: one title or several animated frames per bar, with `&` colour codes and hex (`&#FFAA00`).
- **Colours and styles**: all 7 colours, solid or segmented into 6, 10, 12 or 20 notches.
- **Per-player bars**: show a bar to everyone, or show and hide it for particular players.
- **Per-world control**: pick the worlds each bar shows in, turn a bar off in some worlds, and keep every bar or
  every announcement out of a world entirely.
- **Audience targeting**: `all`, `perm:<permission>` or `players:<name>,<name>`, for bars and announcements alike.
- **Progress control**: set the progress in game, or read it from a placeholder such as `%player_health%/20`.
- **Animations**: `STATIC`, `DECREASE` (a countdown), `INCREASE`, `LOOP` or `BOUNCE`, over as many seconds as you like.
- **Announcements**: `/rbb announce all 10 &eServer restarting soon!` shows a bar that empties over 10 seconds and then disappears.
- **PlaceholderAPI**: any placeholder in titles and progress, plus placeholders of its own.
- **Editor**: dialogs on 1.21.6 and up, and inventories on older versions.
- **API**: create and drive bars from your own plugin.

## Requirements

- Spigot, Paper or a fork, 1.14 or newer, on Java 16 or newer.
- Optional: [PlaceholderAPI](https://www.spigotmc.org/resources/6245/) for placeholders, and RealPermissions to
  hand out RealBossbar's permissions from its GUI.

## Commands and Permissions

Everything is under `/realbossbar`, or `/rbb` for short. `/bossbar` stays vanilla's.

| Command                                         | Permission             | What it does                                                   |
|-------------------------------------------------|------------------------|----------------------------------------------------------------|
| `/rbb`                                          |                        | The plugin's version.                                          |
| `/rbb reload`                                   | `realbossbar.admin`    | Reloads config.yml, language.yml and bossbars.yml.             |
| `/rbb list`                                     | `realbossbar.admin`    | Lists every bar.                                               |
| `/rbb edit [bar]`                               | `realbossbar.admin`    | Opens the editor.                                              |
| `/rbb create <name> [title]`                    | `realbossbar.admin`    | Makes a bar, shown to everyone. Split title frames with `\|`.  |
| `/rbb delete <bar>`                             | `realbossbar.admin`    | Deletes a bar.                                                 |
| `/rbb title <bar> <title>`                      | `realbossbar.admin`    | Replaces the title frames, split with `\|`.                    |
| `/rbb color <bar> <color>`                      | `realbossbar.admin`    | `PINK`, `BLUE`, `RED`, `GREEN`, `YELLOW`, `PURPLE`, `WHITE`.   |
| `/rbb style <bar> <style>`                      | `realbossbar.admin`    | `SOLID`, `SEGMENTED_6`, `_10`, `_12` or `_20`.                 |
| `/rbb progress <bar> <0-100>`                   | `realbossbar.admin`    | Sets the progress and stops the animation.                     |
| `/rbb animation <bar> <animation> [seconds]`    | `realbossbar.admin`    | Sets the progress animation.                                   |
| `/rbb restart <bar>`                            | `realbossbar.admin`    | Starts the animation over.                                     |
| `/rbb audience <bar> <audience>`                | `realbossbar.admin`    | `all`, `perm:<permission>` or `players:<name>,<name>`.         |
| `/rbb world enable\|disable <bar> <world>`      | `realbossbar.admin`    | Lets the bar show in a world, or keeps it out.                 |
| `/rbb enable\|disable <bar>`                    | `realbossbar.admin`    | Turns a bar on or off.                                         |
| `/rbb show\|hide <bar> <players>`               | `realbossbar.admin`    | Shows or hides a bar for players, until the server restarts.   |
| `/rbb announce <audience> <seconds> <message>`  | `realbossbar.announce` | Sends a temporary bar.                                         |
| `/rbb toggle [bar]`                             | `realbossbar.toggle`   | Hides or shows bars for yourself. Everyone has it by default.  |

## Configuration

- **config.yml**: the prefix, whether to use dialogs, how often bars update, the worlds where no bar shows, and
  the colour, style and disabled worlds for announcements.
- **bossbars.yml**: every bar. Each setting is explained at the top of the file.
- **language.yml**: every message and editor label.

```yaml
Bossbars:
  server-info:
    Enabled: true
    Titles:
      - "&fReal&5Bossbar"
      - "&7Players online: &b%server_online%"
    Title-Interval: 60          # ticks per frame
    Color: BLUE
    Style: SEGMENTED_10
    Progress: 1.0
    Progress-Placeholder: ""    # e.g. "%player_health%/20"
    Animation: BOUNCE
    Animation-Duration: 6       # seconds
    Audience: all
    Worlds: ["*"]
    Disabled-Worlds: [world_nether]
```

## Placeholders

| Placeholder                        | Value                                              |
|------------------------------------|----------------------------------------------------|
| `%realbossbar_count%`              | How many bars there are.                           |
| `%realbossbar_progress_<bar>%`     | The bar's animation progress, from 0 to 100.       |
| `%realbossbar_animation_<bar>%`    | The bar's animation.                               |
| `%realbossbar_visible_<bar>%`      | `true` if the player sees the bar right now.       |

## API

Add JitPack and the API, `provided`, since RealBossbar is on the server already:

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.joserodpt.RealBossbar</groupId>
    <artifactId>realbossbar-api</artifactId>
    <version>TAG</version>
    <scope>provided</scope>
</dependency>
```

Then, once `RealBossbarPluginLoadedEvent` has fired:

```java
RealBossbarAPI rbb = RealBossbarAPI.getInstance();

// a bar that lives only as long as the server, never written to bossbars.yml
RBossbar bar = new RBossbar("koth", "&6King of the Hill &7- &f%koth_holder%", false);
bar.setColor(BarColor.YELLOW);
bar.setAnimation(ProgressAnimation.DECREASE, 300);
bar.setAudience(Audience.permission("koth.play"));
rbb.getBossbarManager().register(bar);

// a bar saved to bossbars.yml, which admins can edit too: from the second start on it is already
// there, so ask for it rather than making it again. A reload keeps this same object up to date.
RBossbar event = rbb.getBossbarManager().getOrCreate("event", "&6Event starts soon!");

// a temporary bar that empties over 10 seconds
rbb.getBossbarManager().announce(Audience.all(), "&cThe arena closes soon!", BarColor.RED, null, 10);

// per player
rbb.getPlayerManager().show(player, bar);
rbb.getPlayerManager().hide(player, bar);
```

Changes to a bar show in game on its next update. `BossbarShowEvent` (cancellable) and `BossbarHideEvent` fire
as bars appear on and leave players' screens.

## Building

```sh
mvn clean package
```

The plugin jar ends up in `realbossbar-plugin/target/`.
