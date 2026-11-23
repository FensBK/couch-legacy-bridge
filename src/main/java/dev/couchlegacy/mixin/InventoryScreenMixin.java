package dev.couchlegacy.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Vanilla hides the player's own name on the inventory figure only because the player is the camera.
 * In split screen that check no longer holds, so the name tag is removed from GUI figures directly.
 */
@Mixin(value = InventoryScreen.class, remap = false)
public abstract class InventoryScreenMixin {
	@ModifyReturnValue(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;", at = @org.spongepowered.asm.mixin.injection.At("RETURN"))
	private static EntityRenderState couchlegacy$noNameTag(final EntityRenderState state) {
		state.nameTag = null;
		return state;
	}
}
