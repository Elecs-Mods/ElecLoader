package nl.elec332.minecraft.loader.impl;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import nl.elec332.minecraft.loader.abstraction.AbstractModLoader;
import nl.elec332.minecraft.loader.api.discovery.IAnnotationData;
import nl.elec332.minecraft.loader.api.discovery.IAnnotationDataHandler;
import nl.elec332.minecraft.loader.api.modloader.IModContainer;
import nl.elec332.minecraft.loader.api.modloader.IModFile;
import nl.elec332.minecraft.loader.api.modloader.IModMetaData;
import nl.elec332.minecraft.loader.api.modloader.MappingType;
import nl.elec332.minecraft.loader.api.version.IVersion;
import nl.elec332.minecraft.loader.api.version.IVersionFactory;
import nl.elec332.minecraft.loader.impl.neolang.NeoModContainer;

import java.nio.file.Path;
import java.util.Set;

/**
 * Created by Elec332 on 12-02-2026
 */
public abstract class AbstractNeoModLoader extends AbstractModLoader<ModInfo> {

    public AbstractNeoModLoader() {
        final AbstractModLoader<ModInfo>.ModFileMapper mfm = new AbstractModLoader<ModInfo>.ModFileMapper();
        findMods(mfm);
        identifyPackages(mfm, null);
    }

    protected abstract void findMods(AbstractModLoader<ModInfo>.ModFileMapper mfm);

    protected abstract Dist getNeoDist();

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
        mod = mod.map(modContainer -> modContainer instanceof NeoModContainer ? modContainer : null);
        if (mod.isPresent()) {
            return ((NeoModContainer) mod.get()).elecModContainer;
        }
        return super.createModContainer(meta, packs);
    }

    @Override
    public String getModLoaderName() {
        return "NeoForge";
    }

    @Override
    public Type getModLoaderType() {
        return Type.NEOFORGE;
    }

    @Override
    public MappingType getMappingTarget() {
        return MappingType.NAMED;
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
            if (!getNeoDist().toString().equals(enumHolder.value())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public nl.elec332.minecraft.loader.api.distmarker.Dist getDist() {
        return getNeoDist().isClient() ? nl.elec332.minecraft.loader.api.distmarker.Dist.CLIENT : nl.elec332.minecraft.loader.api.distmarker.Dist.DEDICATED_SERVER;
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
        try {
            return ModLoader.hasErrors();
        } catch (Exception e) {
            try {
                return !((boolean) ModLoader.class.getDeclaredMethod("isLoadingStateValid").invoke(null)); //Old pre-1.21 method
            } catch (Exception e2) {
                throw new RuntimeException(e2);
            }
        }
    }

}
