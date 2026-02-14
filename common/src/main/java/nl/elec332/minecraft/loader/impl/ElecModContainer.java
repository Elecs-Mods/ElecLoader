package nl.elec332.minecraft.loader.impl;

import nl.elec332.minecraft.loader.api.distmarker.Dist;
import nl.elec332.minecraft.loader.api.modloader.IModContainer;
import nl.elec332.minecraft.loader.api.modloader.IModLoader;
import nl.elec332.minecraft.loader.api.modloader.IModMetaData;
import nl.elec332.minecraft.loader.api.modloader.ModLoadingStage;
import nl.elec332.minecraft.loader.util.DynamicClassInstantiator;
import nl.elec332.minecraft.repackaged.net.neoforged.bus.api.BusBuilder;
import nl.elec332.minecraft.repackaged.net.neoforged.bus.api.Event;
import nl.elec332.minecraft.repackaged.net.neoforged.bus.api.EventListener;
import nl.elec332.minecraft.repackaged.net.neoforged.bus.api.IEventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.objectweb.asm.Type;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Created by Elec332 on 05-02-2024
 */
public class ElecModContainer implements IModContainer {

    public ElecModContainer(IModMetaData modMetaData, Set<Type> entryPoints, ClassSupplier classSupplier, BiFunction<ErrorType, Throwable, RuntimeException> errorProducer, Supplier<BiConsumer<ModLoadingStage, Runnable>> deferredWorkRegistry) {
        this.modMetaData = modMetaData;
        this.eventBus = BusBuilder.builder()
                .setExceptionHandler(this::onEventFailed)
                .allowPerPhasePost()
                .build();
        Set<Class<?>> eps = new HashSet<>();
        if (Objects.requireNonNull(entryPoints).isEmpty()) {
            throw new IllegalArgumentException("entryPoints is empty");
        }
        for (Type type : entryPoints) {
            try {
                Class<?> modClass = classSupplier.loadModClass(type.getClassName());
                LOGGER.trace(LOADING,"Loaded modclass {} with {}", modClass.getName(), modClass.getClassLoader());
                eps.add(modClass);
            } catch (Throwable e) {
                LOGGER.error(LOADING, "Failed to load class {}", type.getClassName(), e);
                throw errorProducer.apply(ErrorType.CONSTRUCT, e);
            }
        }
        this.modClasses = Collections.unmodifiableSet(eps);


        this.errorProducer = errorProducer;
        this.deferredWorkRegistry = deferredWorkRegistry;
    }

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker LOADING = MarkerManager.getMarker("LOADING");

    private final IModMetaData modMetaData;
    Set<String> ownedPackages;
    private final IEventBus eventBus;
    private Object modInstance;
    private final Collection<Class<?>> modClasses;
    private final BiFunction<ErrorType, Throwable, RuntimeException> errorProducer;
    private final Supplier<BiConsumer<ModLoadingStage, Runnable>> deferredWorkRegistry;

    @Override
    public final IModMetaData getModMetadata() {
        return this.modMetaData;
    }

    @Override
    public final Set<String> getOwnedPackages() {
        return this.ownedPackages;
    }

    public Object getFirstModInstance() {
        return modInstance;
    }

    private void onEventFailed(IEventBus iEventBus, Event event, EventListener[] iEventListeners, int i, Throwable throwable) {
        throw new RuntimeException("Failed to fire event " + event.getClass() + " to mod \"" + this.getModId() + "\"", throwable);
    }

    public final void constructMod() {
        if (this.modInstance != null) {
            throw new IllegalStateException("Mod already constructed");
        }
        synchronized (ElecModContainer.class) {
            if (!LoaderInitializer.INSTANCE.completedModList()) {
                LoaderInitializer.INSTANCE.finalizeLoading();
            }
        }

        // Allowed arguments for injection via constructor
        Map<Class<?>, Object> allowedConstructorArgs = Map.of(
                IEventBus.class, eventBus,
                IModContainer.class, this,
                Dist.class, IModLoader.INSTANCE.getDist());

        for (Class<?> modClass : this.modClasses) {
            try {
                LOGGER.trace(LOADING, "Loading mod instance {} of type {}", getModId(), modClass.getName());

                Object instance = DynamicClassInstantiator.newInstance(modClass, allowedConstructorArgs);
                if (this.modInstance == null) {
                    this.modInstance = instance;
                }

                LOGGER.trace(LOADING, "Loaded mod instance {} of type {}", getModId(), modClass.getName());
            } catch (Throwable e) {
                if (e instanceof InvocationTargetException) {
                    e = e.getCause(); // exceptions thrown when a reflected method call throws are wrapped in an InvocationTargetException. However, this isn't useful for the end user who has to dig through the logs to find the actual cause.
                }
                LOGGER.error(LOADING,"Failed to create mod instance. ModID: {}, class {}", getModId(), modClass.getName(), e);
                throw errorProducer.apply(ErrorType.CONSTRUCT, e);
            }
        }
    }

    public final IEventBus getEventBus() {
        return this.eventBus;
    }

    /**
     * Enqueues work to be run after the provided {@link ModLoadingStage} has completed.
     * Useful as mod may be loaded async or in an unpredictable order.
     *
     * @param stage The stage the provided worker should run aftermc
     * @param runnable The worker to be run after the specified {@link ModLoadingStage}
     */
    public final void enqueueDeferredWork(ModLoadingStage stage, Runnable runnable) {
        if (stage == ModLoadingStage.PRE_CONSTRUCT) {
            throw new UnsupportedOperationException();
        }
        this.deferredWorkRegistry.get().accept(stage, runnable);
    }

    public interface ClassSupplier {

        Class<?> loadModClass(String className) throws Exception;

    }

    @Override
    public String toString() {
        return toInfoString();
    }

    public enum ErrorType {
        CLASSLOAD, CONSTRUCT
    }

}
