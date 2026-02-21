package nl.elec332.minecraft.loader.impl;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import nl.elec332.minecraft.loader.abstraction.AbstractModFile;
import nl.elec332.minecraft.loader.abstraction.AbstractModLoader;
import nl.elec332.minecraft.loader.abstraction.PathModFile;
import nl.elec332.minecraft.loader.abstraction.PathModFileResource;
import nl.elec332.minecraft.loader.api.modloader.IModFileResource;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by Elec332 on 13-02-2026
 */
final class Neo20ModLoader extends AbstractNeoModLoader {

    public Neo20ModLoader() {
    }

    @Override
    protected void findMods(AbstractModLoader<ModInfo>.ModFileMapper mfm) {
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
    }

    @Override
    protected Dist getNeoDist() {
        return FMLLoader.getDist();
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

}
