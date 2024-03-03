package nl.elec332.minecraft.loader.impl.fmod;

import nl.elec332.minecraft.loader.api.distmarker.Dist;
import nl.elec332.minecraft.loader.impl.ElecModLoader;
import nl.elec332.minecraft.loader.mod.Mod;
import nl.elec332.minecraft.repackaged.net.neoforged.bus.api.IEventBus;

/**
 * Created by Elec332 on 07-02-2024
 */
@Mod("elecloader")
public class ElecLoaderMod extends nl.elec332.minecraft.loader.ElecLoaderMod {

    public ElecLoaderMod(IEventBus bus, Dist dist) {
        super(bus, dist);
        ElecModLoader.checkEnvironment();
    }

}
