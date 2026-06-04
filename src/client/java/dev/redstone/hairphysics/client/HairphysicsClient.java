package dev.redstone.hairphysics.client;

import dev.redstone.hairphysics.client.data.SkinMetadataLoader;
import dev.redstone.hairphysics.client.gui.HairEditorScreen;
import dev.redstone.hairphysics.client.physics.PhysicsTickHandler;
import dev.redstone.hairphysics.client.render.HairFeatureRenderer;
import dev.redstone.hairphysics.client.render.MaskedSkinTextureManager;
import dev.redstone.hairphysics.client.render.SkinTextureCache;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HairphysicsClient implements ClientModInitializer {

    public static final String MOD_ID = "hairphysics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static KeyBinding EDITOR_KEY;

    public static boolean shouldDisplayHairFor(AbstractClientPlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null && player.getUuid().equals(client.player.getUuid());
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("[HairPhysics] Initializing client...");

        
        EDITOR_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.hairphysics.editor",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            KeyBinding.Category.MISC
        ));

        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PhysicsTickHandler.onClientTick(client);
            if (EDITOR_KEY.wasPressed()
                    && client.currentScreen == null
                    && client.player != null) {
                client.setScreen(new HairEditorScreen());
            }
        });

        
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
            (entityType, renderer, registrationHelper, context) -> {
                if (renderer instanceof PlayerEntityRenderer playerRenderer) {
                    registrationHelper.register(new HairFeatureRenderer(playerRenderer));
                }
            }
        );

        
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            PhysicsTickHandler.clearAll();
            SkinMetadataLoader.clearCache();
            MaskedSkinTextureManager.clearAll();
            SkinTextureCache.clearAll();
            LOGGER.info("[HairPhysics] Cleared on disconnect.");
        });

        LOGGER.info("[HairPhysics] Ready. Press G to open Hair Editor.");
    }
}
