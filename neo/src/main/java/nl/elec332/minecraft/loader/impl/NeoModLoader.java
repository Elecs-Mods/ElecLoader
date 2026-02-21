package nl.elec332.minecraft.loader.impl;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import nl.elec332.minecraft.loader.abstraction.AbstractModLoader;
import nl.elec332.minecraft.loader.impl.neolang.NeoModFile;

/**
 * Created by Elec332 on 01-02-2024
 */
final class NeoModLoader extends AbstractNeoModLoader {

    public NeoModLoader() {
    }

    @Override
    protected void findMods(AbstractModLoader<ModInfo>.ModFileMapper mfm) {
        FMLLoader.getCurrent().getLoadingModList().getMods().forEach(mi -> mfm.add(mi, new NeoModFile(mi.getOwningFile())));
    }

    @Override
    protected Dist getNeoDist() {
        return FMLLoader.getCurrent().getDist();
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.getCurrent().isProduction();
    }

}
