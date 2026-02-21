package nl.elec332.minecraft.loader.util;

import nl.elec332.minecraft.loader.api.modloader.IModFile;
import nl.elec332.minecraft.loader.api.modloader.IModFileResource;
import nl.elec332.minecraft.loader.api.modloader.MappingType;
import nl.elec332.minecraft.repackaged.net.neoforged.srgutils.IMappingFile;
import nl.elec332.minecraft.repackaged.net.neoforged.srgutils.INamedMappingFile;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by Elec332 on 08-03-2024
 */
public interface IMappingProvider {

    void registerMappings(IModFile file, MappingType runtime, @Nullable MappingType manifestType, Registry registry);

    static Optional<INamedMappingFile> readMappings(Path path) {
        if (path != null) {
            try (InputStream is = Objects.requireNonNull(Files.newInputStream(path))) {
                return Optional.of(INamedMappingFile.load(is));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return Optional.empty();
    }

    static Optional<INamedMappingFile> readMappings(IModFileResource resource) {
        if (resource != null) {
            try (InputStream is = Objects.requireNonNull(resource.open())) {
                return Optional.of(INamedMappingFile.load(is));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return Optional.empty();
    }

    interface Registry {

        default void registerMappings(MappingType from, MappingType to, IMappingFile mapper) {
            registerMappings(from, to, mapper, null);
        }

        void registerMappings(MappingType from, MappingType to, IMappingFile mapper, Collection<String> targets);

        default void registerMappings(INamedMappingFile mapper) {
            registerMappings(mapper, null);
        }

        void registerMappings(INamedMappingFile mapper, Collection<String> targets);

    }

}
