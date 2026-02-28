package nl.elec332.minecraft.loader.api.service;

import nl.elec332.minecraft.loader.api.modloader.IModLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 28-02-2024
 * <p>
 * Small helper class for loading mod services
 */
public final class ModServiceLoader {

    /**
     * Uses the service loader mechanism to load a single service instance.
     * Will throw an exception if multiple implementations were provided or none were found.
     * This special method will use the classloader used to load the class for classloading, as there are instances where this may be called before the mod classloader is properly setup
     *
     * @see ModServiceLoader#loadSingleService(Class, ClassLoader) for more details.
     *
     * @param type The service type to be loaded
     * @return The loaded service instance
     */
    public static <T> T loadAPIService(Class<T> type) {
        try {
            return loadSingleService(type, type.getClassLoader());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + type.getSimpleName() + " implementation!", e);
        }
    }

    /**
     * Uses the service loader mechanism to load a single service instance.
     * Will throw an exception if multiple implementations were provided or none were found.
     *
     * @see ModServiceLoader#loadSingleService(Class, ClassLoader) for more details.
     *
     * @param type The service type to be loaded
     * @return The loaded service instance
     */
    public static <T> T loadSingleModService(Class<T> type) {
        return loadSingleService(type, IModLoader.INSTANCE.getModClassLoader());
    }

    /**
     * Uses the service loader mechanism to load a single service instance.
     * Will throw an exception if multiple implementations were provided or none were found.
     *
     * @see ModServiceLoader#loadService(Class, ClassLoader)  for more details.
     *
     * @param type The service type to be loaded
     * @param loader The classloader to be used for loading the services
     * @return The loaded service instance
     */
    public static <T> T loadSingleService(Class<T> type, ClassLoader loader) {
        Set<T> srv = loadService(type, loader);
        if (srv.size() != 1) {
            throw new RuntimeException("Expected 1 service when loading " + loader + ", got " + srv.size());
        }
        return srv.iterator().next();
    }

    /**
     * Uses the service loader to load service instances using the mod classloader.
     *
     * @see ModServiceLoader#loadService(Class, ClassLoader)  for more details.
     *
     * @param type The service type to be loaded
     * @return The loaded service instance(s)
     */
    public static <T> Set<T> loadModService(Class<T> type) {
        return loadService(type, IModLoader.INSTANCE.getModClassLoader());
    }

    /**
     * Uses the service loader mechanism to load service instances.
     * If the implementation is annotated with {@link ModService} it will check if the implementation is in a valid environment, otherwise it will be skipped.
     * If the implementation implements {@link IModService} it will check if the implementation is in a valid environment, otherwise it will be skipped.
     * <p>
     * Only supports the java-8 method of registering services!
     *
     * @param type The service type to be loaded
     * @param loader The classloader to be used for loading the services
     * @return The loaded service instance(s)
     */
    public static <T> Set<T> loadService(Class<T> type, ClassLoader loader) {
        Set<T> ret = new HashSet<>();
        try {
            String fullName = "META-INF/services/" + type.getName();
            Enumeration<URL> configs;
            if (loader == null) {
                configs = ClassLoader.getSystemResources(fullName);
            } else {
                configs = loader.getResources(fullName);
            }
            while (configs.hasMoreElements()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(configs.nextElement().openStream(), StandardCharsets.UTF_8));
                for (String service : reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .collect(Collectors.toList())) {
                    Class<?> serviceClass = Class.forName(service, false, loader);
                    if (!type.isAssignableFrom(serviceClass)) {
                        throw new RuntimeException(service + " is not a valid service for " + type.getName());
                    }
                    if (!isValidService(serviceClass)) {
                        continue;
                    }
                    Constructor<?> serviceConstructor = serviceClass.getConstructor();
                    if (!serviceConstructor.isAccessible()) {
                        serviceConstructor.setAccessible(true);
                    }
                    Object serviceInstance = serviceConstructor.newInstance();
                    if (isValidServiceInstance(serviceInstance)) {
                        ret.add(type.cast(serviceInstance));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load service " + type.getName(), e);
        }
        return Collections.unmodifiableSet(ret);
    }

    //ServiceLoader already loads classes before you get access to the data, so there's no point in doing this with fancy ASM annotation data stuff
    private static boolean isValidService(Class<?> clazz) {
        ModService d = clazz.getAnnotation(ModService.class);
        return d == null || Arrays.asList(d.value()).contains(IModLoader.INSTANCE.getModLoaderType());
    }

    private static boolean isValidServiceInstance(Object service) {
        return !(service instanceof IModService) || ((IModService) service).isValidLoader(IModLoader.INSTANCE.getModLoaderType());
    }

}
