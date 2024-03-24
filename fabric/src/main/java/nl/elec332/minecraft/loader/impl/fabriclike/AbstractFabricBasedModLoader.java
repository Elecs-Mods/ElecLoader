package nl.elec332.minecraft.loader.impl.fabriclike;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import nl.elec332.minecraft.loader.abstraction.AbstractModFile;
import nl.elec332.minecraft.loader.abstraction.AbstractModLoader;
import nl.elec332.minecraft.loader.abstraction.PathModFile;
import nl.elec332.minecraft.loader.api.discovery.IAnnotationData;
import nl.elec332.minecraft.loader.api.discovery.IAnnotationDataHandler;
import nl.elec332.minecraft.loader.api.distmarker.Dist;
import nl.elec332.minecraft.loader.api.modloader.IModContainer;
import nl.elec332.minecraft.loader.api.modloader.IModFile;
import nl.elec332.minecraft.loader.api.modloader.ModLoadingStage;
import nl.elec332.minecraft.loader.impl.LoaderConstants;

import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 12-02-2024
 */
public abstract class AbstractFabricBasedModLoader<T> extends AbstractModLoader<T> {

    public AbstractFabricBasedModLoader(Collection<T> mcs, Function<T, IModFile> fileGetter, Function<T, String> modIdGetter) {
        final AbstractModLoader<T>.ModFileMapper mfm = new AbstractModLoader<T>.ModFileMapper();

        mcs.forEach(mc -> mfm.add(mc, Objects.requireNonNull(fileGetter.apply(mc))));

        URLClassLoader uc = (URLClassLoader) getModClassLoader().getParent();
        Set<PathModFile> classPath = Arrays.stream(uc.getURLs())
                .filter(mfm::reduceLibraries)
                .map(PathModFile::of)
                .peek(AbstractModFile::getPackages)
                .collect(Collectors.toSet());

        mfm.checkLibraries(classPath);

        Map<PathModFile, Set<String>> classPathMods = classPath.stream()
                        .map(file -> {
                            Set<IModFile.RawAnnotationData> annotations = new HashSet<>();
                            scanAnnotations(file, new HashSet<>(), annotations);
                            Set<String> s = annotations.stream()
                                    .filter(ad -> ad.annotationType().equals(LoaderConstants.MODANNOTATION))
                                    .map(ad -> (String) ad.annotationData().get("value"))
                                    .collect(Collectors.toSet());
                            if (s.isEmpty()) {
                                return null;
                            }
                            return Map.entry(file, s);
                        }).filter(Objects::nonNull)
                        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        mfm.checkFiles((mc, file) -> {
            if (!(file instanceof PathModFile)) {
                return file;
            }
            String modId = modIdGetter.apply(mc);
            for (var entry : classPathMods.entrySet()) {
                if (entry.getValue().contains(modId)) {
                    entry.getValue().remove(modId);
                    return PathModFile.of((PathModFile) file, entry.getKey());
                }
            }
            return file;
        });

        classPathMods.forEach((file, mods) -> {
            if (!mods.isEmpty()) {
                throw new IllegalStateException();
            }
        });

        identifyPackages(mfm, classPath);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public Dist getDist() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? Dist.CLIENT : Dist.DEDICATED_SERVER;
    }

    @Override
    public boolean hasWrongSideOnly(String clazz, IAnnotationDataHandler annotationData) {
        if (super.hasWrongSideOnly(clazz, annotationData)) {
            return true;
        }
        Set<IAnnotationData> ad = annotationData.getAnnotationsForClass(clazz).apply(org.objectweb.asm.Type.getType(Environment.class));
        for (var a : ad) {
            if (!a.isClass()) {
                continue;
            }
            IAnnotationData.EnumHolder enumHolder = (IAnnotationData.EnumHolder) a.getAnnotationInfo().get("value");
            if (!FabricLoader.getInstance().getEnvironmentType().toString().equals(enumHolder.value())) {
                return true;
            }
        }
        return false;
    }

    static final Map<ModLoadingStage, ConcurrentLinkedDeque<Map.Entry<IModContainer, Runnable>>> DEFERRED_WORK_QUEUE = new HashMap<>();

    @Override
    public void enqueueDeferredWork(ModLoadingStage stage, IModContainer modContainer, Runnable runnable) {
        if (stage == ModLoadingStage.PRE_CONSTRUCT) {
            throw new UnsupportedOperationException();
        }
        if (!DEFERRED_WORK_QUEUE.containsKey(stage)) {
            throw new IllegalArgumentException("Invalid stage: " + stage.getName());
        }
        DEFERRED_WORK_QUEUE.get(stage).add(Map.entry(modContainer, runnable));
    }

    static {
        Arrays.stream(ModLoadingStage.values()).forEach(stage -> {
            if (stage == ModLoadingStage.PRE_CONSTRUCT) {
                return;
            }
            DEFERRED_WORK_QUEUE.put(stage, new ConcurrentLinkedDeque<>());
        });
    }

}
