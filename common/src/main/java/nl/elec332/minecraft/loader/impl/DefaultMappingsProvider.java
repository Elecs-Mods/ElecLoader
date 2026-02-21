package nl.elec332.minecraft.loader.impl;

import nl.elec332.minecraft.loader.api.modloader.IModFile;
import nl.elec332.minecraft.loader.api.modloader.MappingType;
import nl.elec332.minecraft.loader.util.IMappingProvider;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Elec332 on 18-03-2024
 */
public final class DefaultMappingsProvider implements IMappingProvider {

    @Override
    public void registerMappings(IModFile file, MappingType runtime, @Nullable MappingType manifestType, Registry registry) {
        file.scanFile("mappings", (pathName, resource) -> {
            if (pathName.endsWith(".tsrg")) {
                IMappingProvider.readMappings(resource).ifPresent(m -> {
                    Set<String> targets = new HashSet<>();
                    file.findResource(pathName.replace(".tsrg", ".targets")).ifPresent(tp -> {
                        try (InputStream is = tp.open()) {
                            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                            targets.addAll(br.lines().toList());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                    registry.registerMappings(m, targets);
                });
            }
        });
    }

}
