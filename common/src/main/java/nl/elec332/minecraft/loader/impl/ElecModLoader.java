package nl.elec332.minecraft.loader.impl;

import nl.elec332.minecraft.loader.api.discovery.IAnnotationData;
import nl.elec332.minecraft.loader.api.distmarker.Dist;
import nl.elec332.minecraft.loader.api.modloader.IModContainer;
import nl.elec332.minecraft.loader.api.modloader.IModLoader;
import nl.elec332.minecraft.loader.api.modloader.IModMetaData;
import nl.elec332.minecraft.loader.api.modloader.ModLoadingStage;
import nl.elec332.minecraft.loader.mod.IModLoaderEventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

import java.lang.annotation.ElementType;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Elec332 on 13-02-2024
 */
public final class ElecModLoader {

    @NotNull
    public static ElecModLoader waitForModLoader() {
        return LoaderInitializer.INSTANCE.waitForLoader();
    }

    @NotNull
    public static ElecModLoader getModLoader() {
        return LoaderInitializer.INSTANCE.getModLoader();
    }

    public static void verifyModContainer(IModContainer modContainer, Set<String> packages) {
        if (LoaderInitializer.INSTANCE.completedSetup() || !getModLoader().discoveredAllMods()) {
            throw new IllegalStateException();
        }
        if (!(modContainer instanceof ElecModContainer)) {
            return;
        }
        if (modContainer != getModLoader().getModContainer(modContainer.getModId())) {
            throw new RuntimeException();
        }
        if (((ElecModContainer) modContainer).ownedPackages != null) {
            throw new IllegalStateException();
        }
        ((ElecModContainer) modContainer).ownedPackages = Collections.unmodifiableSet(packages);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    ElecModLoader(Function<Type, Set<IAnnotationData>> dataHandler) {
        this.discoveredMods = dataHandler.apply(LoaderConstants.MODANNOTATION).stream()
                .filter(ad -> ad.getTargetType() == ElementType.TYPE)
                .filter(ad -> {
                    Object dist = ad.getAnnotationInfo().get("dist");
                    if (dist == null) {
                        return true;
                    }
                    return ((List<?>) dist).stream()
                            .map(eh -> Dist.valueOf(((IAnnotationData.EnumHolder) eh).value()))
                            .anyMatch(d -> d == IModLoader.INSTANCE.getDist());
                })
                .collect(Collectors.toMap(ad -> (String) Objects.requireNonNull(ad.getAnnotationInfo().get("value")), ad -> {
                    Set<IAnnotationData> ret = new HashSet<>();
                    ret.add(ad);
                    return ret;
                }, (o1, o2) -> {
                    o1.addAll(o2);
                    return o1;
                })).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, s -> {
                    Set<Type> ret = new LinkedHashSet<>();
                    s.getValue().stream()
                            //Make the "most common" (supporting most distributions) entrypoint go first for Forge instance matching
                            .sorted((o1, o2) -> {
                                List<?> t1 = (List<?>) o1.getAnnotationInfo().get("dist");
                                List<?> t2 = (List<?>) o2.getAnnotationInfo().get("dist");
                                if (Objects.equals(t1, t2)) {
                                    return 0;
                                }
                                if (t1 == null && t2.size() != Dist.values().length) {
                                    return -1;
                                }
                                if (t2 == null && t1.size() != Dist.values().length) {
                                    return 1;
                                }
                                if (t1 != null && t2 != null) {
                                    return t2.size() - t1.size();
                                }
                                return 0;
                            })
                            .forEach(ad -> ret.add(ad.getClassType()));
                    return ret;
                }));
        this.containers = new HashMap<>();
        this.modIds = Collections.unmodifiableSet(this.discoveredMods.keySet());
        this.logger = LogManager.getLogger("ElecModLoader");
        IModLoaderEventHandler.INSTANCE.getClass(); //Force-load service
        ElecModLoaderEventHandler.INSTANCE.containers = () -> { //TODO remove?
            if (!ElecModLoader.getModLoader().discoveredAllMods()) {
                throw new IllegalStateException();
            }
            return containers.values().stream();
        };

        this.logger.debug("Discovered mods: {}", this.modIds);
    }

    private final Set<String> modIds;
    private Map<String, Set<Type>> discoveredMods;
    private final Map<String, ElecModContainer> containers;
    private final Logger logger;

    void announcePreLaunch() {
        this.logger.info("Preloader hooks initialized");
    }

    void checkEnvironment() {
        if (!SidedTest.testSide(IModLoader.INSTANCE.getDist())) {
            throw new RuntimeException("SideCleaner isn't active!");
        }
        this.logger.debug("Environment check succeeded");
    }

    void finalizeLoading() {
        if (this.containers.isEmpty() || !discoveredAllMods()) {
            throw new IllegalStateException("ModLoader finalization is being called too early!");
        }
        this.containers.forEach((name, container) -> {
            IModContainer mc = IModLoader.INSTANCE.getModContainer(name);
            if (mc == null) {
                throw new RuntimeException("Failed to load linked mod " + name);
            }
            if (mc != container) {
                throw new IllegalStateException("Container mismatch for " + name);
            }
        });
        Set<String> missing = new HashSet<>();
        for (String s : getDiscoveredMods()) {
            IModContainer mc = IModLoader.INSTANCE.getModContainer(s);
            if (!(mc instanceof ElecModContainer)) {
                missing.add(s);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Failed to load mods: " + missing);
        }
        AnnotationDataHandler.INSTANCE.attribute(IModLoader.INSTANCE.getMods());
        int s = this.containers.size();
        this.logger.info("Finished modlist, found {} mod{}", s, s == 1 ? "" : "s");
    }

    private boolean discoveredAllMods() {
        return discoveredMods == null;
    }

    //TODO: Evaluate existence
    public Collection<String> getDiscoveredMods() {
        return this.modIds;
    }

    public void useDiscoveredMods(BiFunction<IModMetaData, Set<Type>, ElecModContainer> factory) {
        synchronized (LoaderConstants.MODANNOTATION) {
            if (discoveredAllMods()) {
                throw new IllegalStateException();
            }
            discoveredMods.forEach((name, type) -> {
                IModMetaData meta = IModLoader.INSTANCE.getModMetaData(name);
                if (meta == null || !name.equals(meta.getModId())) {
                    throw new IllegalStateException();
                }
                buildMod(factory.apply(meta, type));
            });
            discoveredMods.forEach((name, type) -> {
                if (containers.get(name) == null) {
                    throw new RuntimeException("Mod " + name + " wasn't loaded!");
                }
            });
            discoveredMods = null;
        }
    }

    public ElecModContainer useDiscoveredMod(String name, BiFunction<IModMetaData, Set<Type>, ElecModContainer> factory) {
        synchronized (LoaderConstants.MODANNOTATION) {
            if (discoveredAllMods()) {
                throw new IllegalStateException();
            }
            ElecModContainer ret;
            Set<Type> type = discoveredMods.get(name);
            if (type == null) {
                throw new IllegalArgumentException();
            }
            IModMetaData meta = IModLoader.INSTANCE.getModMetaData(name);
            if (meta == null || !name.equals(meta.getModId())) {
                throw new IllegalStateException();
            }
            ret = factory.apply(meta, type);
            buildMod(ret);
            if (containers.get(name) == null) {
                throw new RuntimeException("Mod " + name + " wasn't loaded!");
            }
            discoveredMods.remove(name);
            if (discoveredMods.isEmpty()) {
                discoveredMods = null;
            }
            return ret;
        }
    }

    private void buildMod(ElecModContainer modContainer) {
        Objects.requireNonNull(modContainer);
        String modId = modContainer.getModId();
        if (!discoveredMods.containsKey(modId)) {
            throw new UnsupportedOperationException("Can't build undiscovered mod " + modId);
        }
        if (containers.containsKey(modId)) {
            throw new IllegalStateException("ModContainer for " + modId + " has already been built!");
        }
        containers.put(modId, modContainer);
    }

    //TODO: Evaluate existence
    public Stream<? extends IModContainer> getMods() {
        return containers.values().stream();
    }

    public IModContainer getModContainer(String modId) {
        if (!discoveredAllMods()) {
            throw new IllegalStateException();
        }
        return containers.get(modId);
    }

    public void processAnnotations(ModLoadingStage stage) {
        if (!discoveredAllMods()) {
            throw new IllegalStateException();
        }
        AnnotationDataHandler.INSTANCE.process(stage);
    }

}
