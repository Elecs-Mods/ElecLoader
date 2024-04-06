package nl.elec332.minecraft.loader.impl.fmod;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import nl.elec332.minecraft.loader.impl.LoaderInitializer;
import nl.elec332.minecraft.loader.impl.SideCleaner;
import nl.elec332.minecraft.loader.util.IClassTransformer;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by Elec332 on 11-02-2024
 */
public class ForgeLikeMixinConnector implements IMixinConnector {

    private void addTransformers(Consumer<IClassTransformer> registry) {
        LoaderInitializer.INSTANCE.startLoader(dataHandler -> {
            SideCleaner.register(registry, dataHandler);
        });
    }

    @Override
    public void connect() {
        try {
            var plugins = getPlugins();
            Map<String, ILaunchPluginService> extraPlugins = new HashMap<>();
            addTransformers(c -> extraPlugins.put(c.getName(), new ILaunchPluginService() {

                @Override
                public String name() {
                    return c.getName();
                }

                @Override
                public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
                    if (c.getTargetClasses().contains(classType.getInternalName())) {
                        return EnumSet.of(Phase.BEFORE);
                    }
                    return EnumSet.noneOf(Phase.class);
                }

                @Override
                public boolean processClass(Phase phase, ClassNode classNode, Type classType) {
                    return c.processClass(classNode);
                }

            }));
            plugins.putAll(extraPlugins);
        } catch (Exception e) {
            LoaderInitializer.INSTANCE.mixinFailed(e);
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, ILaunchPluginService> getPlugins() throws Exception {
        Field f = Launcher.class.getDeclaredField("launchPlugins");
        f.setAccessible(true);
        Object o = f.get(Launcher.INSTANCE);
        f = o.getClass().getDeclaredField("plugins");
        f.setAccessible(true);
        Map<String, ILaunchPluginService> ret = new HashMap<>();
        ret.putAll(((Map<String, ILaunchPluginService>) f.get(o)));
        f.set(o, ret);
        return ret;
    }

}
