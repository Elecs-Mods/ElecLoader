package nl.elec332.minecraft.loader.api.modloader;

import nl.elec332.minecraft.loader.api.discovery.IAnnotationDataHandler;
import nl.elec332.minecraft.loader.api.distmarker.Dist;
import nl.elec332.minecraft.loader.api.service.ModServiceLoader;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Created by Elec332 on 19-09-2023
 * <p>
 * Abstraction that can represent any modloader that this is being run on.
 */
public interface IModLoader {

    /**
     * The provided implementation
     */
    IModLoader INSTANCE = ModServiceLoader.loadAPIService(IModLoader.class);

    /**
     * Gets the name of the current modloader.
     *
     * @return The name of the current modloader
     */
    String getModLoaderName();

    /**
     * @return The current active modloader
     */
    Type getModLoaderType();

    /**
     * @return The normal mapping target for this modloader. <i>Does not care about {@link IModLoader#isDevelopmentEnvironment()}!</i>
     */
    MappingType getMappingTarget();

    /**
     * Finds an {@link IModContainer} by its mod-id.
     *
     * @param id The mod-id
     * @return The {@link IModContainer} for the provided mod-id
     */
    @Nullable
    IModContainer getModContainer(String id);

    /**
     * Finds a mod's {@link IModMetaData} by its mod-id.
     *
     * @param id The mod-id
     * @return The {@link IModMetaData} for the provided mod-id
     */
    @Nullable
    IModMetaData getModMetaData(String id);

    /**
     * @return All currently loaded mods
     */
    Set<IModContainer> getMods();

    /**
     * @return All files associated with currently loaded mods
     */
    Set<IModFile> getModFiles();

    /**
     * @return All loaded files identified as libraries
     */
    Set<IModFile> getLibraries();

    /**
     * Checks whether the provided class is marked to not load on the current {@link Dist}.
     *
     * @param clazz The class to be checked
     * @param annotationData The ASM annotation data gathered during loading
     * @return Whether the provided class is marked to not load on the current {@link Dist}
     */
    boolean hasWrongSideOnly(String clazz, IAnnotationDataHandler annotationData);

    /**
     * @return Whether we are currently running is a development environment
     */
    boolean isDevelopmentEnvironment();

    /**
     * @return The distribution the loader is currently running in
     */
    Dist getDist();

    /**
     * @return A collection of packages for which no owner could be identified
     */
    Set<String> getUnownedPackages();

    /**
     * @return The mod classloader
     */
    default ClassLoader getModClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * @return Whether the modloader has errored.
     */
    default boolean hasLoaderErrored() {
        return false;
    }

    /**
     * Modloader marker
     */
    enum Type {

        FORGE,
        FABRIC,
        NEOFORGE,
        QUILT

    }

}
