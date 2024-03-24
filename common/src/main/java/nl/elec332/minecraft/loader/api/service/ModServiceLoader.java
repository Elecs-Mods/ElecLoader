package nl.elec332.minecraft.loader.api.service;

import nl.elec332.minecraft.loader.api.modloader.IModLoader;

import java.util.*;

/**
 * Created by Elec332 on 28-02-2024
 * <p>
 * Small helper class for loading mod services
 */
public final class ModServiceLoader {

    /**
     * Uses the service loader to load a single service instance.
     * Will throw an exception if multiple implementations were provided or none were found.
     * This special method will use the classloader used to load the class for classloading, as there are instances where this may be called before the mod classloader is properly setup
     *
     * @see ModServiceLoader#loadModService(ServiceLoader) for more details.
     *
     * @param type The service type to be loaded
     * @return The loaded service instance
     */
    public static <T> T loadAPIService(Class<T> type) {
        try {
            return loadSingleModService(ServiceLoader.load(type, type.getClassLoader()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + type.getSimpleName() + " implementation!", e);
        }
    }

    /**
     * Uses the service loader to load a single service instance.
     * Will throw an exception if multiple implementations were provided or none were found.
     *
     * @see ModServiceLoader#loadModService(ServiceLoader) for more details.
     *
     * @param type The service type to be loaded
     * @return The loaded service instance
     */
    public static <T> T loadSingleModService(Class<T> type) {
        return loadSingleModService(ServiceLoader.load(type, IModLoader.INSTANCE.getModClassLoader()));
    }

    /**
     * Uses the service loader to load a single service instance.
     * Will throw an exception if multiple implementations were provided or none were found.
     *
     * @see ModServiceLoader#loadModService(ServiceLoader) for more details.
     *
     * @param loader The service loader to be used for loading services
     * @return The loaded service instance
     */
    public static <T> T loadSingleModService(ServiceLoader<T> loader) {
        Set<T> srv = loadModService(loader);
        if (srv.size() != 1) {
            throw new RuntimeException("Expected 1 service when loading " + loader + ", got " + srv.size());
        }
        return srv.iterator().next();
    }

    /**
     * Uses the service loader to load service instances using the mod classloader.
     *
     * @see ModServiceLoader#loadModService(ServiceLoader) for more details.
     *
     * @param type The service type to be loaded
     * @return The loaded service instance(s)
     */
    public static <T> Set<T> loadModService(Class<T> type) {
        return loadModService(ServiceLoader.load(type, IModLoader.INSTANCE.getModClassLoader()));
    }

    /**
     * Uses the provided service loader to load service instances.
     * If the implementation is annotated with {@link ModService} it will check if the implementation is in a valid environment, otherwise it will be skipped.
     * If the implementation implements {@link IModService} it will check if the implementation is in a valid environment, otherwise it will be skipped.
     *
     * @param loader The service loader to be used for loading services
     * @return The loaded service instance(s)
     */
    public static <T> Set<T> loadModService(ServiceLoader<T> loader) {
        Set<T> ret = new HashSet<>();
        loader.stream().forEach(p -> {
            //ServiceLoader already loads classes before you get access to the data, so there's no point in doing this with fancy ASM annotation data stuff
            ModService d = p.type().getAnnotation(ModService.class);
            if (d != null && !List.of(d.value()).contains(IModLoader.INSTANCE.getModLoaderType())) {
                return;
            }
            T value = p.get();
            if (value instanceof IModService && !((IModService) value).isValidLoader(IModLoader.INSTANCE.getModLoaderType())) {
                return;
            }
            ret.add(value);
        });
        return Collections.unmodifiableSet(ret);
    }

}
