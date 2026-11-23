# Couchcraft + Legacy4J Bridge

*[Leia em português](LEIA-ME.md)*

A small Fabric mod that makes [Couchcraft](https://github.com/zortos293/Couchcraft)'s local split screen work with [Legacy4J](https://github.com/Wilyicaro/Legacy-Minecraft), so couch co-op feels like the Legacy Console Editions: one window, press START on another controller to join, and every player gets the full Legacy4J experience.

Built for the **Re-Console** modpack on Minecraft **26.3** (Fabric).

> **Honest note:** I'm not a Java developer. This was written with AI help (Claude) and tested by me over many hours. Parts of it are workarounds: Legacy4J keeps single-player state in global objects, and this mod swaps that state per player with reflection. It works, but it is fragile across updates. See [docs/UPSTREAM.md](docs/UPSTREAM.md) for what would make it clean.

## Features

**Controllers**
- Legacy4J handles every player's controller, not just player 1's: the console button layout, D-pad/stick menu navigation, remapping, controller icons, virtual cursor in inventories.
- Each player keeps their own controller state, so one player's input never leaks into another's.
- Player 1's controller can't be taken over from another controller (BACK on a free pad does nothing).

**Joining and leaving (console style)**
- START on a free controller shows a *"Press START again to join"* prompt in the middle of the screen; START again opens the profile picker.
- "Exit Game" from a guest's pause menu shows *"Leave Session"* instead of *"return to the main menu"*, and only removes that player.
- Hold BACK + START still leaves (Couchcraft).

**HUD and visuals, per player**
- Legacy4J HUD layout for guests.
- HUD timers, paper doll (top-left figure while sprinting/sneaking/flying), control tooltips and double-tap sprint are per player.
- No name tag on your own figure in the inventory or paper doll.

**Skins**
- Each Couchcraft profile keeps its own Legacy Skins selection and favorites (`config/couchlegacy/guest_skins.json`).

**Guest menus (like on console)**
- Guests get: Resume, Help & Options (Change Skin, How to Play, Controls), Leaderboards, Achievements, Leave, and the player list (SELECT) with chat.
- Player 1 only: Settings (graphics, audio, language, packs), Mods, Save Game, Host Options, per-player privileges, Open to LAN.
- If cheats are enabled in the world, every player can use commands.

**Settings inside Legacy4J's menus (like the Nintendo Switch Edition)**
- *Help & Options → Settings → Game Options*: **Vertical Split Screen**.
- *Help & Options → Settings → User Interface*: **HUD Size (Split Screen)**, used instead of HUD Size while the screen is split.
- Couchcraft's own settings screen is reachable from *Help & Options → Mods → Couchcraft*.

**Fixes**
- Crash when a guest joins in a world with a visible world border (e.g. the Tutorial world): every new renderer replaced the loaded forcefield texture with an empty one.
- Crash while a guest is still choosing a profile.
- Brazilian Portuguese translation for Couchcraft's screens.

## Requirements

| Mod | Version |
|---|---|
| Minecraft | 26.3 |
| Fabric Loader | 0.19.5 or newer |
| Couchcraft | 0.2.x |
| Legacy4J | 1.9.x |
| Java | 25 |

**Known incompatibilities:** Sodium (Couchcraft doesn't support it), RenderScale (all viewports showed the same camera), BedrockSkins (its own skin sync only knows one player, so skins leak between players).

## Install

Download the jar from Releases and put it in your `mods` folder next to Couchcraft and Legacy4J.

## Building

Couchcraft and Legacy4J aren't on any Maven repository, so the build compiles against the jars you already have:

1. Install JDK 25.
2. Create a `mods` folder next to `build.gradle` and copy `couchcraft-0.2.0.jar` and `Legacy4J-26.3-1.9.1-fabric.jar` into it (or pass `-PmodsDir="path/to/your/mods"`).
3. Run `gradlew build`. The jar ends up in `build/libs/`.

## Known limitations

- Performance: the world is rendered once per player and Sodium can't be used.
- The XP level number above the XP bar still follows the normal HUD Size, not the split-screen one.
- Only tested with 2 players so far.
- Tested only on Windows (Prism Launcher); Linux/Steam Deck should work but hasn't been tried yet.

## Credits

- [Couchcraft](https://github.com/zortos293/Couchcraft) by zortos293 (MIT), the split-screen engine.
- [Legacy4J](https://github.com/Wilyicaro/Legacy-Minecraft) by Wilyicaro and contributors (LGPL-3.0), the console experience.
- Bridge by FensZard, written with Claude (Anthropic).

## License

MIT, see [LICENSE](LICENSE).
