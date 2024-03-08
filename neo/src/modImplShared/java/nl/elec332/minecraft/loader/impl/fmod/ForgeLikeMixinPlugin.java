package nl.elec332.minecraft.loader.impl.fmod;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import nl.elec332.minecraft.loader.api.distmarker.OnlyIn;
import nl.elec332.minecraft.loader.impl.ElecModLoader;
import nl.elec332.minecraft.loader.impl.SideCleaner;
import nl.elec332.minecraft.loader.util.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

/**
 * Created by Elec332 on 11-02-2024
 */
public class ForgeLikeMixinPlugin implements IMixinConfigPlugin {

    private void addTransformers(Consumer<IClassTransformer> registry) {
        ElecModLoader.initSideCleaner(dataHandler -> {
            Set<String> sides = new HashSet<>();
            dataHandler.apply(Type.getType(OnlyIn.class)).forEach(ad -> sides.add(ad.getClassType().getInternalName()));
            registry.accept(new SideCleaner(LogManager.getLogger("ElecLoader SideCleaner"), ElecModLoader.getDist().name(), sides));
        });
    }

    @Override
    public void onLoad(String mixinPackage) {
        try {
            var plugins = getPlugins();
            addTransformers(c -> plugins.put(c.getName(), new ILaunchPluginService() {

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
        } catch (Exception e) {
            ElecModLoader.mixinFailed();
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
        return ((Map<String, ILaunchPluginService>) f.get(o));
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return Collections.emptyList();
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

}
