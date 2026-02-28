package nl.elec332.minecraft.loader.impl;

import nl.elec332.minecraft.loader.api.discovery.IAnnotationData;
import nl.elec332.minecraft.loader.api.modloader.IModFileResource;
import nl.elec332.minecraft.loader.api.modloader.IModLoader;
import nl.elec332.minecraft.loader.api.modloader.MappingType;
import nl.elec332.minecraft.loader.api.service.ModServiceLoader;
import nl.elec332.minecraft.loader.util.IClassTransformer;
import nl.elec332.minecraft.loader.util.IMappingProvider;
import nl.elec332.minecraft.repackaged.net.neoforged.srgutils.IMappingBuilder;
import nl.elec332.minecraft.repackaged.net.neoforged.srgutils.IMappingFile;
import nl.elec332.minecraft.repackaged.net.neoforged.srgutils.INamedMappingFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixin;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Created by Elec332 on 08-03-2024
 */
final class MappingTransformer implements IClassTransformer {

    private static final Attributes.Name MAPPINGS = new Attributes.Name("Mappings");
    private static final Logger LOGGER = LogManager.getLogger("ElecLoader Remapper");
    private static final IMappingFile NULL_MAPPINGS = IMappingBuilder.create().build().getMap("left", "right");

    static void register(Consumer<IClassTransformer> registry, Function<Type, Set<IAnnotationData>> dataHandler) {
        MappingType loaderTarget = IModLoader.INSTANCE.getMappingTarget();
        MappingType runtime = IModLoader.INSTANCE.isDevelopmentEnvironment() ? MappingType.NAMED : loaderTarget;
        Set<IMappingProvider> mappers = ModServiceLoader.loadService(IMappingProvider.class, IMappingProvider.class.getClassLoader());
        IModLoader.INSTANCE.getModFiles().forEach(f -> {
            Optional<IModFileResource> mff = f.findResource(JarFile.MANIFEST_NAME);
            MappingType type;
            boolean hasMixedMappings;

            if (mff.isPresent()) {
                try (InputStream is = mff.get().open()) {
                    Manifest mf = new Manifest(is);
                    String value = mf.getMainAttributes().getValue(MAPPINGS);
                    type = MappingType.fromString(value);
                    hasMixedMappings = MappingType.isMixed(value);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                type = null;
                hasMixedMappings = false;
            }
            MappingType fileType = MappingType.checkValue(type);
            Set<String> allClasses = new HashSet<>();
            f.getClasses().forEach(cd -> allClasses.add(cd.clazz().getInternalName()));

            Set<String> remapTargets = new HashSet<>();
            Map<MappingType, IMappingFile> mappings = new EnumMap<>(MappingType.class);
            mappers.forEach(p -> p.registerMappings(f, runtime, type, new IMappingProvider.Registry() {

                @Override
                public void registerMappings(MappingType from, MappingType to, IMappingFile maps, Collection<String> targets) {
                    if (Objects.requireNonNull(from) == Objects.requireNonNull(to)) {
                        throw new IllegalArgumentException();
                    }
                    if (to == fileType) {
                        to = from;
                        from = fileType;
                        maps = maps.reverse();
                    }
                    if (from != fileType) {
                        throw new UnsupportedOperationException("One side of the mapping must be " + fileType);
                    }
                    if (mappings.containsKey(to)) {
                        maps = maps.merge(Objects.requireNonNull(mappings.get(to)));
                    }
                    mappings.put(to, maps);
                    if (targets == null || targets.isEmpty()) {
                        remapTargets.addAll(allClasses);
                    } else {
                        remapTargets.addAll(targets);
                    }
                }

                @Override
                public void registerMappings(INamedMappingFile mapper, Collection<String> targets) {
                    Set<String> names = new HashSet<>(mapper.getNames());
                    if (!names.contains(fileType.name())) {
                        return;
                    }
                    if (names.size() != mapper.getNames().size()) {
                        throw new IllegalStateException("Duplicate names in mappings aren't properly supported unfortunately...");
                    }
                    names.remove(fileType.name());
                    names.forEach(name -> {
                        MappingType type = Objects.requireNonNull(MappingType.fromString(name));
                        if (type == runtime) {
                            registerMappings(fileType, type, mapper.getMap(fileType.name(), name), targets);
                        }
                    });
                }

            }));
            if (type == null && mappings.isEmpty()) {
                LOGGER.debug("Skipping remapping for file {} as it doesn't contain remapping information", f);
                return;
            }
            if (hasMixedMappings && IModLoader.INSTANCE.isDevelopmentEnvironment() && loaderTarget != MappingType.NAMED) {
                LOGGER.warn("File {} was compiled with mixed mappings. Unless this file was otherwise deobfed it is unlikely to work.", f);
            }
            if (fileType == runtime) {
                LOGGER.debug("Skipping remapping for file {} as it's compiled mapping matches the runtime mapping", f);
                return;
            }
            if (fileType == MappingType.NAMED && mappings.isEmpty()) {
                throw new RuntimeException("File " + f + " was compiled for deobf, and no mappings were found. Are you sure you aren't trying to run a dev version?");
            }
            IMappingFile target = Objects.requireNonNull(mappings.get(runtime));

            if (IModLoader.INSTANCE.getModLoaderType() == IModLoader.Type.FABRIC || IModLoader.INSTANCE.getModLoaderType() == IModLoader.Type.QUILT) {
                dataHandler.apply(org.objectweb.asm.Type.getType(Mixin.class)).forEach(d -> {
                    if (remapTargets.contains(d.getClassType().getInternalName())) {
                        throw new UnsupportedOperationException("Due to limitations in the Fabric Loader it is not possible to remap Mixins :(");
                    }
                });
            }
            registry.accept(new MappingTransformer(target, fileType, runtime, remapTargets));
            LOGGER.debug("Registered remapper for file: {}", f);
        });
    }

    private MappingTransformer(IMappingFile mappings, MappingType source, MappingType target, Set<String> targets) {
        this.source = Objects.requireNonNull(source);
        this.target = Objects.requireNonNull(target);
        if (source == target) {
            this.targets = Collections.emptySet();
            this.mappings = NULL_MAPPINGS;
        } else {
            this.targets = Objects.requireNonNull(targets);
            this.mappings = Objects.requireNonNull(mappings);
        }
    }

    private final IMappingFile mappings;
    private final MappingType source;
    private final MappingType target;
    private final Set<String> targets;

    @Override
    public String getName() {
        return "mappingtransformer_" + source.toString().toLowerCase(Locale.ROOT) + "_to_" + target.toString().toLowerCase(Locale.ROOT) + "_" + targets.hashCode();
    }

    @Override
    public Set<String> getTargetClasses() {
        return targets;
    }

    @Override //TODO: Save my sanity
    public boolean processClass(ClassNode classNode) {
        if (targets.isEmpty()) {
            return false;
        }

        ClassNode clone = new ClassNode();
        ClassRemapper remapper = new ClassRemapper(clone, new MappingRemapper());
        classNode.accept(remapper);

        /////////////////////////////////////////////
        //         Please look away                //
        /////////////////////////////////////////////
        try {
            for (Field f : ClassNode.class.getDeclaredFields()) {
                f.set(classNode, f.get(clone));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        /////////////////////////////////////////////

        return true;
    }

    private class MappingRemapper extends Remapper {

        @Override
        public String mapPackageName(String name) {
            return mappings.remapPackage(name);
        }

        @Override
        public String map(String internalName) {
            return mappings.remapClass(internalName);
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            IMappingFile.IClass cls = mappings.getClass(owner);
            if (cls != null) {
                return cls.remapMethod(name, descriptor);
            }
            return super.mapMethodName(owner, name, descriptor);
        }

        @Override
        public String mapFieldName(String owner, String name, String descriptor) {
            IMappingFile.IClass cls = mappings.getClass(owner);
            if (cls != null) {
                return cls.remapField(name);
            }
            return super.mapFieldName(owner, name, descriptor);
        }

        @Override
        public String mapRecordComponentName(String owner, String name, String descriptor) {
            return this.mapFieldName(owner, name, descriptor);
        }

    }

}
