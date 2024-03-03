package nl.elec332.minecraft.loader.api.service;

import nl.elec332.minecraft.loader.api.modloader.IModLoader;

import java.util.*;

/**
 * Created by Elec332 on 28-02-2024
 */
public final class ModServiceLoader {

    public static <T> T loadSingleModService(Class<T> type) {
        Set<T> srv = loadModService(type);
        if (srv.size() != 1) {
            throw new RuntimeException();
        }
        return srv.iterator().next();
    }

    public static <T> Set<T> loadModService(Class<T> type) {
        Set<T> ret = new HashSet<>();
        ServiceLoader.load(type, IModLoader.INSTANCE.getModClassLoader()).stream().forEach(p -> {
            //ServiceLoader already loads classes before you get access to the data, so there's no point in doing this with fancy ASM annotation data stuff
            ModService d = p.type().getAnnotation(ModService.class);
            if (d == null || !List.of(d.value()).contains(IModLoader.INSTANCE.getModLoaderType())) {
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
