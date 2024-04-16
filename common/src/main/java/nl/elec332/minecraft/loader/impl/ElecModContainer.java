package nl.elec332.minecraft.loader.impl;

import nl.elec332.minecraft.loader.api.distmarker.Dist;
import nl.elec332.minecraft.loader.api.modloader.IModContainer;
import nl.elec332.minecraft.loader.api.modloader.IModLoader;
import nl.elec332.minecraft.loader.api.modloader.IModMetaData;
import nl.elec332.minecraft.loader.mod.event.ConstructModEvent;
import nl.elec332.minecraft.repackaged.net.neoforged.bus.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 05-02-2024
 */
public class ElecModContainer implements IModContainer {

    public ElecModContainer(IModMetaData modMetaData, String className, ClassSupplier classSupplier, BiFunction<Map.Entry<ErrorType, Throwable>, Class<?>, RuntimeException> errorProducer) {
        this.modMetaData = modMetaData;
        this.eventBus = BusBuilder.builder()
                .setExceptionHandler(this::onEventFailed)
                .allowPerPhasePost()
                .build();
        eventBus.addListener(EventPriority.HIGHEST, ConstructModEvent.class, e -> constructMod());

        try {
            modClass = classSupplier.loadModClass(className);
            LOGGER.trace(LOADING,"Loaded modclass {} with {}", modClass.getName(), modClass.getClassLoader());
        } catch (Throwable e) {
            LOGGER.error(LOADING, "Failed to load class {}", className, e);
            throw errorProducer.apply(Map.entry(ErrorType.CONSTRUCT, e), null);
        }
        this.errorProducer = errorProducer;
    }

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker LOADING = MarkerManager.getMarker("LOADING");

    private final IModMetaData modMetaData;
    Set<String> ownedPackages;
    private final IEventBus eventBus;
    private Object modInstance;
    private final Class<?> modClass;
    private final BiFunction<Map.Entry<ErrorType, Throwable>, Class<?>, RuntimeException> errorProducer;

    @Override
    public final IModMetaData getModMetadata() {
        return this.modMetaData;
    }

    @Override
    public final Set<String> getOwnedPackages() {
        return this.ownedPackages;
    }

    private void onEventFailed(IEventBus iEventBus, Event event, EventListener[] iEventListeners, int i, Throwable throwable) {
        throw new RuntimeException("Failed to fire event " + event.getClass() + " to mod \"" + this.getModId() + "\"", throwable);
    }

    private void constructMod() {
        if (!LoaderInitializer.INSTANCE.completedModList()) {
            throw new IllegalStateException("ModList hasn't been finalized before instantiation!");
        }
        try {
            LOGGER.trace(LOADING, "Loading mod instance {} of type {}", getModId(), modClass.getName());

            var constructors = modClass.getConstructors();
            if (constructors.length != 1) {
                throw new RuntimeException("Mod class must have exactly 1 public constructor, found " + constructors.length);
            }
            var constructor = constructors[0];

            // Allowed arguments for injection via constructor
            Map<Class<?>, Object> allowedConstructorArgs = Map.of(
                    IEventBus.class, eventBus,
                    IModContainer.class, this,
                    Dist.class, IModLoader.INSTANCE.getDist());

            var parameterTypes = constructor.getParameterTypes();
            Object[] constructorArgs = new Object[parameterTypes.length];
            Set<Class<?>> foundArgs = new HashSet<>();

            for (int i = 0; i < parameterTypes.length; i++) {
                Object argInstance = allowedConstructorArgs.get(parameterTypes[i]);
                if (argInstance == null) {
                    throw new RuntimeException("Mod constructor has unsupported argument " + parameterTypes[i] + ". Allowed optional argument classes: " + allowedConstructorArgs.keySet().stream().map(Class::getSimpleName).collect(Collectors.joining(", ")));
                }

                if (foundArgs.contains(parameterTypes[i])) {
                    throw new RuntimeException("Duplicate mod constructor argument type: " + parameterTypes[i]);
                }

                foundArgs.add(parameterTypes[i]);
                constructorArgs[i] = argInstance;
            }

            // All arguments are found
            this.modInstance = constructor.newInstance(constructorArgs);

            LOGGER.trace(LOADING, "Loaded mod instance {} of type {}", getModId(), modClass.getName());
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = e.getCause(); // exceptions thrown when a reflected method call throws are wrapped in an InvocationTargetException. However, this isn't useful for the end user who has to dig through the logs to find the actual cause.
            }
            LOGGER.error(LOADING,"Failed to create mod instance. ModID: {}, class {}", getModId(), modClass.getName(), e);
            throw errorProducer.apply(Map.entry(ErrorType.CONSTRUCT, e), modClass);
        }
    }

    public final boolean matches(Object mod) {
        return mod == modInstance;
    }

    public final Object getMod() {
        return modInstance;
    }

    public final IEventBus getEventBus() {
        return this.eventBus;
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
