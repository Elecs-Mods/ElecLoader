package nl.elec332.minecraft.loader.impl.fabriclike.mixin.plugin;

import nl.elec332.minecraft.loader.api.distmarker.OnlyIn;
import nl.elec332.minecraft.loader.impl.ElecModLoader;
import nl.elec332.minecraft.loader.impl.SideCleaner;
import nl.elec332.minecraft.loader.util.AbstractDynamicMixinPlugin;
import nl.elec332.minecraft.loader.util.DynamicURLLoader;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.function.Consumer;

/**
 * Created by Elec332 on 01-02-2024
 */
public final class FabricLikeMixinPlugin extends AbstractDynamicMixinPlugin {

    public FabricLikeMixinPlugin() {
        this.sideCleaner = new SideCleaner(LogManager.getLogger("ElecLoader SideCleaner"), ElecModLoader.getDist().name());
    }

    private final SideCleaner sideCleaner;

    @Override
    protected void defineClass(String name, byte[] data) {
        findURLAdder().accept(DynamicURLLoader.create(name, data));
    }

    @Override
    protected void collectMixinClasses(Consumer<String> classConsumer) {
        ElecModLoader.initSideCleaner(dataHandler -> {
            dataHandler.apply(Type.getType(OnlyIn.class)).forEach(ad -> classConsumer.accept(ad.getClassName().replace('.', '/')));
        });
    }

    @Override
    protected RuntimeException onLoadFailed(Exception source) {
        ElecModLoader.mixinFailed();
        return super.onLoadFailed(source);
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        sideCleaner.processClass(targetClass);
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
            if (!addUrlMethod.trySetAccessible()) {
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
