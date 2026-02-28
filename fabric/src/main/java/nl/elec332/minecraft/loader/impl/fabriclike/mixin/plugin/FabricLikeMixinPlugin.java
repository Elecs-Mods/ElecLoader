package nl.elec332.minecraft.loader.impl.fabriclike.mixin.plugin;

import nl.elec332.minecraft.loader.impl.LoaderInitializer;
import nl.elec332.minecraft.loader.util.AbstractDynamicMixinPlugin;
import nl.elec332.minecraft.loader.util.DynamicURLLoader;
import nl.elec332.minecraft.loader.util.IClassTransformer;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.function.Consumer;

/**
 * Created by Elec332 on 01-02-2024
 */
public final class FabricLikeMixinPlugin extends AbstractDynamicMixinPlugin {

    @Override
    protected void defineClass(String name, byte[] data) {
        findURLAdder().accept(DynamicURLLoader.create(name, data));
    }

    @Override
    protected void addTransformers(Consumer<IClassTransformer> registry) {
        LoaderInitializer.INSTANCE.startLoader(registry);
    }

    @Override
    protected RuntimeException onLoadFailed(Exception source) {
        LoaderInitializer.INSTANCE.mixinFailed(source);
        return super.onLoadFailed(source);
    }

    private static Consumer<URL> findURLAdder() {
        ClassLoader root = FabricLikeMixinPlugin.class.getClassLoader();
        ClassLoader loader = root;
        Consumer<URL> ret = findUrlAddMethod(loader, false);
        while (loader != null && ret == null) {
            loader = loader.getParent();
            ret = findUrlAddMethod(loader, false);
        }
        if (ret == null) {
            return findUrlAddMethod(root, true);
        }
        return ret;
    }

    @Nullable
    private static Consumer<URL> findUrlAddMethod(ClassLoader classLoader, boolean force) {
        if (classLoader == null) {
            if (force) {
                throw new IllegalArgumentException("Null Classloader!");
            } else {
                return null;
            }
        }
        Method addUrlMethod = null;
        for (Method method : classLoader.getClass().getDeclaredMethods()) {
            if (method.getReturnType() == Void.TYPE && method.getParameterCount() == 1 && method.getParameterTypes()[0] == URL.class) {
                addUrlMethod = method; //Probably
                break;
            }
        }
        if (addUrlMethod == null) {
            if (force) {
                throw new IllegalStateException("Couldn't find method in " + classLoader);
            }
            return null;
        }
        try {
            try {
                addUrlMethod.setAccessible(true);
            } catch (Throwable e) {
                if (force) {
                    throw new IllegalAccessException("Failed to make URL method public!");
                } else {
                    return null;
                }
            }
            MethodHandle handle = MethodHandles.lookup().unreflect(addUrlMethod);
            return url -> {
                try {
                    handle.invoke(classLoader, url);
                } catch (Throwable t) {
                    throw new RuntimeException("Unexpected error adding URL", t);
                }
            };
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Couldn't get handle for " + addUrlMethod, e);
        }
    }

}
