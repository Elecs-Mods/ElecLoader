package nl.elec332.minecraft.loader.impl.neolang;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.IModLanguageLoader;
import net.neoforged.neoforgespi.language.ModFileScanData;

/**
 * Created by Elec332 on 06-02-2024
 */
public class NeoLanguageLoader implements IModLanguageLoader {

    @Override
    public String name() {
        return "elecjava";
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public ModContainer loadMod(IModInfo info, ModFileScanData scanResult, ModuleLayer layer) throws ModLoadingException {
        return new NeoModContainer(info, layer);
    }

}
