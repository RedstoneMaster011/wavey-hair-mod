package dev.redstone.hairphysics.client.physics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.util.List;




public class PhysicsTickHandler {

    public static final HairStrandManager MANAGER = new HairStrandManager();

    public static void onClientTick(MinecraftClient client) {
        if (client.world == null || client.player == null) return;

        tickPlayer(client.player);
    }

    private static void tickPlayer(AbstractClientPlayerEntity player) {
        List<StrandSimulation> sims = MANAGER.getOrCreate(player);
        if (sims.isEmpty()) return;

        
        double entX = player.getX();
        double entY = player.getY();
        double entZ = player.getZ();

        for (StrandSimulation sim : sims) {
            double[] root = HairStrandManager.computeRootWorld(player, sim.definition.origin);
            sim.tick(root[0], root[1], root[2], entX, entY, entZ, player);
        }
    }

    public static void clearAll() {
        MANAGER.clearAll();
    }
}
