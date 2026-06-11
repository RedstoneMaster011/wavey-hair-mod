package dev.redstone.hairphysics.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
//? if >=1.21.11
import net.minecraft.resources.Identifier;
//? if <1.21.11
/*import net.minecraft.resources.ResourceLocation;
*/
import net.minecraft.server.packs.resources.Resource;





public final class SkinPixelCache {
    private static final int MIN_VISIBLE_ALPHA = 24;
//? if >=1.21.11
    private static final Map<Identifier, PixelData> CACHE = new HashMap<>();
//? if <1.21.11
    /*private static final Map<ResourceLocation, PixelData> CACHE = new HashMap<>();
    */

    private SkinPixelCache() {}

//? if >=1.21.11
    public static boolean isVisible(Identifier skinId, int x, int y) {
//? if <1.21.11
    /*public static boolean isVisible(ResourceLocation skinId, int x, int y) {
    */
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

//? if >=1.21.11
    private static PixelData load(Identifier skinId) {
//? if <1.21.11
    /*private static PixelData load(ResourceLocation skinId) {
    */
        Minecraft client = Minecraft.getInstance();
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
//? if >=1.21.2
                    int pixel = image.getPixel(x, y);
//? if <1.21.2
                    /*int pixel = image.getPixelRGBA(x, y);
                    */
                    int alpha = (pixel >>> 24) & 0xFF;
                    visible[y * width + x] = alpha >= MIN_VISIBLE_ALPHA;
                }
            }
            return new PixelData(width, height, visible);
        } finally {
            image.close();
        }
    }

//? if >=1.21.11
    private static NativeImage copyLoadedImage(Minecraft client, Identifier skinId) {
//? if <1.21.11
    /*private static NativeImage copyLoadedImage(Minecraft client, ResourceLocation skinId) {
    */
        AbstractTexture texture = client.getTextureManager().getTexture(skinId);
        if (texture instanceof DynamicTexture nativeTexture) {
            NativeImage image = nativeTexture.getPixels();
            if (image != null) {
                return image.mappedCopy(color -> color);
            }
        }

        Optional<Resource> resource = client.getResourceManager().getResource(skinId);
        if (resource.isPresent()) {
            try (InputStream stream = resource.get().open()) {
                return NativeImage.read(stream);
            } catch (IOException ignored) {
                return null;
            }
        }

        return null;
    }

    private record PixelData(int width, int height, boolean[] visible) {}
}
