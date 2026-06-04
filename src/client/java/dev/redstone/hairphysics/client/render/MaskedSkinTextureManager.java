package dev.redstone.hairphysics.client.render;

import dev.redstone.hairphysics.client.HairphysicsClient;
import dev.redstone.hairphysics.client.data.HairDefinition;
import dev.redstone.hairphysics.client.data.HairStrand;
import dev.redstone.hairphysics.client.data.SkinMetadataLoader;
import dev.redstone.hairphysics.client.data.SkinRegion;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;




public final class MaskedSkinTextureManager {

    private static final Map<String, Identifier> CACHE = new HashMap<>();
    private static final Set<String> FAILED = new HashSet<>();

    private MaskedSkinTextureManager() {}

    public static Identifier getMaskedSkin(AbstractClientPlayerEntity player, Identifier originalSkin, String sourceUrl) {
        UUID uuid = player.getUuid();
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
        Identifier cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        if (FAILED.contains(key)) {
            return originalSkin;
        }

        MinecraftClient client = MinecraftClient.getInstance();
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

            Identifier maskedId = Identifier.of(
                HairphysicsClient.MOD_ID,
                "masked_skin/" + uuid.toString().replace("-", "") + "/" + Integer.toUnsignedString(signature.hashCode(), 36)
            );
            client.getTextureManager().registerTexture(maskedId, new NativeImageBackedTexture(maskedId::toString, image));
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

    private static NativeImage loadSkinImage(MinecraftClient client, Identifier originalSkin, String sourceUrl) {
        AbstractTexture texture = client.getTextureManager().getTexture(originalSkin);
        if (texture instanceof NativeImageBackedTexture nativeTexture) {
            NativeImage image = nativeTexture.getImage();
            if (image != null) {
                return image.applyToCopy(color -> color);
            }
        }

        Optional<Resource> resource = client.getResourceManager().getResource(originalSkin);
        if (resource.isPresent()) {
            try (InputStream stream = resource.get().getInputStream()) {
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
                image.setColorArgb(x, y, 0);
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
