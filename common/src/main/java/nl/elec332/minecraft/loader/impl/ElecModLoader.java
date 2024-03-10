package nl.elec332.minecraft.loader.impl;

import nl.elec332.minecraft.loader.api.discovery.IAnnotationData;
import nl.elec332.minecraft.loader.api.discovery.IAnnotationDataHandler;
import nl.elec332.minecraft.loader.api.distmarker.Dist;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 13-02-2024
 */
public final class ElecModLoader {

    static IAnnotationDataHandler dataHandler;
    private static ElecModLoader modLoader;
    private static boolean ranInit = false;
    private static boolean checked = false;
    private static boolean crashed = false;

    public static void initSideCleaner(Consumer<Function<Type, Set<IAnnotationData>>> initializer) {
        if (ranInit || dataHandler != null) {
            throw new IllegalStateException();
        }
        ranInit = true;
        try {
            dataHandler = AnnotationDataHandler.INSTANCE.identify(DeferredModLoader.INSTANCE.getModFiles(), DeferredModLoader.INSTANCE::hasWrongSideOnly);
            modLoader = new ElecModLoader(dataHandler::getAnnotationList);
            initializer.accept(dataHandler::getAnnotationList);
        } catch (Exception e) {
            dataHandler = null;
            throw new RuntimeException(e);
        }
    }

    public static void processAnnotations(ModLoadingStage stage) {
        if (!ranInit || dataHandler != null || !modLoader.hasLoaded()) {
            throw new IllegalStateException();
        }
        AnnotationDataHandler.INSTANCE.process(stage);
    }

    public static void verifyModContainer(IModContainer modContainer, Set<String> packages) {
        if (DeferredModLoader.INSTANCE.hasInitialized()) {
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

    public static Dist getDist() {
        return DeferredModLoader.INSTANCE.getDist();
    }

    public static void mixinFailed() {
        dataHandler = null;
        modLoader = null;
        crashed = true;
    }

    @NotNull
    public static ElecModLoader getModLoader() {
        if (modLoader == null) {
            if (!ranInit) {
                throw new UnsupportedOperationException();
            }
            checkEnvironment();
            throw new RuntimeException("wut?");
        }
        return modLoader;
    }

    public static void checkEnvironment() {
        if (checked) {
            return;
        }
        if (crashed || !ranInit || modLoader == null || (dataHandler == null && !modLoader.hasLoaded())) {
            throw new IllegalStateException("Mixin setup failed!");
        }
        if (!SidedTest.testSide(DeferredModLoader.INSTANCE.getDist())) {
            throw new RuntimeException("SideCleaner isn't active!");
        }
        checked = true;

        modLoader.logger.debug("Environment check succeeded");
    }

    public static void checkModLoader() {
        DeferredModLoader.INSTANCE.postInit(true);
    }

    private ElecModLoader(Function<Type, Set<IAnnotationData>> dataHandler) {
        this.discoveredMods = dataHandler.apply(LoaderConstants.MODANNOTATION).stream()
                .collect(Collectors.toMap(ad -> (String) ad.getAnnotationInfo().get("value"), IAnnotationData::getClassType, (t1, t2) -> null));
        this.containers = new HashMap<>();
        this.modIds = Collections.unmodifiableSet(this.discoveredMods.keySet());
        this.logger = LogManager.getLogger("ElecModLoader");

        this.logger.debug("Discovered mods: " + this.modIds);
    }

    private final Set<String> modIds;
    private Map<String, Type> discoveredMods;
    final Map<String, ElecModContainer> containers;
    private final Logger logger;

    private void checkMods() {
        Set<String> missing = new HashSet<>();
        for (String s : modIds) {
            IModContainer mc = DeferredModLoader.INSTANCE.getModContainer(s);
            if (!(mc instanceof ElecModContainer)) {
                missing.add(s);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Failed to load mods: " + missing);
        }
        AnnotationDataHandler.INSTANCE.attribute(DeferredModLoader.INSTANCE.getMods());
    }

    public void useDiscoveredMods(BiFunction<IModMetaData, Type, ElecModContainer> factory) {
        if (hasLoaded()) {
            throw new IllegalStateException();
        }
        synchronized (LoaderConstants.MODANNOTATION) {
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
            dataHandler = null;
            discoveredMods = null;
        }
    }

    public ElecModContainer useDiscoveredMod(String name, BiFunction<IModMetaData, Type, ElecModContainer> factory) {
        if (hasLoaded()) {
            throw new IllegalStateException();
        }
        synchronized (LoaderConstants.MODANNOTATION) {
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
                dataHandler = null;
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

    public void finalizeLoading() {
        checkEnvironment();
        DeferredModLoader.INSTANCE.postInit(false);
        dataHandler = null;
        checkMods();
        AnnotationDataHandler.INSTANCE.preProcess();
        int s = containers.size();
        logger.info("Finished modlist, found " + s + " mod" + (s == 1 ? "" : "s"));
        AnnotationDataHandler.INSTANCE.process(ModLoadingStage.PRE_CONSTRUCT);
    }

    public boolean hasLoaded() {
        return discoveredMods == null;
    }

    public IModContainer getModContainer(String modId) {
        if (!hasLoaded()) {
            throw new IllegalStateException();
        }
        return containers.get(modId);
    }

    public <T extends Event> T postEvent(T event) {
        if (!hasLoaded()) {
            throw new IllegalStateException();
        }
        containers.values().forEach(mc -> mc.getEventBus().post(event));
        return event;
    }

    public <T extends Event> void postModEvent(Function<IModContainer, T> event) {
        if (!hasLoaded()) {
            throw new IllegalStateException();
        }
        containers.values().forEach(mc -> mc.getEventBus().post(event.apply(mc)));
    }

    public void enqueueDeferredWork(ModLoadingStage stage, IModContainer modContainer, Runnable work) {
        DeferredModLoader.INSTANCE.enqueueDeferredWork(stage, modContainer, work);
    }

}
