package dev.redstone.hairphysics.client.render;

import dev.redstone.hairphysics.client.data.SkinRegion;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;








public class SkinTextureCache {

    
    private static final Map<UUID, Identifier> SKIN_IDS     = new HashMap<>();
    private static final Map<UUID, Boolean>    SLIM_FLAGS   = new HashMap<>();

    
    public static void store(UUID uuid, Identifier skinId, boolean slim) {
        SKIN_IDS.put(uuid, skinId);
        SLIM_FLAGS.put(uuid, slim);
    }

    
    public static Identifier getLastSeenSkin(UUID uuid) {
        return SKIN_IDS.get(uuid);
    }

    
    public static boolean isSlim(UUID uuid) {
        return Boolean.TRUE.equals(SLIM_FLAGS.get(uuid));
    }

    public static void clearAll() {
        SKIN_IDS.clear();
        SLIM_FLAGS.clear();
    }

    



    public static float[] getUVs(SkinRegion region) {
        float s = 64.0f;
        return new float[]{
            region.u / s,
            region.v / s,
            (region.u + region.width) / s,
            (region.v + region.height) / s
        };
    }
}
