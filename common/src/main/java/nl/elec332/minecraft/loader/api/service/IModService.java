package nl.elec332.minecraft.loader.api.service;

import nl.elec332.minecraft.loader.api.modloader.IModLoader;

/**
 * Created by Elec332 on 28-02-2024
 * <p>
 * USe with care, as using this interface means the class gets instantiated before the check (as opposed to @ModService)
 * This means that your class constructor is not allowed to can any modloader-specific code!
 */
public interface IModService {

    boolean isValidLoader(IModLoader.LoaderType loader);

}
