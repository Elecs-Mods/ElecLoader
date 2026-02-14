package nl.elec332.minecraft.loader.impl;

import nl.elec332.minecraft.loader.ElecLoaderMod;
import nl.elec332.minecraft.loader.abstraction.AbstractModLoader;
import nl.elec332.minecraft.loader.api.discovery.IAnnotationDataHandler;
import nl.elec332.minecraft.loader.api.distmarker.Dist;
import nl.elec332.minecraft.loader.api.modloader.*;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Elec332 on 06-02-2024
 */
public final class DeferredModLoader implements IModLoader {

    static DeferredModLoader INSTANCE;

    public DeferredModLoader() {
        if (INSTANCE != null) {
            throw new IllegalStateException();
        }
        Set<Object> loaded = Stream.of("NeoModLoader", "Neo20ModLoader", "ForgeModLoader", "FabricModLoader", "QuiltModLoader")
                .map(s -> {
                    try {
                        return Class.forName(LoaderConstants.PACKAGE_ROOT + ".impl." + s).getConstructor().newInstance();
                    } catch (Throwable e) {
                        if (e.getCause() != null) {
                            e = e.getCause();
                            String cn = e.getClass().getCanonicalName();
                            if (cn.equals("java.lang.NoClassDefFoundError")) {
                                String message = e.getMessage();
                                if (message.startsWith("net/minecraftforge") || message.startsWith("net/neoforged") || message.startsWith("net/fabricmc") || message.startsWith("org/quiltmc")) {
                                    return null;
                                }
                            }
                            if (cn.equals("java.lang.IncompatibleClassChangeError") || cn.equals("java.lang.NoSuchMethodError")) {
                                String message = e.getMessage();
                                if (message.contains("net.minecraftforge") || message.contains("net.neoforged") || message.contains("net.fabricmc") || message.contains("org.quiltmc")) {
                                    return null;
                                }
                            }
                        }
                        String message = e.getMessage();
                        if (message != null) {
                            if ("Nope".equals(message)) {
                                return null;
                            }
                            if (message.startsWith("nl.elec332.minecraft.loader.impl.")) {
                                return null; //ClassNotFound, compiled without support
                            }
                        }

                        throw new RuntimeException(e);
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (loaded.size() != 1) {
            throw new RuntimeException("Failed to properly load modloader! (Found " + loaded.size() + ")");
        }
        realModLoader = (AbstractModLoader<?>) loaded.iterator().next();
        if (realModLoader.getModMetaData(ElecLoaderMod.MODID) == null && !hasLoaderErrored()) {
            LoaderInitializer.INSTANCE.loaderModNotFound();
        }
        INSTANCE = this;
    }

    private final AbstractModLoader<?> realModLoader;

    void fillModContainers() {
        realModLoader.fillModContainers();
    }

    @Override
    public String getModLoaderName() {
        return realModLoader.getModLoaderName();
    }

    @Override
    public Type getModLoaderType() {
        return realModLoader.getModLoaderType();
    }

    @Override
    public MappingType getMappingTarget() {
        return Objects.requireNonNull(realModLoader.getMappingTarget());
    }

    @Nullable
    @Override
    public IModContainer getModContainer(String id) {
        if (!LoaderInitializer.INSTANCE.completedModList()) {
            throw new IllegalStateException();
        }
        return realModLoader.getModContainer(id);
    }

    @Override
    public @Nullable IModMetaData getModMetaData(String id) {
        return realModLoader.getModMetaData(id);
    }

    @Override
    public Set<IModContainer> getMods() {
        if (!LoaderInitializer.INSTANCE.completedModList()) {
            throw new IllegalStateException();
        }
        return realModLoader.getMods();
    }

    @Override
    public Set<IModFile> getModFiles() {
        return realModLoader.getModFiles();
    }

    @Override
    public Set<IModFile> getLibraries() {
        return realModLoader.getLibraries();
    }

    @Override
    public boolean hasWrongSideOnly(String clazz, IAnnotationDataHandler annotationData) {
        return realModLoader.hasWrongSideOnly(clazz, annotationData);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return realModLoader.isDevelopmentEnvironment();
    }

    @Override
    public Dist getDist() {
        return realModLoader.getDist();
    }

    @Override
    public Set<String> getUnownedPackages() {
        return realModLoader.getUnownedPackages();
    }

    @Override
    public Path getGameDir() {
        return realModLoader.getGameDir();
    }

    @Override
    public Path getConfigDir() {
        return realModLoader.getConfigDir();
    }

    @Override
    public ClassLoader getModClassLoader() {
        return realModLoader.getModClassLoader();
    }

    @Override
    public boolean hasLoaderErrored() {
        return realModLoader.hasLoaderErrored();
    }

}
