package dev.redstone.hairphysics.mixin.client;

import dev.redstone.hairphysics.client.HairphysicsClient;
import dev.redstone.hairphysics.client.render.MaskedSkinTextureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
//? if >=1.21.9
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
//? if <1.21.9
/*import net.minecraft.client.renderer.entity.player.PlayerRenderer;
*/
//? if >=1.21.9
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
//? if >=1.21.2 && <1.21.9
/*import net.minecraft.client.renderer.entity.state.PlayerRenderState;
*/
//? if >=1.21.9
import net.minecraft.core.ClientAsset;
//? if >=1.21.11
import net.minecraft.resources.Identifier;
//? if <1.21.11
/*import net.minecraft.resources.ResourceLocation;
*/
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if >=1.21.9
@Mixin(AvatarRenderer.class)
//? if <1.21.9
/*@Mixin(PlayerRenderer.class)
*/
public class PlayerEntityRendererMixin {

    @Inject(
//? if >=1.21.11
        method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Lnet/minecraft/resources/Identifier;",
//? if >=1.21.9 && <1.21.11
        /*method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Lnet/minecraft/resources/ResourceLocation;",
        */
//? if >=1.21.2 && <1.21.9
        /*method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;)Lnet/minecraft/resources/ResourceLocation;",
        */
//? if <1.21.2
        /*method = "getTextureLocation(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/resources/ResourceLocation;",
        */
        at = @At("RETURN"),
        cancellable = true
    )
//? if >=1.21.11
    private void hairphysics$useMaskedSkin(AvatarRenderState state, CallbackInfoReturnable<Identifier> cir) {
//? if >=1.21.9 && <1.21.11
    /*private void hairphysics$useMaskedSkin(AvatarRenderState state, CallbackInfoReturnable<ResourceLocation> cir) {
    */
//? if >=1.21.2 && <1.21.9
    /*private void hairphysics$useMaskedSkin(PlayerRenderState state, CallbackInfoReturnable<ResourceLocation> cir) {
    */
//? if <1.21.2
    /*private void hairphysics$useMaskedSkin(AbstractClientPlayer player, CallbackInfoReturnable<ResourceLocation> cir) {
    */
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

//? if >=1.21.2
        Entity entity = client.level.getEntity(state.id);
//? if >=1.21.2
        if (!(entity instanceof AbstractClientPlayer player)) return;
//? if <1.21.2 {
        if (player == null) return;
//?}
        if (!HairphysicsClient.shouldDisplayHairFor(player)) return;

//? if >=1.21.11
        Identifier original = cir.getReturnValue();
//? if <1.21.11
        /*ResourceLocation original = cir.getReturnValue();
        */
        if (original == null) return;

        String sourceUrl = null;
//? if >=1.21.9 {
        try {
            if (state.skin.body() instanceof ClientAsset.DownloadedTexture skinAsset) {
                sourceUrl = skinAsset.url();
            }
        } catch (Exception ignored) {}
//?}
//? if >=1.21.2 && <1.21.9 {
        try {
            if (state.skin != null) {
                sourceUrl = state.skin.textureUrl();
            }
        } catch (Exception ignored) {}
//?}

//? if >=1.21.11
        Identifier masked = MaskedSkinTextureManager.getMaskedSkin(player, original, sourceUrl);
//? if <1.21.11
        /*ResourceLocation masked = MaskedSkinTextureManager.getMaskedSkin(player, original, sourceUrl);
        */
        if (!masked.equals(original)) {
            cir.setReturnValue(masked);
        }
    }
}
