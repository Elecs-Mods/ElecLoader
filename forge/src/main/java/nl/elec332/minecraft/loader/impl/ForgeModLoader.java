package nl.elec332.minecraft.loader.impl;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.versions.forge.ForgeVersion;
import nl.elec332.minecraft.loader.abstraction.AbstractModFile;
import nl.elec332.minecraft.loader.abstraction.AbstractModLoader;
import nl.elec332.minecraft.loader.abstraction.PathModFile;
import nl.elec332.minecraft.loader.abstraction.PathModFileResource;
import nl.elec332.minecraft.loader.api.discovery.IAnnotationData;
import nl.elec332.minecraft.loader.api.discovery.IAnnotationDataHandler;
import nl.elec332.minecraft.loader.api.distmarker.Dist;
import nl.elec332.minecraft.loader.api.modloader.*;
import nl.elec332.minecraft.loader.api.version.IVersion;
import nl.elec332.minecraft.loader.api.version.IVersionFactory;
import nl.elec332.minecraft.loader.impl.forgelang.ForgeModContainer;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Created by Elec332 on 01-02-2024
 */
@SuppressWarnings("UnstableApiUsage")
final class ForgeModLoader extends AbstractModLoader<ModInfo> {

    public ForgeModLoader() {
        final AbstractModLoader<ModInfo>.ModFileMapper mfm = new ModFileMapper();
        FMLLoader.getLoadingModList().getMods().forEach(mi -> mfm.add(mi, new AbstractModFile() {

            final ModFile mf = mi.getOwningFile().getFile();

            @Override
            public void scanFile(String startFolder, IModFileResource.Visitor consumer) {
                Path root = mf.getSecureJar().getRootPath();
                PathModFileResource resource = new PathModFileResource(null, true);
                PathModFile.iterate(startFolder, root, (p, a) -> a.isRegularFile(), p -> {
                    resource.path = p;
                    consumer.visit(root.relativize(p).toString(), resource);
                });
            }

            @Override
            public Optional<IModFileResource> findResource(String file) {
                Path p = mf.findResource(file);
                if (Files.exists(p)) {
                    return Optional.of(new PathModFileResource(p, false));
                }
                return Optional.empty();
            }

            @Nullable
            @Override
            public String getComparableRootPath() {
                return mf.getFilePath().toUri().getPath();
            }

            @Override
            protected void scanFile() {
                scanFile((p, r) -> {
                    if (p.endsWith(".class")) {
                        this.classPaths.add(p);
                    }
                });
                this.pack.addAll(this.mf.getSecureJar().getPackages());
            }

            @Override
            public String getRootFileString() {
                return Objects.toString(this.mf.getSecureJar().getRootPath().toUri());
            }

            @Override
            public String toString() {
                return getRootFileString();
            }

        }));

        identifyPackages(mfm, null);
    }

    @Override
    protected IModMetaData getModMeta(final ModInfo container, final IModFile modFile) {
        final IVersion version = IVersionFactory.INSTANCE.createVersion(container.getVersion().toString());
        return new IModMetaData() {

            @Override
            public String getModId() {
                return container.getModId();
            }

            @Override
            public String getModName() {
                return container.getDisplayName();
            }

            @Override
            public String getDescription() {
                return container.getDescription();
            }

            @Override
            public IVersion getVersion() {
                return version;
            }

            @Override
            public String getNamespace() {
                return container.getNamespace();
            }

            @Override
            public IModFile getModFile() {
                return modFile;
            }

            @Override
            public String toString() {
                return toInfoString();
            }

        };
    }

    @Override
    protected IModContainer createModContainer(IModMetaData meta, Set<String> packs) {
        var mod = ModList.get().getModContainerById(meta.getModId());
        if (mod.isEmpty()) {
            throw new IllegalStateException("Couldn't find ModContainer for " + meta.getModId());
        }
        mod = mod.map(modContainer -> modContainer instanceof ForgeModContainer ? modContainer : null);
        if (mod.isPresent()) {
            return ((ForgeModContainer) mod.get()).elecModContainer;
        }
        return super.createModContainer(meta, packs);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
//        return Launcher.INSTANCE.environment().getProperty(IEnvironment.Keys.LAUNCHTARGET.get()).orElseThrow(NullPointerException::new).contains("Dev");
    }

    @Override
    public String getModLoaderName() {
        return "Forge";
    }

    @Override
    public Type getModLoaderType() {
        return Type.FORGE;
    }

    @Override
    public MappingType getMappingTarget() {
        try {
            if (Integer.parseInt(ForgeVersion.getVersion().split("\\.")[0]) > 50) { //Forge switched from SRG to named in 1.20.6
                return MappingType.NAMED;
            }
        } catch (Throwable e) {
            // Crashes below 1.20.1, but that's fine...
        }
        return MappingType.FORGE_SRG;
    }

    @Override
    public boolean hasWrongSideOnly(String clazz, IAnnotationDataHandler annotationData) {
        if (super.hasWrongSideOnly(clazz, annotationData)) {
            return true;
        }
        Set<IAnnotationData> ad = annotationData.getAnnotationsForClass(clazz).apply(org.objectweb.asm.Type.getType(OnlyIn.class));
        for (var a : ad) {
            if (!a.isClass()) {
                continue;
            }
            IAnnotationData.EnumHolder enumHolder = (IAnnotationData.EnumHolder) a.getAnnotationInfo().get("value");
            if (!FMLLoader.getDist().toString().equals(enumHolder.value())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Dist getDist() {
        return FMLLoader.getDist().isClient() ? Dist.CLIENT : Dist.DEDICATED_SERVER;
    }

    @Override
    public Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean hasLoaderErrored() {
        return !ModLoader.isLoadingStateValid();
    }

}
