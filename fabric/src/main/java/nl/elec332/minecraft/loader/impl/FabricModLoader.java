package nl.elec332.minecraft.loader.impl;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import nl.elec332.minecraft.loader.abstraction.PathModFile;
import nl.elec332.minecraft.loader.api.modloader.IModFile;
import nl.elec332.minecraft.loader.api.modloader.IModMetaData;
import nl.elec332.minecraft.loader.impl.fabriclike.AbstractFabricBasedModLoader;
import nl.elec332.minecraft.repackaged.org.apache.maven.artifact.versioning.ArtifactVersion;
import nl.elec332.minecraft.repackaged.org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/**
 * Created by Elec332 on 12-02-2024
 */
final class FabricModLoader extends AbstractFabricBasedModLoader<ModContainer> {

    public FabricModLoader() {
        super(FabricLoader.getInstance().getAllMods(), mc -> PathModFile.of(mc.getRootPaths()), mc -> mc.getMetadata().getId());
    }

    @Override
    protected IModMetaData getModMeta(ModContainer container, IModFile file) {
        final ModMetadata metadata = container.getMetadata();
        return new IModMetaData() {

            @Override
            public String getModId() {
                return metadata.getId();
            }

            @Override
            public String getModName() {
                return metadata.getName();
            }

            @Override
            public String getDescription() {
                return metadata.getDescription();
            }

            @Override
            public ArtifactVersion getVersion() {
                return new DefaultArtifactVersion(metadata.getVersion().getFriendlyString());
            }

            @Override
            public String getNamespace() {
                return String.join(",", metadata.getProvides());
            }

            @Override
            public IModFile getModFile() {
                return file;
            }

            @Override
            public String toString() {
                return toInfoString();
            }

        };
    }

    @Override
    public String getModLoaderName() {
        return "Fabric";
    }

    @Override
    public LoaderType getModLoaderType() {
        return LoaderType.FABRIC;
    }

    static {
        try {
            Class.forName("org.quiltmc.loader.api.entrypoint.PreLaunchEntrypoint");
            throw new RuntimeException("Nope");
        } catch (ClassNotFoundException e) {
            //
        }
    }

}
