package nl.elec332.minecraft.loader.abstraction;

import nl.elec332.minecraft.loader.api.modloader.IModFileResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by Elec332 on 06-02-2026
 */
public final class PathModFileResource implements IModFileResource {

    public PathModFileResource(Path path, boolean mutable) {
        this.mutable = mutable;
        this.path = path;
    }

    private final boolean mutable;
    public Path path;

    @Override
    public InputStream open() throws IOException {
        return Files.newInputStream(path);
    }

    @Override
    public PathModFileResource retain() {
        if (mutable) {
            return new PathModFileResource(this.path, false);
        } else {
            return this;
        }
    }

}
