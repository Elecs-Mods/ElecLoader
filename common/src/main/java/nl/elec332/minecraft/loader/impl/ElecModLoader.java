package nl.elec332.minecraft.loader.impl;

import nl.elec332.minecraft.loader.api.discovery.IAnnotationData;
import nl.elec332.minecraft.loader.api.modloader.IModContainer;
import nl.elec332.minecraft.loader.api.modloader.IModMetaData;
import nl.elec332.minecraft.loader.api.modloader.ModLoadingStage;
import nl.elec332.minecraft.repackaged.net.neoforged.bus.api.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 13-02-2024
 */
public final class ElecModLoader {

    @NotNull
    public static ElecModLoader getModLoader() {
        return LoaderInitializer.INSTANCE.getModLoader();
    }

    public static void verifyModContainer(IModContainer modContainer, Set<String> packages) {
        if (LoaderInitializer.INSTANCE.completedSetup()) {
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

    ElecModLoader(Function<Type, Set<IAnnotationData>> dataHandler) {
        this.discoveredMods = dataHandler.apply(LoaderConstants.MODANNOTATION).stream()
                .collect(Collectors.toMap(ad -> (String) ad.getAnnotationInfo().get("value"), IAnnotationData::getClassType, (t1, t2) -> null));
        this.containers = new HashMap<>();
        this.modIds = Collections.unmodifiableSet(this.discoveredMods.keySet());
        this.logger = LogManager.getLogger("ElecModLoader");

        this.logger.debug("Discovered mods: " + this.modIds);
    }

    private final Set<String> modIds;
    private Map<String, Type> discoveredMods;
    private final Map<String, ElecModContainer> containers;
    private final Logger logger;

    void checkEnvironment() {
        if (!SidedTest.testSide(DeferredModLoader.INSTANCE.getDist())) {
            throw new RuntimeException("SideCleaner isn't active!");
        }
        this.logger.debug("Environment check succeeded");
    }

    void finalizeLoading() {
        if (getModLoader().containers.isEmpty() || !getModLoader().discoveredAllMods()) {
            throw new IllegalStateException("ModLoader finalization is being called too early!");
        }
        getModLoader().containers.forEach((name, container) -> {
            IModContainer mc = DeferredModLoader.INSTANCE.getModContainer(name);
            if (mc == null) {
                throw new RuntimeException("Failed to load linked mod " + name);
            }
            if (mc != container) {
                throw new IllegalStateException("Container mismatch for " + name);
            }
        });
        Set<String> missing = new HashSet<>();
        for (String s : this.modIds) {
            IModContainer mc = DeferredModLoader.INSTANCE.getModContainer(s);
            if (!(mc instanceof ElecModContainer)) {
                missing.add(s);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Failed to load mods: " + missing);
        }
        AnnotationDataHandler.INSTANCE.attribute(DeferredModLoader.INSTANCE.getMods());
        int s = this.containers.size();
        this.logger.info("Finished modlist, found " + s + " mod" + (s == 1 ? "" : "s"));
    }

    boolean discoveredAllMods() {
        return discoveredMods == null;
    }

    public void useDiscoveredMods(BiFunction<IModMetaData, Type, ElecModContainer> factory) {
        synchronized (LoaderConstants.MODANNOTATION) {
            if (discoveredAllMods()) {
                throw new IllegalStateException();
            }
            discoveredMods.forEach((name, type) -> {
                var meta = DeferredModLoader.INSTANCE.getModMetaData(name);
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

    public ElecModContainer useDiscoveredMod(String name, BiFunction<IModMetaData, Type, ElecModContainer> factory) {
        synchronized (LoaderConstants.MODANNOTATION) {
            if (discoveredAllMods()) {
                throw new IllegalStateException();
            }
            ElecModContainer ret;
            Type type = discoveredMods.get(name);
            if (type == null) {
                throw new IllegalArgumentException();
            }
            var meta = DeferredModLoader.INSTANCE.getModMetaData(name);
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

    public IModContainer getModContainer(String modId) {
        if (!discoveredAllMods()) {
            throw new IllegalStateException();
        }
        return containers.get(modId);
    }

    public <T extends Event> T postEvent(T event) {
        if (!discoveredAllMods()) {
            throw new IllegalStateException();
        }
        containers.values().forEach(mc -> mc.getEventBus().post(event));
        return event;
    }

    public <T extends Event> void postModEvent(Function<IModContainer, T> event) {
        if (!discoveredAllMods()) {
            throw new IllegalStateException();
        }
        containers.values().forEach(mc -> mc.getEventBus().post(event.apply(mc)));
    }

    public void enqueueDeferredWork(ModLoadingStage stage, IModContainer modContainer, Runnable work) {
        DeferredModLoader.INSTANCE.enqueueDeferredWork(stage, modContainer, work);
    }

    public void processAnnotations(ModLoadingStage stage) {
        if (!discoveredAllMods()) {
            throw new IllegalStateException();
        }
        AnnotationDataHandler.INSTANCE.process(stage);
    }

}
