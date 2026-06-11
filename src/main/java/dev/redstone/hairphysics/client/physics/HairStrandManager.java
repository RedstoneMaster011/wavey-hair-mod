package dev.redstone.hairphysics.client.physics;

import dev.redstone.hairphysics.client.data.HairDefinition;
import dev.redstone.hairphysics.client.data.HairStrand;
import dev.redstone.hairphysics.client.data.SkinMetadataLoader;
import dev.redstone.hairphysics.client.data.StrandOrigin;
import dev.redstone.hairphysics.client.HairphysicsClient;
import java.util.*;
import net.minecraft.client.player.AbstractClientPlayer;







public class HairStrandManager {

    private static final double HEAD_CENTER_Y_OFFSET = 1.52;
    private static final double BODY_CENTER_Y_OFFSET = 1.125;
    private static final double ARM_CENTER_Y_OFFSET = 1.125;
    private static final double LEG_CENTER_Y_OFFSET = 0.375;
    private static final double ARM_CENTER_X_OFFSET = 0.3125;
    private static final double LEG_CENTER_X_OFFSET = 0.125;

    
    private final Map<UUID, List<StrandSimulation>> playerStrands = new HashMap<>();

    



    public List<StrandSimulation> getOrCreate(AbstractClientPlayer player) {
        UUID uuid = player.getUUID();
        if (playerStrands.containsKey(uuid)) {
            return playerStrands.get(uuid);
        }

        Optional<HairDefinition> defOpt = SkinMetadataLoader.loadForPlayer(uuid);
        if (defOpt.isEmpty() || !defOpt.get().hasStrands()) {
            playerStrands.put(uuid, List.of());
            return List.of();
        }

        HairDefinition def = defOpt.get();
        List<StrandSimulation> sims = new ArrayList<>();

        for (HairStrand strand : def.strands) {
            double[] rootPos = computeRootWorld(player, strand.origin);
            StrandSimulation sim = new StrandSimulation(strand, rootPos[0], rootPos[1], rootPos[2]);
            sims.add(sim);
            HairphysicsClient.LOGGER.debug("[HairPhysics] Created simulation for strand '{}' on player {}", strand.id, uuid);
        }

        playerStrands.put(uuid, sims);
        HairphysicsClient.LOGGER.info("[HairPhysics] Loaded {} hair strand simulation(s) for {}", sims.size(), uuid);
        return sims;
    }

    
    public List<StrandSimulation> get(UUID uuid) {
        return playerStrands.getOrDefault(uuid, List.of());
    }

    
    public void remove(UUID uuid) {
        playerStrands.remove(uuid);
    }

    
    public void clearAll() {
        playerStrands.clear();
    }

    





    public static double[] computeRootWorld(AbstractClientPlayer player, StrandOrigin origin) {
        BoneAnchor bone = BoneAnchor.from(origin.bone);
        double centerX = player.getX();
        double centerY = player.getY() + bone.centerYOffset;
        double centerZ = player.getZ();

        float yaw = (float) Math.toRadians(bone.usesHeadPitch ? player.getYHeadRot() : player.getVisualRotationYInDegrees());
        float pitch = bone.usesHeadPitch ? (float) Math.toRadians(player.getXRot()) : 0.0f;
        float cosYaw = (float) Math.cos(yaw);
        float sinYaw = (float) Math.sin(yaw);
        float cosPitch = (float) Math.cos(pitch);
        float sinPitch = (float) Math.sin(pitch);

        
        float lx = (float) (bone.centerXOffset + origin.offsetX);
        float ly = origin.offsetY;
        float lz = origin.offsetZ;

        double pitchedY = ly * cosPitch + lz * sinPitch;
        double pitchedZ = -ly * sinPitch + lz * cosPitch;

        
        double worldOffX = lx * cosYaw - pitchedZ * sinYaw;
        double worldOffZ = lx * sinYaw + pitchedZ * cosYaw;

        return new double[]{
            centerX + worldOffX,
            centerY + pitchedY,
            centerZ + worldOffZ
        };
    }

    private record BoneAnchor(double centerXOffset, double centerYOffset, boolean usesHeadPitch) {
        private static BoneAnchor from(String rawBone) {
            return switch (normalizeBone(rawBone)) {
                case "body" -> new BoneAnchor(0.0, BODY_CENTER_Y_OFFSET, false);
                case "left_arm" -> new BoneAnchor(ARM_CENTER_X_OFFSET, ARM_CENTER_Y_OFFSET, false);
                case "right_arm" -> new BoneAnchor(-ARM_CENTER_X_OFFSET, ARM_CENTER_Y_OFFSET, false);
                case "left_leg" -> new BoneAnchor(LEG_CENTER_X_OFFSET, LEG_CENTER_Y_OFFSET, false);
                case "right_leg" -> new BoneAnchor(-LEG_CENTER_X_OFFSET, LEG_CENTER_Y_OFFSET, false);
                default -> new BoneAnchor(0.0, HEAD_CENTER_Y_OFFSET, true);
            };
        }

        private static String normalizeBone(String rawBone) {
            if (rawBone == null) return "head";
            return switch (rawBone.trim().toLowerCase()) {
                case "body", "torso", "chest", "jacket" -> "body";
                case "left_arm", "leftarm", "l_arm", "left sleeve", "left_sleeve" -> "left_arm";
                case "right_arm", "rightarm", "r_arm", "right sleeve", "right_sleeve" -> "right_arm";
                case "left_leg", "leftleg", "l_leg", "left pants", "left_pants" -> "left_leg";
                case "right_leg", "rightleg", "r_leg", "right pants", "right_pants" -> "right_leg";
                default -> "head";
            };
        }
    }
}
