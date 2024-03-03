package nl.elec332.minecraft.loader.api.modloader;

import nl.elec332.minecraft.loader.api.discovery.IAnnotationDataHandler;
import nl.elec332.minecraft.loader.api.distmarker.Dist;
import org.jetbrains.annotations.Nullable;

import java.util.ServiceLoader;
import java.util.Set;

/**
 * Created by Elec332 on 19-09-2023
 */
public interface IModLoader {

    IModLoader INSTANCE = ServiceLoader.load(IModLoader.class, IModLoader.class.getClassLoader())
            .findFirst()
            .orElseThrow(() -> new NullPointerException("Failed to load IModLoader implementation!"));

    String getModLoaderName();

    LoaderType getModLoaderType();

    /**
     * Finds a {@link IModContainer} by its mod-id
     *
     * @param id The mod-id
     * @return The {@link IModContainer}
     */
    @Nullable
    IModContainer getModContainer(String id);

    /**
     * Finds a mod's {@link IModMetaData} by its mod-id
     *
     * @param id The mod-id
     * @return The {@link IModMetaData}
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

    void enqueueDeferredWork(ModLoadingStage stage, IModContainer modContainer, Runnable runnable);

    boolean hasWrongSideOnly(String clazz, IAnnotationDataHandler annotationData);

    boolean isDevelopmentEnvironment();

    Dist getDist();

    Set<String> getUnownedPackages();

    default ClassLoader getModClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    enum LoaderType {

        FORGE, FABRIC, NEOFORGE, QUILT

    }

}
