package nl.elec332.minecraft.loader.impl.quilt;

import nl.elec332.minecraft.loader.impl.LoaderInitializer;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.entrypoint.PreLaunchEntrypoint;

/**
 * Created by Elec332 on 18-02-2024
 */
public class QuiltPreLoader implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch(ModContainer mod) {
        LoaderInitializer.INSTANCE.checkEnvironment();
    }

}
