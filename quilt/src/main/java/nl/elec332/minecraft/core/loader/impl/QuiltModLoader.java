package nl.elec332.minecraft.loader.impl;

import nl.elec332.minecraft.loader.abstraction.PathModFile;
import nl.elec332.minecraft.loader.api.modloader.IModFile;
import nl.elec332.minecraft.loader.api.modloader.IModMetaData;
import nl.elec332.minecraft.loader.api.modloader.MappingType;
import nl.elec332.minecraft.loader.api.version.IVersion;
import nl.elec332.minecraft.loader.api.version.IVersionFactory;
import nl.elec332.minecraft.loader.impl.fabriclike.AbstractFabricBasedModLoader;
import nl.elec332.minecraft.repackaged.org.apache.maven.artifact.versioning.ArtifactVersion;
import nl.elec332.minecraft.repackaged.org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.ModMetadata;
import org.quiltmc.loader.api.QuiltLoader;

/**
 * Created by Elec332 on 12-02-2024
 */
final class QuiltModLoader extends AbstractFabricBasedModLoader<ModContainer> {

    public QuiltModLoader() {
        super(QuiltLoader.getAllMods(), mc -> PathModFile.of(mc.rootPath()), mc -> mc.metadata().id());
        try {
            Class.forName("org.quiltmc.loader.api.entrypoint.PreLaunchEntrypoint");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Nope");
        }
    }

    @Override
    protected IModMetaData getModMeta(ModContainer container, IModFile file) {
        final ModMetadata metadata = container.metadata();
        final IVersion version = IVersionFactory.INSTANCE.createVersion(metadata.version().raw());
        return new IModMetaData() {

            @Override
            public String getModId() {
                return metadata.id();
            }

            @Override
            public String getModName() {
                return metadata.name();
            }

            @Override
            public String getDescription() {
                return metadata.description();
            }

            @Override
            public IVersion getVersion() {
                return version;
            }

            @Override
            public String getNamespace() {
                return metadata.group();
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
        return "Quilt";
    }

    @Override
    public Type getModLoaderType() {
        return Type.QUILT;
    }

    @Override
    public MappingType getMappingTarget() {
        return MappingType.FABRIC_INTERMEDIARY;
    }

}
