package dev.redstone.hairphysics;

import dev.redstone.hairphysics.client.HairphysicsClient;
//? if fabric_like
import net.fabricmc.api.ModInitializer;
//? if neoforge {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
*///?}
//? if forge
/*import net.minecraftforge.fml.common.Mod;
*/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//? if forge_like
/*@Mod(Hairphysics.FORGE_MOD_ID)
*///? if fabric_like
public class Hairphysics implements ModInitializer {
//? if forge_like
/*public class Hairphysics {
*/
    public static String MOD_ID = "hair-physics";
    public static final String FORGE_MOD_ID = "hair_physics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

//? if fabric_like {
    @Override
    public void onInitialize() {
        init();
    }
//?}

//? if neoforge {
    /*public Hairphysics(IEventBus modBus) {
        init();
        HairphysicsClient.registerNeoForge(modBus);
    }
    *///?}

//? if forge {
    /*public Hairphysics() {
        init();
        HairphysicsClient.registerForge();
    }
    *///?}

    private static void init() {
        LOGGER.info("bros hair is starting up");
    }
}
