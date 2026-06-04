package dev.redstone.hairphysics.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;





public final class SkinPixelCache {
    private static final int MIN_VISIBLE_ALPHA = 24;
    private static final Map<Identifier, PixelData> CACHE = new HashMap<>();

    private SkinPixelCache() {}

    public static boolean isVisible(Identifier skinId, int x, int y) {
        PixelData data = CACHE.get(skinId);
        if (data == null) {
            data = load(skinId);
            if (data == null) {
                return true;
            }
            CACHE.put(skinId, data);
        }

        if (x < 0 || y < 0 || x >= data.width || y >= data.height) {
            return false;
        }
        return data.visible[y * data.width + x];
    }

    public static void clear() {
        CACHE.clear();
    }

    private static PixelData load(Identifier skinId) {
        MinecraftClient client = MinecraftClient.getInstance();
        NativeImage image = copyLoadedImage(client, skinId);
        if (image == null) {
            return null;
        }

        try {
            int width = image.getWidth();
            int height = image.getHeight();
            boolean[] visible = new boolean[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int alpha = (image.getColorArgb(x, y) >>> 24) & 0xFF;
                    visible[y * width + x] = alpha >= MIN_VISIBLE_ALPHA;
                }
            }
            return new PixelData(width, height, visible);
        } finally {
            image.close();
        }
    }

    private static NativeImage copyLoadedImage(MinecraftClient client, Identifier skinId) {
        AbstractTexture texture = client.getTextureManager().getTexture(skinId);
        if (texture instanceof NativeImageBackedTexture nativeTexture) {
            NativeImage image = nativeTexture.getImage();
            if (image != null) {
                return image.applyToCopy(color -> color);
            }
        }

        Optional<Resource> resource = client.getResourceManager().getResource(skinId);
        if (resource.isPresent()) {
            try (InputStream stream = resource.get().getInputStream()) {
                return NativeImage.read(stream);
            } catch (IOException ignored) {
                return null;
            }
        }

        return null;
    }

    private record PixelData(int width, int height, boolean[] visible) {}
}
