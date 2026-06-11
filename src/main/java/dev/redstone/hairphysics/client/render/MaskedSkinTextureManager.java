package dev.redstone.hairphysics.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import dev.redstone.hairphysics.client.HairphysicsClient;
import dev.redstone.hairphysics.client.data.HairDefinition;
import dev.redstone.hairphysics.client.data.HairStrand;
import dev.redstone.hairphysics.client.data.SkinMetadataLoader;
import dev.redstone.hairphysics.client.data.SkinRegion;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
//? if >=1.21.11
import net.minecraft.resources.Identifier;
//? if <1.21.11
/*import net.minecraft.resources.ResourceLocation;
*/
import net.minecraft.server.packs.resources.Resource;




public final class MaskedSkinTextureManager {

//? if >=1.21.11
    private static final Map<String, Identifier> CACHE = new HashMap<>();
//? if <1.21.11
    /*private static final Map<String, ResourceLocation> CACHE = new HashMap<>();
    */
    private static final Set<String> FAILED = new HashSet<>();

    private MaskedSkinTextureManager() {}

//? if >=1.21.11
    public static Identifier getMaskedSkin(AbstractClientPlayer player, Identifier originalSkin, String sourceUrl) {
//? if <1.21.11
    /*public static ResourceLocation getMaskedSkin(AbstractClientPlayer player, ResourceLocation originalSkin, String sourceUrl) {
    */
        UUID uuid = player.getUUID();
        Optional<HairDefinition> defOpt = SkinMetadataLoader.loadForPlayer(uuid);
        if (defOpt.isEmpty() || !defOpt.get().hasStrands()) {
            return originalSkin;
        }

        HairDefinition definition = defOpt.get();
        String signature = signature(definition);
        if (signature.isEmpty()) {
            return originalSkin;
        }

        String key = uuid + "|" + originalSkin + "|" + signature;
//? if >=1.21.11
        Identifier cached = CACHE.get(key);
//? if <1.21.11
        /*ResourceLocation cached = CACHE.get(key);
        */
        if (cached != null) {
            return cached;
        }
        if (FAILED.contains(key)) {
            return originalSkin;
        }

        Minecraft client = Minecraft.getInstance();
        NativeImage image = loadSkinImage(client, originalSkin, sourceUrl);
        if (image == null) {
            FAILED.add(key);
            HairphysicsClient.LOGGER.debug("[HairPhysics] Could not read skin pixels for masking: {}", originalSkin);
            return originalSkin;
        }

        boolean masked = false;
        try {
            for (HairStrand strand : definition.strands) {
                if (strand.anchorOnly) continue;
                masked |= maskRegion(image, strand.skinRegion);
            }

            if (!masked) {
                image.close();
                return originalSkin;
            }

            String maskedPath = "masked_skin/" + uuid.toString().replace("-", "") + "/" + Integer.toUnsignedString(signature.hashCode(), 36);
//? if >=1.21.11
            Identifier maskedId = Identifier.fromNamespaceAndPath(HairphysicsClient.MOD_ID, maskedPath);
//? if >=1.21 && <1.21.11
            /*ResourceLocation maskedId = ResourceLocation.fromNamespaceAndPath(HairphysicsClient.MOD_ID, maskedPath);
            */
//? if <1.21
            /*ResourceLocation maskedId = new ResourceLocation(HairphysicsClient.MOD_ID, maskedPath);
            */
//? if >=1.21.5
            client.getTextureManager().register(maskedId, new DynamicTexture(maskedId::toString, image));
//? if <1.21.5
            /*client.getTextureManager().register(maskedId, new DynamicTexture(image));
            */
            CACHE.put(key, maskedId);
            return maskedId;
        } catch (Exception e) {
            image.close();
            FAILED.add(key);
            HairphysicsClient.LOGGER.warn("[HairPhysics] Failed to build masked skin for {}: {}", uuid, e.getMessage());
            return originalSkin;
        }
    }

    public static void clear(UUID uuid) {
        String prefix = uuid + "|";
        CACHE.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
        FAILED.removeIf(key -> key.startsWith(prefix));
    }

    public static void clearAll() {
        CACHE.clear();
        FAILED.clear();
    }

//? if >=1.21.11
    private static NativeImage loadSkinImage(Minecraft client, Identifier originalSkin, String sourceUrl) {
//? if <1.21.11
    /*private static NativeImage loadSkinImage(Minecraft client, ResourceLocation originalSkin, String sourceUrl) {
    */
        AbstractTexture texture = client.getTextureManager().getTexture(originalSkin);
        if (texture instanceof DynamicTexture nativeTexture) {
            NativeImage image = nativeTexture.getPixels();
            if (image != null) {
                return image.mappedCopy(color -> color);
            }
        }

        Optional<Resource> resource = client.getResourceManager().getResource(originalSkin);
        if (resource.isPresent()) {
            try (InputStream stream = resource.get().open()) {
                return NativeImage.read(stream);
            } catch (IOException ignored) {
                
            }
        }

        if (sourceUrl == null || sourceUrl.isBlank()) {
            return null;
        }

        try (InputStream stream = URI.create(sourceUrl).toURL().openStream()) {
            return NativeImage.read(stream);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean maskRegion(NativeImage image, SkinRegion region) {
        int minX = clamp(region.u, 0, image.getWidth());
        int minY = clamp(region.v, 0, image.getHeight());
        int maxX = clamp(region.u + region.width, 0, image.getWidth());
        int maxY = clamp(region.v + region.height, 0, image.getHeight());
        if (minX >= maxX || minY >= maxY) {
            return false;
        }

        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
//? if >=1.21.2
                image.setPixel(x, y, 0);
//? if <1.21.2
                /*image.setPixelRGBA(x, y, 0);
                */
            }
        }
        return true;
    }

    private static String signature(HairDefinition definition) {
        StringBuilder sb = new StringBuilder();
        for (HairStrand strand : definition.strands) {
            if (strand.anchorOnly) continue;
            SkinRegion region = strand.skinRegion;
            sb.append(strand.id).append(':')
                .append(region.u).append(',')
                .append(region.v).append(',')
                .append(region.width).append(',')
                .append(region.height).append(';');
        }
        return sb.toString();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
