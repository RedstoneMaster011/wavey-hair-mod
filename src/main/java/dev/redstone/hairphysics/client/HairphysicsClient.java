package dev.redstone.hairphysics.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.redstone.hairphysics.client.data.SkinMetadataLoader;
import dev.redstone.hairphysics.client.gui.HairEditorScreen;
import dev.redstone.hairphysics.client.physics.PhysicsTickHandler;
import dev.redstone.hairphysics.client.render.HairFeatureRenderer;
import dev.redstone.hairphysics.client.render.MaskedSkinTextureManager;
import dev.redstone.hairphysics.client.render.SkinTextureCache;
//? if fabric_like {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
//?}
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
//? if >=1.21.9
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
//? if <1.21.9
/*import net.minecraft.client.renderer.entity.player.PlayerRenderer;
*/
//? if neoforge && <1.21.9
/*import net.minecraft.client.resources.PlayerSkin;
*/
//? if forge && >=1.20.2 && <1.21.9
/*import net.minecraft.client.resources.PlayerSkin;
*/
//? if forge_like && >=1.21.9
/*import net.minecraft.world.entity.player.PlayerModelType;
*/
//? if neoforge
/*import net.neoforged.bus.api.IEventBus;
*/
//? if neoforge
/*import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
*/
//? if neoforge && >=1.20.5
/*import net.neoforged.neoforge.client.event.ClientTickEvent;
*/
//? if neoforge && <1.20.5
/*import net.neoforged.neoforge.event.TickEvent;
*/
//? if neoforge
/*import net.neoforged.neoforge.client.event.EntityRenderersEvent;
*/
//? if neoforge
/*import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
*/
//? if neoforge
/*import net.neoforged.neoforge.common.NeoForge;
*/
//? if forge
/*import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
*/
//? if forge
/*import net.minecraftforge.client.event.EntityRenderersEvent;
*/
//? if forge
/*import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
*/
//? if forge && <1.21.9
/*import net.minecraftforge.common.MinecraftForge;
*/
//? if forge
/*import net.minecraftforge.event.TickEvent;
*/
//? if forge && <1.21.9
/*import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
*/
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//? if fabric_like
public class HairphysicsClient implements ClientModInitializer {
//? if forge_like
/*public class HairphysicsClient {
*/
    public static final String MOD_ID = "hairphysics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static KeyMapping EDITOR_KEY;

    public static boolean shouldDisplayHairFor(AbstractClientPlayer player) {
        Minecraft client = Minecraft.getInstance();
        return client.player != null && player.getUUID().equals(client.player.getUUID());
    }

    private static KeyMapping createEditorKey() {
        return new KeyMapping(
            "key.hairphysics.editor",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
//? if >=1.21.9
            KeyMapping.Category.MISC
//? if <1.21.9
            /*KeyMapping.CATEGORY_MISC
            */
        );
    }

    public static void onClientTick(Minecraft client) {
        PhysicsTickHandler.onClientTick(client);
        if (EDITOR_KEY != null
                && EDITOR_KEY.consumeClick()
                && client.screen == null
                && client.player != null) {
            client.setScreen(new HairEditorScreen());
        }
    }

    public static void clearClientState() {
        PhysicsTickHandler.clearAll();
        SkinMetadataLoader.clearCache();
        MaskedSkinTextureManager.clearAll();
        SkinTextureCache.clearAll();
        LOGGER.info("[HairPhysics] Cleared on disconnect.");
    }

//? if fabric_like {
    @Override
    public void onInitializeClient() {
        LOGGER.info("[HairPhysics] Initializing client...");

        EDITOR_KEY = KeyBindingHelper.registerKeyBinding(createEditorKey());

        ClientTickEvents.END_CLIENT_TICK.register(HairphysicsClient::onClientTick);

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
            (entityType, renderer, registrationHelper, context) -> {
//? if >=1.21.9 {
                if (renderer instanceof AvatarRenderer playerRenderer) {
                    registrationHelper.register(new HairFeatureRenderer(playerRenderer));
                }
//?} else {
                /*if (renderer instanceof PlayerRenderer playerRenderer) {
                    registrationHelper.register(new HairFeatureRenderer(playerRenderer));
                }
                *///?}
            }
        );

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clearClientState());

        LOGGER.info("[HairPhysics] Ready. Press G to open Hair Editor.");
    }
//?}

//? if neoforge && >=1.21.9 {
    /*public static void registerNeoForge(IEventBus modBus) {
        LOGGER.info("[HairPhysics] Initializing client...");
        modBus.addListener(RegisterKeyMappingsEvent.class, HairphysicsClient::registerNeoForgeKey);
        modBus.addListener(EntityRenderersEvent.AddLayers.class, HairphysicsClient::registerNeoForgeLayers);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, HairphysicsClient::onNeoForgeClientTick);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, HairphysicsClient::onNeoForgeDisconnect);
        LOGGER.info("[HairPhysics] Ready. Press G to open Hair Editor.");
    }

    private static void registerNeoForgeKey(RegisterKeyMappingsEvent event) {
        EDITOR_KEY = createEditorKey();
        event.register(EDITOR_KEY);
    }

    private static void registerNeoForgeLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType skin : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(skin);
            if (renderer != null) {
                renderer.addLayer(new HairFeatureRenderer(renderer));
            }
        }
    }

    private static void onNeoForgeClientTick(ClientTickEvent.Post event) {
        onClientTick(Minecraft.getInstance());
    }

    private static void onNeoForgeDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        clearClientState();
    }
    *///?}

//? if neoforge && >=1.21.4 && <1.21.9 {
    /*public static void registerNeoForge(IEventBus modBus) {
        LOGGER.info("[HairPhysics] Initializing client...");
        modBus.addListener(RegisterKeyMappingsEvent.class, HairphysicsClient::registerNeoForgeKey);
        modBus.addListener(EntityRenderersEvent.AddLayers.class, HairphysicsClient::registerNeoForgeLayers);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, HairphysicsClient::onNeoForgeClientTick);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, HairphysicsClient::onNeoForgeDisconnect);
        LOGGER.info("[HairPhysics] Ready. Press G to open Hair Editor.");
    }

    private static void registerNeoForgeKey(RegisterKeyMappingsEvent event) {
        EDITOR_KEY = createEditorKey();
        event.register(EDITOR_KEY);
    }

    private static void registerNeoForgeLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new HairFeatureRenderer(renderer));
            }
        }
    }

    private static void onNeoForgeClientTick(ClientTickEvent.Post event) {
        onClientTick(Minecraft.getInstance());
    }

    private static void onNeoForgeDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        clearClientState();
    }
    *///?}

//? if neoforge && >=1.20.5 && <1.21.4 {
    /*public static void registerNeoForge(IEventBus modBus) {
        LOGGER.info("[HairPhysics] Initializing client...");
        modBus.addListener(RegisterKeyMappingsEvent.class, HairphysicsClient::registerNeoForgeKey);
        modBus.addListener(EntityRenderersEvent.AddLayers.class, HairphysicsClient::registerNeoForgeLayers);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, HairphysicsClient::onNeoForgeClientTick);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, HairphysicsClient::onNeoForgeDisconnect);
        LOGGER.info("[HairPhysics] Ready. Press G to open Hair Editor.");
    }

    private static void registerNeoForgeKey(RegisterKeyMappingsEvent event) {
        EDITOR_KEY = createEditorKey();
        event.register(EDITOR_KEY);
    }

    private static void registerNeoForgeLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new HairFeatureRenderer(renderer));
            }
        }
    }

    private static void onNeoForgeClientTick(ClientTickEvent.Post event) {
        onClientTick(Minecraft.getInstance());
    }

    private static void onNeoForgeDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        clearClientState();
    }
    *///?}

//? if neoforge && <1.20.5 {
    /*public static void registerNeoForge(IEventBus modBus) {
        LOGGER.info("[HairPhysics] Initializing client...");
        modBus.addListener(RegisterKeyMappingsEvent.class, HairphysicsClient::registerNeoForgeKey);
        modBus.addListener(EntityRenderersEvent.AddLayers.class, HairphysicsClient::registerNeoForgeLayers);
        NeoForge.EVENT_BUS.addListener(HairphysicsClient::onNeoForgeClientTick);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, HairphysicsClient::onNeoForgeDisconnect);
        LOGGER.info("[HairPhysics] Ready. Press G to open Hair Editor.");
    }

    private static void registerNeoForgeKey(RegisterKeyMappingsEvent event) {
        EDITOR_KEY = createEditorKey();
        event.register(EDITOR_KEY);
    }

    private static void registerNeoForgeLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new HairFeatureRenderer(renderer));
            }
        }
    }

    private static void onNeoForgeClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            onClientTick(Minecraft.getInstance());
        }
    }

    private static void onNeoForgeDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        clearClientState();
    }
    *///?}

//? if forge && >=1.21.9 {
    /*public static void registerForge() {
        LOGGER.info("[HairPhysics] Initializing client...");
        RegisterKeyMappingsEvent.BUS.addListener(HairphysicsClient::registerForgeKey);
        EntityRenderersEvent.AddLayers.BUS.addListener(HairphysicsClient::registerForgeLayers);
        TickEvent.ClientTickEvent.Post.BUS.addListener(HairphysicsClient::onForgeClientTick);
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(HairphysicsClient::onForgeDisconnect);
        LOGGER.info("[HairPhysics] Ready. Press G to open Hair Editor.");
    }

    private static void registerForgeKey(RegisterKeyMappingsEvent event) {
        EDITOR_KEY = createEditorKey();
        event.register(EDITOR_KEY);
    }

    private static void registerForgeLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType modelType : event.getModelTypes()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(modelType);
            if (renderer != null) {
                renderer.addLayer(new HairFeatureRenderer(renderer));
            }
        }
    }

    private static void onForgeClientTick(TickEvent.ClientTickEvent.Post event) {
        onClientTick(Minecraft.getInstance());
    }

    private static void onForgeDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        clearClientState();
    }
    *///?}

//? if forge && >=1.21.6 && <1.21.9 {
    /*public static void registerForge() {
        LOGGER.info("[HairPhysics] Initializing client...");
        var modBusGroup = FMLJavaModLoadingContext.get().getModBusGroup();
        RegisterKeyMappingsEvent.getBus(modBusGroup).addListener(HairphysicsClient::registerForgeKey);
        EntityRenderersEvent.AddLayers.getBus(modBusGroup).addListener(HairphysicsClient::registerForgeLayers);
        TickEvent.ClientTickEvent.Post.BUS.addListener(HairphysicsClient::onForgeClientTick);
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(HairphysicsClient::onForgeDisconnect);
        LOGGER.info("[HairPhysics] Ready. Press G to open Hair Editor.");
    }

    private static void registerForgeKey(RegisterKeyMappingsEvent event) {
        EDITOR_KEY = createEditorKey();
        event.register(EDITOR_KEY);
    }

    private static void registerForgeLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getPlayerSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new HairFeatureRenderer(renderer));
            }
        }
    }

    private static void onForgeClientTick(TickEvent.ClientTickEvent.Post event) {
        onClientTick(Minecraft.getInstance());
    }

    private static void onForgeDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        clearClientState();
    }
    *///?}

//? if forge && >=1.21.4 && <1.21.6 {
    /*public static void registerForge() {
        LOGGER.info("[HairPhysics] Initializing client...");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(HairphysicsClient::registerForgeKey);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(HairphysicsClient::registerForgeLayers);
        MinecraftForge.EVENT_BUS.addListener(HairphysicsClient::onForgeClientTick);
        MinecraftForge.EVENT_BUS.addListener(HairphysicsClient::onForgeDisconnect);
        LOGGER.info("[HairPhysics] Ready. Press G to open Hair Editor.");
    }

    private static void registerForgeKey(RegisterKeyMappingsEvent event) {
        EDITOR_KEY = createEditorKey();
        event.register(EDITOR_KEY);
    }

    private static void registerForgeLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getPlayerSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new HairFeatureRenderer(renderer));
            }
        }
    }

    private static void onForgeClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            onClientTick(Minecraft.getInstance());
        }
    }

    private static void onForgeDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        clearClientState();
    }
    *///?}

//? if forge && >=1.21 && <1.21.4 {
    /*public static void registerForge() {
        LOGGER.info("[HairPhysics] Initializing client...");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(HairphysicsClient::registerForgeKey);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(HairphysicsClient::registerForgeLayers);
        MinecraftForge.EVENT_BUS.addListener(HairphysicsClient::onForgeClientTick);
        MinecraftForge.EVENT_BUS.addListener(HairphysicsClient::onForgeDisconnect);
        LOGGER.info("[HairPhysics] Ready. Press G to open Hair Editor.");
    }

    private static void registerForgeKey(RegisterKeyMappingsEvent event) {
        EDITOR_KEY = createEditorKey();
        event.register(EDITOR_KEY);
    }

    private static void registerForgeLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getPlayerSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new HairFeatureRenderer(renderer));
            }
        }
    }

    private static void onForgeClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            onClientTick(Minecraft.getInstance());
        }
    }

    private static void onForgeDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        clearClientState();
    }
    *///?}

//? if forge && >=1.20.2 && <1.21 {
    /*public static void registerForge() {
        LOGGER.info("[HairPhysics] Initializing client...");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(HairphysicsClient::registerForgeKey);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(HairphysicsClient::registerForgeLayers);
        MinecraftForge.EVENT_BUS.addListener(HairphysicsClient::onForgeClientTick);
        MinecraftForge.EVENT_BUS.addListener(HairphysicsClient::onForgeDisconnect);
        LOGGER.info("[HairPhysics] Ready. Press G to open Hair Editor.");
    }

    private static void registerForgeKey(RegisterKeyMappingsEvent event) {
        EDITOR_KEY = createEditorKey();
        event.register(EDITOR_KEY);
    }

    private static void registerForgeLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new HairFeatureRenderer(renderer));
            }
        }
    }

    private static void onForgeClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            onClientTick(Minecraft.getInstance());
        }
    }

    private static void onForgeDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        clearClientState();
    }
    *///?}

//? if forge && <1.20.2 {
    /*public static void registerForge() {
        LOGGER.info("[HairPhysics] Initializing client...");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(HairphysicsClient::registerForgeKey);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(HairphysicsClient::registerForgeLayers);
        MinecraftForge.EVENT_BUS.addListener(HairphysicsClient::onForgeClientTick);
        MinecraftForge.EVENT_BUS.addListener(HairphysicsClient::onForgeDisconnect);
        LOGGER.info("[HairPhysics] Ready. Press G to open Hair Editor.");
    }

    private static void registerForgeKey(RegisterKeyMappingsEvent event) {
        EDITOR_KEY = createEditorKey();
        event.register(EDITOR_KEY);
    }

    private static void registerForgeLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new HairFeatureRenderer(renderer));
            }
        }
    }

    private static void onForgeClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            onClientTick(Minecraft.getInstance());
        }
    }

    private static void onForgeDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        clearClientState();
    }
    *///?}
}
