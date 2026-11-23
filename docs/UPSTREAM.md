# Notes for the Legacy4J and Couchcraft developers

This bridge works, but most of it exists only because each mod assumes things the other breaks. This page lists exactly what it patches, so it's easy to judge what could be done properly upstream. File names refer to `src/main/java/dev/couchlegacy/`.

## How Couchcraft works (short version)

Couchcraft keeps one `PlayerContext` per player and swaps it onto the `Minecraft` singleton (`Couch.enter(ctx)` → `saveFrom` / `loadInto`) around each player's tick and frame: player, gui/hud, game renderer, mouse handler, key states, and so on. Anything stored *outside* those objects is shared by all players.

## Legacy4J: single-player state that had to be swapped per player

All of these are swapped in `PlayerContextMixin` (on `saveFrom` / `loadInto`):

| Legacy4J state | Symptom when shared | Where in the bridge |
|---|---|---|
| `ControllerManager` instance fields (`connectedController`, `isCursorDisabled`, `isControllerTheLastInput`, `simulateShift`, `canChangeSlidersValue`, ...) | Guest input and cursor state mixed with player 1's | `GuestPadBridge` (reflection) |
| Every `ControllerBinding.state()` (`BindingState` fields) | Button presses leaking between players | `GuestPadBridge` (reflection) |
| `ControlTooltipRenderer.getInstance()` | Tooltip bar flickering / showing on the wrong viewport | `ControlTooltipRendererMixin` |
| `LegacyGuiElements.lastHotbarSelectionChange`, `lastHotbarSelection`, `lastGui` | XP bar / HUD flicker | `CouchLegacyBridge.save/loadHudState` |
| `AnimatedCharacterRenderer.time`, `remainingTime` | Paper doll shown for every player when one sprints | same |
| `Legacy4JClient.canSprint`, `sprintTicksLeft` | Double-tap sprint broken for others while P1 is in a menu | same |
| UIAccessor HUD definitions (installed only on the startup `Hud`) | Guests got the vanilla HUD | `CouchLegacyBridge.onContextCreated` |

Other Legacy4J assumptions:

- **Controllers:** Legacy4J reads one controller (`selectedController`). For guests, the bridge wraps Couchcraft's already-polled `Gamepad` in a Legacy4J `Controller` (`GuestController`, SDL3 indices) and calls `ControllerManager.updateBindings(controller)` inside the guest's scope (`PadControllerMixin`). Couchcraft's own guest pad handling is turned off.
- **System cursor:** `setRawPointerPos` warps and `setCursorInputMode` hides/shows the real OS cursor. Guests must stay virtual-only (`ControllerManagerMixin`).
- **Pointer visibility:** `renderGameOverlay` draws the pointer only when `SDL_GetWindowRelativeMouseMode` is false, which follows player 1's mouse grab (`LegacyRenderUtilMixin`).
- **Skins:** Legacy Skins stores one selection and one favorites list (`LegacyOptions`), and `SkinSyncClient` uses the session user id. Guests get their own store (`SkinDataStoreMixin`, `SkinSyncClientMixin`, `GuestSkinStore`).
- **Own name tag in GUI figures:** vanilla hides it only because the entity is the camera entity, which isn't true for every viewport in split screen (`InventoryScreenMixin`, `LegacyRenderUtilMixin`).

### What would make this clean in Legacy4J

1. Keep per-player client state in one object (controller manager + binding states + HUD timers + sprint + doll + tooltip renderer) instead of statics/singletons, or expose a small hook to snapshot/restore it.
2. Let `ControllerManager` drive a given `Controller` without touching the OS cursor (virtual-only mode).
3. Optionally: a "split-screen HUD size" option, and a way for another mod to mark screens as host-only.

Even without official split screen, (1) alone would remove almost all of the reflection here.

## Couchcraft: bugs and suggestions

**Bug: world border texture (crash).** Every `WorldBorderRenderer` constructor registers a new `MipmappedTexture` at `FORCEFIELD_LOCATION`. Creating a guest's renderer replaces the loaded texture with an unloaded one, and the game crashes (`Texture view does not exist`) the next time a world border is on screen. Fix used here: register only once (`WorldBorderRendererMixin`).

**Bug: layout not applied.** Changing `twoPlayerLayout` needs `Couch.markLayoutDirty()`; fine inside Couchcraft, but worth documenting for other mods.

**Suggestions:**
- A small API: "a player context was created / saved / loaded" callbacks, so other mods can keep per-player state without mixing into `PlayerContext`.
- A way for another mod to take over a guest's controller input (the bridge cancels `PadController.frame` for that).
- Options for console-style joining (START twice), and for disabling "BACK on a free pad takes over player 1".
- `pt_br` translation (included here in `assets/couchcraft/lang/pt_br.json`).
