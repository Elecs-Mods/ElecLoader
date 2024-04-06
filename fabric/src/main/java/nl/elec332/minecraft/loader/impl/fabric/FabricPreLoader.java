package nl.elec332.minecraft.loader.impl.fabric;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import nl.elec332.minecraft.loader.impl.LoaderInitializer;

/**
 * Created by Elec332 on 13-02-2024
 */
public class FabricPreLoader implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch() {
        LoaderInitializer.INSTANCE.checkEnvironment();
    }

}
