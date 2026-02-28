package nl.elec332.minecraft.loader.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 02-01-2025.
 */
public class DynamicClassInstantiator {

    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<T> clazz, Map<Class<?>, Object> allowedConstructorArgs) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<?>[] constructors = clazz.getConstructors();
        if (constructors.length != 1) {
            throw new RuntimeException("Class must have exactly 1 public constructor, found " + constructors.length);
        }
        Constructor<?> constructor = constructors[0];

        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] constructorArgs = new Object[parameterTypes.length];
        Set<Class<?>> foundArgs = new HashSet<>();

        for (int i = 0; i < parameterTypes.length; i++) {
            Object argInstance = allowedConstructorArgs.get(parameterTypes[i]);
            if (argInstance == null) {
                throw new RuntimeException("Constructor has unsupported argument " + parameterTypes[i] + ". Allowed optional argument classes: " + allowedConstructorArgs.keySet().stream().map(Class::getSimpleName).collect(Collectors.joining(", ")));
            }

            if (foundArgs.contains(parameterTypes[i])) {
                throw new RuntimeException("Duplicate constructor argument type: " + parameterTypes[i]);
            }

            foundArgs.add(parameterTypes[i]);
            constructorArgs[i] = argInstance;
        }

        return (T) constructor.newInstance(constructorArgs);
    }

}
