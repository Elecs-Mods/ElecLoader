package nl.elec332.minecraft.loader.impl.fabric;

import net.fabricmc.api.DedicatedServerModInitializer;
import nl.elec332.minecraft.loader.api.distmarker.Dist;
import nl.elec332.minecraft.loader.impl.fabriclike.FabricModStages;

/**
 * Created by Elec332 on 13-02-2024
 */
public class FabricServerInitializer implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        FabricModStages.discover();
        FabricModStages.init(Dist.DEDICATED_SERVER);
        FabricModStages.postInit();
    }

}
