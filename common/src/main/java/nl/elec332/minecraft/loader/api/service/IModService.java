package nl.elec332.minecraft.loader.api.service;

import nl.elec332.minecraft.loader.api.modloader.IModLoader;

/**
 * Created by Elec332 on 28-02-2024
 * <p>
 * Use with care, as using this interface means the class gets instantiated before the check (as opposed to the {@link ModService} annotation).
 * This means that your class constructor is not allowed to reference any modloader-specific code!
 */
public interface IModService {

    /**
     * Checks whether this implementation is in a valid environment.
     * If this method returns false the implementation will be skipped by {@link ModServiceLoader}.
     *
     * @param loader The active modloader
     * @return Whether this implementation is in a valid environment
     */
    boolean isValidLoader(IModLoader.Type loader);

}
