package dev.couchlegacy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.couchlegacy.CouchLegacyBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.util.client.LegacyRenderUtil;

/**
 * Legacy4J only draws its pointer while the real mouse is free (not captured by the game), and that
 * follows player 1: the pointer of a guest showed up only while player 1 also had a menu open.
 * A guest never uses the real mouse, so for a guest it counts as free exactly while that guest has a menu open.
 */
@Mixin(value = LegacyRenderUtil.class, remap = false)
public abstract class LegacyRenderUtilMixin {
	@WrapOperation(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLMouse;SDL_GetWindowRelativeMouseMode(J)Z"))
	private static boolean couchlegacy$guestPointerAlwaysVisible(final long window, final Operation<Boolean> original) {
		if (CouchLegacyBridge.activeGuest() != null) {
			// No menu open: act as if the mouse were captured, so the pointer is hidden in game.
			return net.minecraft.client.Minecraft.getInstance().gui.screen() == null;
		}

		return original.call(window);
	}

	/** "HUD Size (Split Screen)" replaces "HUD Size" while the screen is split. */
	@WrapOperation(method = "getHUDScale", at = @At(value = "INVOKE", target = "Lwily/factoryapi/base/config/FactoryConfig;get()Ljava/lang/Object;"))
	private static Object couchlegacy$splitHudSize(final wily.factoryapi.base.config.FactoryConfig<?> config, final Operation<Object> original) {
		Object value = original.call(config);
		if (config == wily.legacy.client.LegacyOptions.hudSize && value instanceof Integer size) {
			return dev.couchlegacy.SplitSettings.effectiveHudSize(size);
		}

		return value;
	}

	/** Player figures drawn in menus and the HUD (inventory, paper doll) never carry a name tag. */
	@WrapOperation(method = "renderEntity(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIFLorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/world/entity/Entity;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"))
	private static net.minecraft.client.renderer.entity.state.EntityRenderState couchlegacy$noNameTagInGui(final net.minecraft.client.renderer.entity.EntityRenderer<?, ?> renderer, final net.minecraft.world.entity.Entity entity, final float partialTick, final Operation<net.minecraft.client.renderer.entity.state.EntityRenderState> original) {
		net.minecraft.client.renderer.entity.state.EntityRenderState state = original.call(renderer, entity, partialTick);
		state.nameTag = null;
		return state;
	}
}
