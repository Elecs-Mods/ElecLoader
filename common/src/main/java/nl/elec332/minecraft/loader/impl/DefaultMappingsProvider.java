package nl.elec332.minecraft.loader.impl;

import nl.elec332.minecraft.loader.api.modloader.IModFile;
import nl.elec332.minecraft.loader.api.modloader.MappingType;
import nl.elec332.minecraft.loader.util.IMappingProvider;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Elec332 on 18-03-2024
 */
public final class DefaultMappingsProvider implements IMappingProvider {

    @Override
    public void registerMappings(IModFile file, MappingType runtime, @Nullable MappingType manifestType, Registry registry) {
        file.findPath("mappings/").ifPresent(mp -> {
            try(var stream = Files.walk(mp)) {
                stream.forEach(p -> {
                    String pathName = p.toString();
                    if (pathName.endsWith(".tsrg")) {
                        IMappingProvider.readMappings(p).ifPresent(m -> {
                            Set<String> targets = new HashSet<>();
                            file.findPath(pathName.replace(".tsrg", ".targets")).ifPresent(tp -> {
                                try {
                                    targets.addAll(Files.readAllLines(tp));
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                            registry.registerMappings(m, targets);
                        });
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

}
