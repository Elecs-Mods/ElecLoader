package nl.elec332.minecraft.loader.impl.neolang;

import net.neoforged.fml.jarcontents.JarResource;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import nl.elec332.minecraft.loader.abstraction.AbstractModFile;
import nl.elec332.minecraft.loader.api.modloader.IModFileResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by Elec332 on 12-02-2026
 */
@SuppressWarnings("UnstableApiUsage")
public class NeoModFile extends AbstractModFile {

    public NeoModFile(ModFileInfo mf) {
        this.mf = mf.getFile();
    }

    private final ModFile mf;

    @Override
    public void scanFile(String startFolder, IModFileResource.Visitor consumer) {
        NeoModFileResource neoResource = new NeoModFileResource(null, true);
        this.mf.getContents().visitContent(startFolder, (p, r) -> {
            neoResource.neoResource = r;
            consumer.visit(p, neoResource);
        });
    }

    @Override
    public Optional<IModFileResource> findResource(String file) {
        JarResource neoResource = this.mf.getContents().get(file);
        if (neoResource == null) {
            return Optional.empty();
        }
        return Optional.of(new NeoModFileResource(neoResource.retain(), false));
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
        this.pack.addAll(mf.getModuleDescriptor().packages());
//                this.pack.addAll(this.mf.getSecureJar().moduleDataProvider().descriptor().packages());
    }

    @Override
    public String getRootFileString() {
        return Objects.toString(this.mf.getContents().getPrimaryPath().toUri());
//                return Objects.toString(this.mf.getSecureJar().getRootPath().toUri());
    }

    @Override
    public String toString() {
        return getRootFileString();
    }

    private static final class NeoModFileResource implements IModFileResource {

        public NeoModFileResource(JarResource neoResource, boolean mutable) {
            this.mutable = mutable;
            this.neoResource = neoResource;
        }

        private final boolean mutable;
        public JarResource neoResource;

        @Override
        public InputStream open() throws IOException {
            return this.neoResource.open();
        }

        @Override
        public NeoModFileResource retain() {
            if (mutable) {
                return new NeoModFileResource(this.neoResource.retain(), false);
            } else {
                return this;
            }
        }

    }

}
