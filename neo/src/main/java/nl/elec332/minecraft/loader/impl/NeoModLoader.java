package nl.elec332.minecraft.loader.impl;

import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import nl.elec332.minecraft.loader.abstraction.AbstractModFile;
import nl.elec332.minecraft.loader.abstraction.AbstractModLoader;
import nl.elec332.minecraft.loader.api.discovery.IAnnotationData;
import nl.elec332.minecraft.loader.api.discovery.IAnnotationDataHandler;
import nl.elec332.minecraft.loader.api.distmarker.Dist;
import nl.elec332.minecraft.loader.api.modloader.*;
import nl.elec332.minecraft.loader.impl.neolang.NeoModContainer;
import nl.elec332.minecraft.repackaged.org.apache.maven.artifact.versioning.ArtifactVersion;
import nl.elec332.minecraft.repackaged.org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by Elec332 on 01-02-2024
 */
final class NeoModLoader extends AbstractModLoader<ModInfo> {

    public NeoModLoader() {
        final AbstractModLoader<ModInfo>.ModFileMapper mfm = new AbstractModLoader<ModInfo>.ModFileMapper();

        FMLLoader.getLoadingModList().getMods().forEach(mi -> mfm.add(mi, new AbstractModFile() {

            final ModFile mf = mi.getOwningFile().getFile();

            @Override
            public void scanFile(Consumer<Path> consumer) {
                this.mf.getProvider().scanFile(this.mf, consumer);
            }

            @Override
            public Optional<Path> findPath(String file) {
                Path p = mf.findResource(file);
                if (Files.exists(p)) {
                    return Optional.of(p);
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
                scanFile(p -> this.classPaths.add(p.toString()));
                this.pack.addAll(this.mf.getSecureJar().moduleDataProvider().descriptor().packages());
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
        ArtifactVersion version = new DefaultArtifactVersion(container.getVersion().toString());
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
            public ArtifactVersion getVersion() {
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
        mod = mod.map(modContainer -> modContainer instanceof NeoModContainer ? modContainer : null);
        if (mod.isPresent()) {
            return ((NeoModContainer) mod.get()).elecModContainer;
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
        return "NeoForge";
    }

    @Override
    public LoaderType getModLoaderType() {
        return LoaderType.NEOFORGE;
    }

    @Override
    public void enqueueDeferredWork(ModLoadingStage stage, IModContainer modContainer, Runnable runnable) {
        if (stage == ModLoadingStage.PRE_CONSTRUCT) {
            throw new UnsupportedOperationException();
        }
        net.neoforged.fml.ModLoadingStage mls = net.neoforged.fml.ModLoadingStage.values()[stage.ordinal() + 1];
        mls.getDeferredWorkQueue().enqueueWork(ModList.get().getModContainerById(modContainer.getModId()).get(), runnable);
    }

    @Override
    public boolean hasWrongSideOnly(String clazz, IAnnotationDataHandler annotationData) {
        if (super.hasWrongSideOnly(clazz, annotationData)) {
            return true;
        }
        Set<IAnnotationData> ad = annotationData.getAnnotationsForClass(clazz).apply(Type.getType(OnlyIn.class));
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

}
