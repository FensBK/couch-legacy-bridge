<!-- Paste this as a new issue at https://github.com/zortos293/Couchcraft/issues -->

**Title:** Crash "Texture view does not exist" when a guest joins and a world border is on screen

**Version:** Couchcraft 0.2.0, Minecraft 26.3, Fabric Loader 0.20.1

**What happens**

Joining a second player in a world where the world border is visible (for example the Legacy4J Tutorial world) crashes the game on the next frame:

```
java.lang.IllegalStateException: Texture view does not exist, can't get it before something initializes it
	at net.minecraft.client.renderer.texture.AbstractTexture.getTextureView(AbstractTexture.java:41)
	at net.minecraft.client.renderer.WorldBorderRenderer.prepareRenderPass(WorldBorderRenderer.java:201)
	at net.minecraft.client.renderer.WorldBorderRenderer.render(WorldBorderRenderer.java:166)
	at net.minecraft.client.renderer.LevelRenderer.executeClassicTransparency(LevelRenderer.java:677)
	...
	at dev.couchcraft.context.Couch.asPrimary(Couch.java:362)
```

**Cause**

The `WorldBorderRenderer` constructor does:

```java
this.textureManager.register(FORCEFIELD_LOCATION, new MipmappedTexture(FORCEFIELD_LOCATION, 4));
```

When Couchcraft creates the renderer for a guest, this replaces the forcefield texture that was loaded with the resource packs by a new one that never gets loaded. Every player's border rendering then uses the empty texture.

**Workaround / possible fix**

Only let the first registration through, for example with a `@WrapOperation` on `TextureManager.register` in `WorldBorderRenderer.<init>` that skips it once the texture is already registered. That's what I'm doing in a compatibility mod and it fixes the crash:
https://github.com/FensBK/couch-legacy-bridge/blob/main/src/main/java/dev/couchlegacy/mixin/WorldBorderRendererMixin.java
