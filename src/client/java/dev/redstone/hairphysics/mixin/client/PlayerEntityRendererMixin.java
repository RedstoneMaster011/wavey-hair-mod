package dev.redstone.hairphysics.mixin.client;

import dev.redstone.hairphysics.client.HairphysicsClient;
import dev.redstone.hairphysics.client.render.MaskedSkinTextureManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

    @Inject(
        method = "getTexture(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)Lnet/minecraft/util/Identifier;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void hairphysics$useMaskedSkin(PlayerEntityRenderState state, CallbackInfoReturnable<Identifier> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        Entity entity = client.world.getEntityById(state.id);
        if (!(entity instanceof AbstractClientPlayerEntity player)) return;
        if (!HairphysicsClient.shouldDisplayHairFor(player)) return;

        Identifier original = cir.getReturnValue();
        if (original == null) return;

        String sourceUrl = null;
        try {
            if (state.skinTextures.body() instanceof AssetInfo.SkinAssetInfo skinAsset) {
                sourceUrl = skinAsset.url();
            }
        } catch (Exception ignored) {}

        Identifier masked = MaskedSkinTextureManager.getMaskedSkin(player, original, sourceUrl);
        if (!masked.equals(original)) {
            cir.setReturnValue(masked);
        }
    }
}
